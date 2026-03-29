package de.mgdi.jupiter.swap;

import com.google.common.io.BaseEncoding;
import de.mgdi.jupiter.swap.client.JupiterHttpClient;
import de.mgdi.jupiter.swap.config.JupiterSwapConfig;
import de.mgdi.jupiter.swap.model.JupiterExecuteRequest;
import de.mgdi.jupiter.swap.model.JupiterExecuteResponse;
import de.mgdi.jupiter.swap.model.JupiterOrderRequest;
import de.mgdi.jupiter.swap.model.JupiterOrderResponse;
import de.mgdi.jupiter.swap.model.SwapResult;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.p2p.solanaj.core.Account;
import org.p2p.solanaj.utils.TweetNaclFast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

/**
 * Entry point for executing token swaps via the Jupiter Aggregator v2 on Solana.
 * Handles order fetching, transaction signing, and submission via Jupiter's execute endpoint.
 * Jupiter handles transaction landing internally and returns the final status directly.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@Slf4j
@Builder
public class JupiterSwap {

    private final JupiterHttpClient jupiterClient;
    private final Account account;

    @Builder.Default
    private final JupiterSwapConfig config = JupiterSwapConfig.defaultConfig();

    /**
     * Executes a swap and returns the transaction signature once Jupiter confirms landing.
     *
     * @param inputMint  mint address of the token to sell
     * @param outputMint mint address of the token to buy
     * @param amount     amount in smallest unit (e.g. lamports for SOL, micro-USDT for USDT)
     * @return transaction signature
     */
    public String swap(final String inputMint, final String outputMint, final long amount) throws Exception {
        return executeSwap(inputMint, outputMint, amount).getSignature();
    }

    /**
     * Executes a swap and returns the result including confirmation status and transaction signature.
     * Jupiter v2 handles transaction landing in the execute call — the result is final.
     *
     * @param inputMint  mint address of the token to sell
     * @param outputMint mint address of the token to buy
     * @param amount     amount in smallest unit (e.g. lamports for SOL, micro-USDT for USDT)
     * @return {@link SwapResult} with success flag and transaction signature
     */
    public SwapResult swapAndAwait(final String inputMint, final String outputMint, final long amount) throws Exception {
        final JupiterExecuteResponse response = executeSwap(inputMint, outputMint, amount);
        return new SwapResult("Success".equals(response.getStatus()), response.getSignature());
    }

    private JupiterExecuteResponse executeSwap(final String inputMint, final String outputMint, final long amount) throws Exception {
        final JupiterOrderResponse orderResponse = jupiterClient.order(
                JupiterOrderRequest.builder()
                        .inputMint(inputMint)
                        .outputMint(outputMint)
                        .amount(amount)
                        .taker(account.getPublicKey().toBase58())
                        .build()
        );

        final byte[] tx = BaseEncoding.base64().decode(orderResponse.getTransaction());
        final int numSignatures = tx[0] & 0xFF;
        final int messageOffset = 1 + numSignatures * 64;
        final byte[] messageBytes = Arrays.copyOfRange(tx, messageOffset, tx.length);
        final byte[] signature = new TweetNaclFast.Signature(new byte[0], account.getSecretKey()).detached(messageBytes);
        ByteBuffer.wrap(tx).put(1, signature, 0, 64);

        final JupiterExecuteResponse executeResponse = jupiterClient.execute(
                JupiterExecuteRequest.builder()
                        .signedTransaction(Base64.getEncoder().encodeToString(tx))
                        .requestId(orderResponse.getRequestId())
                        .build()
        );

        log.info("Swap execute status={} signature={}", executeResponse.getStatus(), executeResponse.getSignature());
        return executeResponse;
    }
}
