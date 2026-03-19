package de.mgdi.jupiter.swap.model;

import lombok.Builder;
import lombok.Data;

/**
 * Request model for the Jupiter quote API endpoint.
 * Specifies the input/output tokens, trade amount, and maximum accepted slippage.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Data
@Builder
public class JupiterQuoteRequest {
    private final String inputMint;
    private final String outputMint;
    private final long amount;
    private final int slippageBps;
}
