/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpCookie;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolve method arguments annotated with {@code @CookieValue}.
 *
 * <p>An {@code @CookieValue} is a named value that is resolved from a cookie.
 * It has a required flag and a default value to fall back on when the cookie
 * does not exist.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CookieValueMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {

	/**
	 * Create a new {@link CookieValueMethodArgumentResolver} instance.
	 * @param factory a bean factory to use for resolving {@code ${...}}
	 * placeholder and {@code #{...}} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to contain expressions
	 * @param registry for checking reactive type wrappers
	 */
	public CookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory factory,
			ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, CookieValue.class, (annot, type) -> true);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		CookieValue ann = parameter.getParameterAnnotation(CookieValue.class);
		Assert.state(ann != null, "No CookieValue annotation");
		return new CookieValueNamedValueInfo(ann);
	}

	@Override
	protected @Nullable Object resolveNamedValue(String name, MethodParameter parameter, ServerWebExchange exchange) {
		HttpCookie cookie = exchange.getRequest().getCookies().getFirst(name);
		Class<?> paramType = parameter.getNestedParameterType();
		if (HttpCookie.class.isAssignableFrom(paramType)) {
			return cookie;
		}
		return (cookie != null ? cookie.getValue() : null);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new MissingRequestValueException(
				name, parameter.getNestedParameterType(), "cookie", parameter);
	}


	private static final class CookieValueNamedValueInfo extends NamedValueInfo {

		private CookieValueNamedValueInfo(CookieValue annotation) {
			super(annotation.name(), annotation.required(), annotation.defaultValue());
		}
	}

}
