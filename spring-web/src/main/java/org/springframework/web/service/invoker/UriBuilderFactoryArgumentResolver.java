/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.net.URL;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
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
