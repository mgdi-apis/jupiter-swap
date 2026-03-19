package de.mgdi.jupiter.swap.model;

import lombok.Data;

import java.math.BigInteger;

/**
 * Response model returned by the Jupiter quote API.
 * Contains routing information, expected output amount, price impact, and slippage details.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Data
public class JupiterQuoteResponse {
    private String inputMint;
    private String inAmount;
    private String outputMint;
    private String outAmount;
    private String otherAmountThreshold;
    private String swapMode;
    private int slippageBps;
    private String platformFee;
    private String priceImpactPct;
    private JupiterRoutePlan routePlan[];
    private BigInteger contextSlot;
    private Double timeTaken;
}
