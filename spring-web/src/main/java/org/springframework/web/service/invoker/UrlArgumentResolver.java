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

package org.springframework.web.service.invoker;

import java.net.URI;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * {@link HttpServiceArgumentResolver} that resolves the URL for the request
 * from a {@link URI} argument.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class UrlArgumentResolver implements HttpServiceArgumentResolver {

	@Override
	public boolean resolve(
			@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

		if (!parameter.getParameterType().equals(URI.class)) {
			return false;
		}

		if (argument != null) {
			requestValues.setUri((URI) argument);
		}

		return true;
	}

}
