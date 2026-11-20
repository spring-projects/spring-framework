/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments annotated with an @{@link SessionAttribute}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestAttributeMethodArgumentResolver
 */
public class SessionAttributeMethodArgumentResolver extends AbstractNamedValueArgumentResolver {

	public SessionAttributeMethodArgumentResolver(ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry) {
		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(SessionAttribute.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		SessionAttribute ann = parameter.getParameterAnnotation(SessionAttribute.class);
		Assert.state(ann != null, "No SessionAttribute annotation");
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1290
	@Override
	protected Mono<Object> resolveName(String name, MethodParameter parameter, ServerWebExchange exchange) {
		return exchange.getSession().mapNotNull(session -> session.getAttribute(name));
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new MissingRequestValueException(
				name, parameter.getNestedParameterType(), "session attribute", parameter);
	}

}
