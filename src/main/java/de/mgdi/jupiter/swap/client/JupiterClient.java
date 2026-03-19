package de.mgdi.jupiter.swap.client;

import de.mgdi.jupiter.swap.model.JupiterQuoteRequest;
import de.mgdi.jupiter.swap.model.JupiterQuoteResponse;
import de.mgdi.jupiter.swap.model.JupiterSwapRequest;
import de.mgdi.jupiter.swap.model.JupiterSwapResponse;

/**
 * Abstraction over the Jupiter Aggregator REST API.
 * Provides methods for fetching swap quotes and submitting swap transactions.
 *
 * @author mgdi consulting
 * @since 2026-03-06
 */
public interface JupiterClient {

    JupiterQuoteResponse quote(JupiterQuoteRequest request);

    JupiterSwapResponse swap(JupiterSwapRequest request);
}
