package de.mgdi.jupiter.swap.client;

import de.mgdi.jupiter.swap.model.JupiterExecuteRequest;
import de.mgdi.jupiter.swap.model.JupiterExecuteResponse;
import de.mgdi.jupiter.swap.model.JupiterOrderRequest;
import de.mgdi.jupiter.swap.model.JupiterOrderResponse;

/**
 * Abstraction over the Jupiter Aggregator v2 REST API.
 * Provides methods for ordering a swap transaction and executing it via Jupiter.
 *
 * @author mgdi consulting
 * @since 2026-03-20
 */
public interface JupiterClient {

    JupiterOrderResponse order(JupiterOrderRequest request);

    JupiterExecuteResponse execute(JupiterExecuteRequest request);
}
