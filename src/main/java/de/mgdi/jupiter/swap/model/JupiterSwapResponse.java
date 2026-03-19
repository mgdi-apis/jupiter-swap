package de.mgdi.jupiter.swap.model;

import lombok.Data;

import java.math.BigInteger;

/**
 * Response model returned by the Jupiter swap API.
 * Contains the base64-encoded versioned transaction ready to be signed and submitted to the RPC node.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Data
public class JupiterSwapResponse {
    private String swapTransaction;
    private BigInteger lastValidBlockHeight;
    private BigInteger prioritizationFeeLamports;
}
