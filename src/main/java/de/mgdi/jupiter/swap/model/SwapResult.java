package de.mgdi.jupiter.swap.model;

import lombok.Value;

/**
 * Result of a swap execution including confirmation status and transaction signature.
 *
 * @author mgdi consulting
 * @since 2026-03-25
 */
@Value
public class SwapResult {
    boolean success;
    String signature;
}
