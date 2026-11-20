/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.web.accept.ApiVersionHolder;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves argument values of type {@link SemanticApiVersionParser.Version}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ApiVersionMethodArgumentResolver implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (SemanticApiVersionParser.Version.class == parameter.nestedIfOptional().getNestedParameterType());
	}

	@Override
	public @Nullable Object resolveArgumentValue(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		ApiVersionHolder versionHolder = exchange.getRequiredAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
		Object version = versionHolder.getVersionIfPresent();

		if (parameter.getParameterType() == Optional.class) {
			return Optional.ofNullable(version);
		}

		if (version == null) {
			if (!parameter.isOptional()) {
				// typically this should be caught earlier in AbstractHandlerMapping
				throw new MissingApiVersionException();
			}
			return null;
		}

		if (version instanceof SemanticApiVersionParser.Version semanticApiVersion) {
			return semanticApiVersion;
		}

		// Should never happen
		throw new IllegalStateException("Unexpected version type: " + version.getClass());
	}

}
