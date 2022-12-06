/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Resolves ServerWebExchange-related method argument values of the following types:
 * <ul>
 * <li>{@link ServerWebExchange}
 * <li>{@link ServerHttpRequest}
 * <li>{@link ServerHttpResponse}
 * <li>{@link HttpMethod}
 * <li>{@link Locale}
 * <li>{@link TimeZone}
 * <li>{@link ZoneId}
 * <li>{@link UriBuilder} or {@link UriComponentsBuilder} -- for building URL's
 * relative to the current request
 * </ul>
 *
 * <p>For the {@code WebSession} see {@link WebSessionMethodArgumentResolver}
 * and for the {@code Principal} see {@link PrincipalMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see WebSessionMethodArgumentResolver
 * @see PrincipalMethodArgumentResolver
 */
public class ServerWebExchangeMethodArgumentResolver extends HandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ServerWebExchangeMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkParameterTypeNoReactiveWrapper(parameter,
				type -> ServerWebExchange.class.isAssignableFrom(type) ||
						ServerHttpRequest.class.isAssignableFrom(type) ||
						ServerHttpResponse.class.isAssignableFrom(type) ||
						HttpMethod.class == type ||
						Locale.class == type ||
						TimeZone.class == type ||
						ZoneId.class == type ||
						UriBuilder.class == type || UriComponentsBuilder.class == type);
	}

	@Override
	public Object resolveArgumentValue(
			MethodParameter methodParameter, BindingContext context, ServerWebExchange exchange) {

		Class<?> paramType = methodParameter.getParameterType();
		if (ServerWebExchange.class.isAssignableFrom(paramType)) {
			return exchange;
		}
		else if (ServerHttpRequest.class.isAssignableFrom(paramType)) {
			return exchange.getRequest();
		}
		else if (ServerHttpResponse.class.isAssignableFrom(paramType)) {
			return exchange.getResponse();
		}
		else if (HttpMethod.class == paramType) {
			return exchange.getRequest().getMethod();
		}
		else if (Locale.class == paramType) {
			return exchange.getLocaleContext().getLocale();
		}
		else if (TimeZone.class == paramType) {
			LocaleContext localeContext = exchange.getLocaleContext();
			TimeZone timeZone = getTimeZone(localeContext);
			return (timeZone != null ? timeZone : TimeZone.getDefault());
		}
		else if (ZoneId.class == paramType) {
			LocaleContext localeContext = exchange.getLocaleContext();
			TimeZone timeZone = getTimeZone(localeContext);
			return (timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault());
		}
		else if (UriBuilder.class == paramType || UriComponentsBuilder.class == paramType) {
			URI uri = exchange.getRequest().getURI();
			String contextPath = exchange.getRequest().getPath().contextPath().value();
			return UriComponentsBuilder.fromUri(uri).replacePath(contextPath).replaceQuery(null);
		}
		else {
			// should never happen...
			throw new IllegalArgumentException("Unknown parameter type: " +
					paramType + " in method: " + methodParameter.getMethod());
		}
	}

	@Nullable
	private TimeZone getTimeZone(LocaleContext localeContext) {
		TimeZone timeZone = null;
		if (localeContext instanceof TimeZoneAwareLocaleContext timeZoneAwareLocaleContext) {
			timeZone = timeZoneAwareLocaleContext.getTimeZone();
		}
		return timeZone;
	}

}
