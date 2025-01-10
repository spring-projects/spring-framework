/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * {@link HttpServiceArgumentResolver} that resolves the target
 * request's HTTP headers from an {@link HttpHeaders} argument.
 *
 * @author Yanming Zhou
 */
public class HttpHeadersArgumentResolver implements HttpServiceArgumentResolver {

	private static final Log logger = LogFactory.getLog(HttpHeadersArgumentResolver.class);


	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		parameter = parameter.nestedIfOptional();

		if (!parameter.getNestedParameterType().equals(HttpHeaders.class)) {
			return false;
		}

		if (argument instanceof Optional<?> optionalValue) {
			argument = optionalValue.orElse(null);
		}

		if (argument == null) {
			Assert.isTrue(parameter.isOptional(), "HttpHeaders is required");
			return true;
		}

		((HttpHeaders) argument).forEach((name, values) ->
				requestValues.addHeader(name, values.toArray(new String[0])));

		return true;
	}

}
