/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.invocation.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Assist with configuration for handler method return value handlers.
 * At present, it supports only providing a list of custom handlers.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ReturnValueHandlerConfigurer {

	private final List<HandlerMethodReturnValueHandler> customHandlers = new ArrayList<>(8);


	/**
	 * Configure custom return value handlers for handler methods.
	 * @param handlers the handlers to add
	 */
	public void addCustomHandler(HandlerMethodReturnValueHandler... handlers) {
		Assert.notNull(handlers, "'handlers' must not be null");
		this.customHandlers.addAll(Arrays.asList(handlers));
	}


	public List<HandlerMethodReturnValueHandler> getCustomHandlers() {
		return this.customHandlers;
	}

}
