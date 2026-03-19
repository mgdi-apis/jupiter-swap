package de.mgdi.jupiter.swap.model;

import lombok.Data;

/**
 * Represents a single leg in a Jupiter swap route.
 * A route may consist of multiple steps across different liquidity pools (AMMs).
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
@Data
public class JupiterRoutePlan {

    private JupiterSwapInfo swapInfo;
    private int percent;
}
