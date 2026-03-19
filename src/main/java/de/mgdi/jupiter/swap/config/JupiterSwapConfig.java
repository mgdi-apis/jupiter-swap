package de.mgdi.jupiter.swap.config;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable configuration for {@link de.mgdi.jupiter.swap.JupiterSwap}.
 * Controls retry behaviour, status polling, and swap slippage tolerance.
 * Use {@link #defaultConfig()} for sensible defaults or the builder for custom values.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Value
@Builder
public class JupiterSwapConfig {

    /**
     * Number of attempts to send the transaction via RPC before giving up.
     * Retries can help compensate for transient network issues or RPC node instability.
     */
    int sendRetryCount;

    /**
     * Total time budget in milliseconds across all send attempts.
     * The interval between retries is derived as {@code sendRetryTimeoutMs / sendRetryCount}.
     * For example, 3 retries over 30 000 ms results in a 10-second pause between attempts.
     */
    int sendRetryTimeoutMs;

    /**
     * Maximum number of times to poll the RPC node for transaction confirmation
     * after the transaction has been sent.
     */
    int statusCheckCount;

    /**
     * Total time budget in milliseconds for waiting on transaction confirmation.
     * The polling interval is derived as {@code statusCheckTimeoutMs / statusCheckCount}.
     * For example, 10 checks over 60 000 ms results in a poll every 6 seconds.
     */
    int statusCheckTimeoutMs;

    /**
     * Maximum accepted slippage for the swap in basis points (1 bps = 0.01%).
     * For example, 50 means a maximum slippage of 0.5%.
     * If the price moves beyond this threshold, Jupiter will reject the quote.
     */
    int slippageBps;

    public static JupiterSwapConfig defaultConfig() {
        return JupiterSwapConfig.builder()
                .sendRetryCount(3)
                .sendRetryTimeoutMs(30_000)
                .statusCheckCount(10)
                .statusCheckTimeoutMs(60_000)
                .slippageBps(50)
                .build();
    }
}
