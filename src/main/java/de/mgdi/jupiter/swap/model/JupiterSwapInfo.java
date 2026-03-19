package de.mgdi.jupiter.swap.model;

import lombok.Data;

/**
 * Detailed information about a single swap step within a {@link JupiterRoutePlan}.
 * Includes the AMM used, token pair, amounts, and fee details for that step.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Data
public class JupiterSwapInfo {
    private String ammKey;
    private String label;
    private String inputMint;
    private String outputMint;
    private String inAmount;
    private String outAmount;
    private String feeAmount;
    private String feeMint;
}
