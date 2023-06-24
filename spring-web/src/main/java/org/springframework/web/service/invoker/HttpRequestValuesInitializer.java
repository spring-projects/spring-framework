/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Factory for {@link HttpRequestValues} with values extracted from the type and
 * method-level {@link HttpExchange @HttpRequest} annotations.
 *
 * @param httpMethod the HTTP method to use
 * @param url the URL to use
 * @param contentType the content type to use
 * @param acceptMediaTypes the accept media types to use
 * @author Freeman Lau
 * @since 6.1.0
 */
public record HttpRequestValuesInitializer(@Nullable HttpMethod httpMethod,
		@Nullable String url, @Nullable MediaType contentType,
		@Nullable List<MediaType> acceptMediaTypes) {

	public HttpRequestValues.Builder initializeRequestValuesBuilder() {
		HttpRequestValues.Builder requestValues = HttpRequestValues.builder();
		if (this.httpMethod != null) {
			requestValues.setHttpMethod(this.httpMethod);
		}
		if (this.url != null) {
			requestValues.setUriTemplate(this.url);
		}
		if (this.contentType != null) {
			requestValues.setContentType(this.contentType);
		}
		if (this.acceptMediaTypes != null) {
			requestValues.setAccept(this.acceptMediaTypes);
		}
		return requestValues;
	}

}
