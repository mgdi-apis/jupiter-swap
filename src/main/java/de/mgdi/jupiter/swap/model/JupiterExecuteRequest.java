package de.mgdi.jupiter.swap.model;

import lombok.Builder;
import lombok.Data;

/**
 * Request model for the Jupiter v2 execute API endpoint.
 * Wraps the signed transaction and the requestId obtained from the order step.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@Data
@Builder
public class JupiterExecuteRequest {
    private final String signedTransaction;
    private final String requestId;
}
