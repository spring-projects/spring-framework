/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolver for a {@link SessionStatus} argument obtaining it from the
 * {@link BindingContext}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class SessionStatusMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return SessionStatus.class == parameter.getParameterType();
	}

	@Override
	public @Nullable Object resolveArgumentValue(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		Assert.isInstanceOf(InitBinderBindingContext.class, bindingContext);
		return ((InitBinderBindingContext) bindingContext).getSessionStatus();
	}

}
