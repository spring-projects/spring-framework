/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.net.URL;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriTemplate;

/**
 * An {@link HttpServiceArgumentResolver} that uses the provided
 * {@link UriBuilderFactory} to expand the {@link UriTemplate}.
 * <p>Unlike with the {@link UrlArgumentResolver},
 * if the {@link UriBuilderFactoryArgumentResolver} is provided,
 * it will not override the entire {@link URL}, but just the {@code baseUri}.
 * <p>This allows for dynamically setting the {@code baseUri},
 * while keeping the {@code path} specified through class
 * and method annotations.
 *
 * @author Olga Maciaszek-Sharma
 * @since 6.1
 */
public class UriBuilderFactoryArgumentResolver implements HttpServiceArgumentResolver {

	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		parameter = parameter.nestedIfOptional();

		if (!parameter.getNestedParameterType().equals(UriBuilderFactory.class)) {
			return false;
		}

		if (argument instanceof Optional<?> optionalValue) {
			argument = optionalValue.orElse(null);
		}

		if (argument == null) {
			Assert.isTrue(parameter.isOptional(), "UriBuilderFactory is required");
			return true;
		}

		requestValues.setUriBuilderFactory((UriBuilderFactory) argument);
		return true;
	}
}
