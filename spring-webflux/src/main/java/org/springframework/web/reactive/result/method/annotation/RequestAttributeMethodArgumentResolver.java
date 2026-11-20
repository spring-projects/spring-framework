/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments annotated with an @{@link RequestAttribute}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see SessionAttributeMethodArgumentResolver
 */
public class RequestAttributeMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * Create a new {@link RequestAttributeMethodArgumentResolver} instance.
	 * @param factory a bean factory to use for resolving {@code ${...}}
	 * placeholder and {@code #{...}} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to have expressions
	 * @param registry for checking reactive type wrappers
	 */
	public RequestAttributeMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return param.hasParameterAnnotation(RequestAttribute.class);
	}


	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestAttribute ann = parameter.getParameterAnnotation(RequestAttribute.class);
		Assert.state(ann != null, "No RequestAttribute annotation");
		return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
	}

	@Override
	protected @Nullable Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		Object value = exchange.getAttribute(name);
		ReactiveAdapter toAdapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		if (toAdapter != null) {
			if (value == null) {
				Assert.isTrue(toAdapter.supportsEmpty(),
						() -> "No request attribute '" + name + "' and target type " +
								parameter.getGenericParameterType() + " doesn't support empty values.");
				return toAdapter.fromPublisher(Mono.empty());
			}
			if (parameter.getParameterType().isInstance(value)) {
				return value;
			}
			ReactiveAdapter fromAdapter = getAdapterRegistry().getAdapter(value.getClass());
			Assert.isTrue(fromAdapter != null,
					() -> getClass().getSimpleName() + " doesn't support " +
							"reactive type wrapper: " + parameter.getGenericParameterType());
			return toAdapter.fromPublisher(fromAdapter.toPublisher(value));
		}
		return value;
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new MissingRequestValueException(
				name, parameter.getNestedParameterType(), "request attribute", parameter);
	}

}
