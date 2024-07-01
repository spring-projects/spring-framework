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

package org.springframework.web.client;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Arjen Poutsma
 */
public class RestClientBuilderTests {

	@SuppressWarnings("unchecked")
	@Test
	void createFromRestTemplate() {
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory();
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("baseUri");
		ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();
		List<HttpMessageConverter<?>> restTemplateMessageConverters = List.of(new StringHttpMessageConverter());
		ClientHttpRequestInterceptor interceptor = new BasicAuthenticationInterceptor("foo", "bar");
		ClientHttpRequestInitializer initializer = request -> {};

		RestTemplate restTemplate = new RestTemplate(requestFactory);
		restTemplate.setUriTemplateHandler(uriBuilderFactory);
		restTemplate.setErrorHandler(errorHandler);
		restTemplate.setMessageConverters(restTemplateMessageConverters);
		restTemplate.setInterceptors(List.of(interceptor));
		restTemplate.setClientHttpRequestInitializers(List.of(initializer));

		RestClient.Builder builder = RestClient.builder(restTemplate);
		assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
		DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

		assertThat(fieldValue("requestFactory", defaultBuilder)).isSameAs(requestFactory);
		assertThat(fieldValue("uriBuilderFactory", defaultBuilder)).isSameAs(uriBuilderFactory);

		List<StatusHandler> statusHandlers = (List<StatusHandler>) fieldValue("statusHandlers", defaultBuilder);
		assertThat(statusHandlers).hasSize(1);

		List<HttpMessageConverter<?>> restClientMessageConverters =
				(List<HttpMessageConverter<?>>) fieldValue("messageConverters", defaultBuilder);
		assertThat(restClientMessageConverters).containsExactlyElementsOf(restClientMessageConverters);

		List<ClientHttpRequestInterceptor> interceptors =
				(List<ClientHttpRequestInterceptor>) fieldValue("interceptors", defaultBuilder);
		assertThat(interceptors).containsExactly(interceptor);

		List<ClientHttpRequestInitializer> initializers =
				(List<ClientHttpRequestInitializer>) fieldValue("initializers", defaultBuilder);
		assertThat(initializers).containsExactly(initializer);
	}

	@Test
	void defaultUriBuilderFactory() {
		RestTemplate restTemplate = new RestTemplate();

		RestClient.Builder builder = RestClient.builder(restTemplate);
		assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
		DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

		assertThat(fieldValue("uriBuilderFactory", defaultBuilder)).isNull();
	}

	@Test
	void defaultUri() {
		URI baseUrl = URI.create("https://example.org");
		RestClient.Builder builder = RestClient.builder();
		builder.baseUrl(baseUrl);

		assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
		DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

		assertThat(fieldValue("baseUrl", defaultBuilder)).isEqualTo(baseUrl.toString());
	}

	@Nullable
	private static Object fieldValue(String name, DefaultRestClientBuilder instance) {
		try {
			Field field = DefaultRestClientBuilder.class.getDeclaredField(name);
			field.setAccessible(true);

			return field.get(instance);
		}
		catch (NoSuchFieldException | IllegalAccessException ex) {
			fail(ex.getMessage(), ex);
			return null;
		}
	}
}
