/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.accept.ApiVersionHolder;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Resolves argument values of type {@link SemanticApiVersionParser.Version}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ApiVersionMethodArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (SemanticApiVersionParser.Version.class == parameter.nestedIfOptional().getNestedParameterType());
	}

	@Override
	public @Nullable Object resolveArgument(
			MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(request != null, "No HttpServletRequest");
		ApiVersionHolder versionHolder = (ApiVersionHolder) request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
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
