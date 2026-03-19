package de.mgdi.jupiter.swap;

import com.google.common.io.BaseEncoding;
import de.mgdi.jupiter.swap.client.JupiterHttpClient;
import de.mgdi.jupiter.swap.config.JupiterSwapConfig;
import de.mgdi.jupiter.swap.model.JupiterQuoteRequest;
import de.mgdi.jupiter.swap.model.JupiterQuoteResponse;
import de.mgdi.jupiter.swap.model.JupiterSwapRequest;
import de.mgdi.jupiter.swap.model.JupiterSwapResponse;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.types.ConfirmedTransaction;
import org.p2p.solanaj.rpc.types.config.RpcSendTransactionConfig;
import org.p2p.solanaj.utils.TweetNaclFast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Entry point for executing token swaps via the Jupiter Aggregator on Solana.
 * Handles quote fetching, transaction signing, submission, and optional confirmation polling.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Slf4j
@Builder
public class JupiterSwap {

    private final JupiterHttpClient jupiterClient;
    private final RpcClient rpcClient;
    private final Account account;

    @Builder.Default
    private final JupiterSwapConfig config = JupiterSwapConfig.defaultConfig();

    /**
     * Sends a swap transaction and returns the transaction hash immediately without waiting for confirmation.
     *
     * @param inputMint  mint address of the token to sell
     * @param outputMint mint address of the token to buy
     * @param amount     amount in smallest unit (e.g. lamports for SOL, micro-USDT for USDT)
     * @return transaction hash
     */
    public String swap(final String inputMint, final String outputMint, final long amount) throws Exception {
        final byte[] tx = buildSignedTransaction(inputMint, outputMint, amount);
        return sendTransaction(tx);
    }

    /**
     * Sends a swap transaction and blocks until it is confirmed or the status check limit is reached.
     *
     * @param inputMint  mint address of the token to sell
     * @param outputMint mint address of the token to buy
     * @param amount     amount in smallest unit (e.g. lamports for SOL, micro-USDT for USDT)
     * @return true if transaction confirmed successfully, false if failed or timed out
     */
    public boolean swapAndAwait(final String inputMint, final String outputMint, final long amount) throws Exception {
        final String hash = swap(inputMint, outputMint, amount);
        return awaitTransaction(hash);
    }

    /**
     * Polls the transaction status until it is confirmed, failed, or the check limit is reached.
     *
     * @param hash transaction hash returned by {@link #swap}
     * @return true if transaction confirmed successfully, false if failed or timed out
     */
    public boolean awaitTransaction(final String hash) throws InterruptedException {
        final long statusCheckIntervalMs = config.getStatusCheckTimeoutMs() / config.getStatusCheckCount();
        for (int i = 0; i < config.getStatusCheckCount(); i++) {
            Thread.sleep(statusCheckIntervalMs);
            try {
                final ConfirmedTransaction confirmed = rpcClient.getApi().getTransaction(hash);
                if (confirmed == null) {
                    continue;
                }
                return confirmed.getMeta().getErr() == null;
            } catch (Exception e) {
                log.debug("Transaction {} not yet available on attempt {}/{}: {}", hash, i + 1, config.getStatusCheckCount(), e.getMessage());
            }
        }
        return false;
    }

    private byte[] buildSignedTransaction(final String inputMint, final String outputMint, final long amount) throws Exception {
        final JupiterQuoteResponse quoteResponse = jupiterClient.quote(
                JupiterQuoteRequest.builder()
                        .inputMint(inputMint)
                        .outputMint(outputMint)
                        .amount(amount)
                        .slippageBps(config.getSlippageBps())
                        .build()
        );

        final JupiterSwapResponse swapResponse = jupiterClient.swap(
                JupiterSwapRequest.builder()
                        .quoteResponse(quoteResponse)
                        .userPublicKey(account.getPublicKey().toBase58())
                        .dynamicComputeUnitLimit(true)
                        .prioritizationFeeLamports("auto")
                        .build()
        );

        // Jupiter's blockhash is valid for ~150 slots (~60s) — sign directly without fetching a new one
        final byte[] tx = BaseEncoding.base64().decode(swapResponse.getSwapTransaction());
        final byte[] messageBytes = Arrays.copyOfRange(tx, 65, tx.length);
        final byte[] signature = new TweetNaclFast.Signature(new byte[0], account.getSecretKey()).detached(messageBytes);
        ByteBuffer.wrap(tx).put(1, signature, 0, 64);
        return tx;
    }

    private String sendTransaction(final byte[] tx) throws Exception {
        final List<Object> params = new ArrayList<>();
        params.add(Base64.getEncoder().encodeToString(tx));
        params.add(new RpcSendTransactionConfig(RpcSendTransactionConfig.Encoding.base64, false, 10));

        final long sendRetryIntervalMs = config.getSendRetryTimeoutMs() / config.getSendRetryCount();
        for (int i = 0; i < config.getSendRetryCount(); i++) {
            try {
                return rpcClient.call("sendTransaction", params, String.class);
            } catch (Exception e) {
                log.warn("Send attempt {}/{} failed, retrying in {}ms: {}", i + 1, config.getSendRetryCount(), sendRetryIntervalMs, e.getMessage());
                Thread.sleep(sendRetryIntervalMs);
            }
        }
        throw new RuntimeException("Failed to send transaction after " + config.getSendRetryCount() + " retries");
    }
}
