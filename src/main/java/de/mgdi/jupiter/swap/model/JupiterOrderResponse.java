package de.mgdi.jupiter.swap.model;

import lombok.Data;

/**
 * Response model returned by the Jupiter v2 order API.
 * Contains the base64-encoded unsigned transaction and a requestId required for the execute step.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@Data
public class JupiterOrderResponse {
    private String transaction;
    private String requestId;
    private String outAmount;
    private String router;
    private String mode;
    private Integer feeBps;
    private String feeMint;
}
