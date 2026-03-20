package de.mgdi.jupiter.swap.config;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Immutable configuration for {@link de.mgdi.jupiter.swap.JupiterSwap}.
 * All fields are passed as optional query parameters to the Jupiter v2 /order endpoint.
 * Null values are omitted from the request — Jupiter uses its own defaults.
 * Use {@link #defaultConfig()} for sensible defaults or the builder for custom values.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@Value
@Builder
public class JupiterSwapConfig {

    /**
     * Slippage tolerance in basis points (1 bps = 0.01%).
     * Example: 50 = max 0.5% slippage. If null, Jupiter determines slippage dynamically.
     */
    Integer slippageBps;

    /**
     * Priority fee in lamports to speed up transaction landing.
     * If null, Jupiter uses its default priority fee.
     */
    Integer priorityFee;

    /**
     * Price per compute unit in micro-lamports.
     * If null, Jupiter determines the compute unit price automatically.
     */
    Integer computeUnitPrice;

    /**
     * If true, only direct token pair routes are used (no multi-hop routing).
     * Reduces route complexity but may result in worse prices.
     */
    @Builder.Default
    boolean onlyDirectRoutes = false;

    /**
     * Maximum number of accounts allowed in the swap route (1–64, default 64).
     * Lower values free up space for custom instructions.
     */
    Integer maxAccounts;

    /**
     * List of DEX names to exclude from routing (e.g. "Raydium", "Orca").
     */
    List<String> excludeDexes;

    /**
     * List of routers to exclude. Possible values: "Metis", "JupiterZ", "Dflow", "OKX".
     * Note: excluding routers may restrict liquidity and result in worse prices.
     */
    List<String> excludeRouters;

    public static JupiterSwapConfig defaultConfig() {
        return JupiterSwapConfig.builder()
                .slippageBps(50)
                .build();
    }
}
