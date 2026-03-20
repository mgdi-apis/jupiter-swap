package de.mgdi.jupiter.swap.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request model for the Jupiter v2 /order endpoint.
 * Required fields: inputMint, outputMint, amount, taker.
 * Optional fields are omitted from the query string when null.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@Data
@Builder
public class JupiterOrderRequest {
    private final String inputMint;
    private final String outputMint;
    private final long amount;
    private final String taker;

    private final Integer slippageBps;
    private final Integer priorityFee;
    private final Integer computeUnitPrice;
    private final Boolean onlyDirectRoutes;
    private final Integer maxAccounts;
    private final List<String> excludeDexes;
    private final List<String> excludeRouters;
}
