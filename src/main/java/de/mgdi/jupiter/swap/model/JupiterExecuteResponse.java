package de.mgdi.jupiter.swap.model;

import lombok.Data;

/**
 * Response model returned by the Jupiter v2 execute API.
 * Jupiter handles transaction landing and returns the final status and signature.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
@Data
public class JupiterExecuteResponse {
    private String status;
    private String signature;
    private Integer code;
    private String inputAmountResult;
    private String outputAmountResult;
    private String error;
}
