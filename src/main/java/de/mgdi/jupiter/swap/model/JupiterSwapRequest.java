package de.mgdi.jupiter.swap.model;

import lombok.Builder;
import lombok.Data;

/**
 * Request model for the Jupiter swap API endpoint.
 * Wraps a previously obtained {@link JupiterQuoteResponse} together with user preferences
 * such as SOL wrapping, compute unit limits, and fee prioritization.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Data
@Builder
public class JupiterSwapRequest {
    private final JupiterQuoteResponse quoteResponse;
    private final String userPublicKey;

    @Builder.Default
    private final boolean wrapAndUnwrapSol = true;

    @Builder.Default
    private final boolean dynamicComputeUnitLimit = true;

    @Builder.Default
    private final String prioritizationFeeLamports = "auto";
}
