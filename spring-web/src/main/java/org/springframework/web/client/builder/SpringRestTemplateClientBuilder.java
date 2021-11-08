/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client.builder;

import java.lang.reflect.Proxy;

import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.builder.handler.SpringRestTemplateClientInvocationHandler;

public final class SpringRestTemplateClientBuilder {
	private SpringRestTemplateClientBuilder() {
	}

	public static <T> Builder<T> springRestTemplateClientBuilder(final Class<T> clazz) {
		return new Builder<>(clazz);
	}

	public static <T> Builder<T> create(final Class<T> clazz) {
		return springRestTemplateClientBuilder(clazz);
	}

	public static class Builder<T> {
		private final Class<T> api;
		private RestTemplate restTemplate = new RestTemplate();
		private HttpHeaders headers = new HttpHeaders();
		private String url;

		public Builder(final Class<T> api) {
			if (api == null) {
				throw new IllegalArgumentException("api cannot be null");
			}
			this.api = api;
		}

		public Builder<T> setRestTemplate(final RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
			return this;
		}

		public Builder<T> setUrl(final String url) {
			this.url = url;
			return this;
		}

		public Builder<T> setHeaders(final HttpHeaders headers) {
			this.headers = headers;
			return this;
		}

		public Builder<T> setHeader(final String headerName, final String headerValue) {
			this.headers.add(headerName, headerValue);
			return this;
		}

		@SuppressWarnings("unchecked")
		public T build() {
			if (this.url == null) {
				throw new IllegalArgumentException("url cannot be null");
			}
			final SpringRestTemplateClientInvocationHandler<T> invocationHandler =
					new SpringRestTemplateClientInvocationHandler<>(
							this.url, this.restTemplate, this.headers);

			return (T)
					Proxy.newProxyInstance(
							this.api.getClassLoader(), new Class<?>[]{this.api}, invocationHandler);
		}
	}
}
