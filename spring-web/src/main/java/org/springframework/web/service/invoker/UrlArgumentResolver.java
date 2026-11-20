/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.net.URI;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;

/**
 * {@link HttpServiceArgumentResolver} that resolves the URL for the request
 * from a {@link URI} argument.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 6.0
 */
public class UrlArgumentResolver implements HttpServiceArgumentResolver {

	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		parameter = parameter.nestedIfOptional();

		if (!parameter.getNestedParameterType().equals(URI.class)) {
			return false;
		}

		if (argument instanceof Optional<?> optionalValue) {
			argument = optionalValue.orElse(null);
		}

		if (argument == null) {
			Assert.isTrue(parameter.isOptional(), "URI is required");
			return true;
		}

		requestValues.setUri((URI) argument);
		return true;
	}

}
