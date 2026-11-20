/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket;

import io.rsocket.core.RSocketConnector;

/**
 * Strategy to apply configuration to an {@code RSocketConnector}. For use with
 * {@link RSocketRequester.Builder#rsocketConnector RSocketRequester.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2.6
 */
@FunctionalInterface
public interface RSocketConnectorConfigurer {

	/**
	 * Apply configuration to the given {@code RSocketConnector}.
	 */
	void configure(RSocketConnector connector);

}
