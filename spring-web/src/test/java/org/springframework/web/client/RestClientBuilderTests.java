/*
 * Copyright 2002-present the original author or authors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Nicklas Wiegandt
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

	@Test
	void messageConvertersList() {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		RestClient.Builder builder = RestClient.builder();
		builder.messageConverters(List.of(stringConverter));

		assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
		DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

		assertThat(fieldValue("messageConverters", defaultBuilder))
				.asInstanceOf(InstanceOfAssertFactories.LIST)
				.containsExactly(stringConverter);
	}

	@Test
	void messageConvertersListEmpty() {
		RestClient.Builder builder = RestClient.builder();
		List<HttpMessageConverter<?>> converters = Collections.emptyList();
		assertThatIllegalArgumentException().isThrownBy(() -> builder.messageConverters(converters));
	}

	@Test
	void messageConvertersListWithNullElement() {
		RestClient.Builder builder = RestClient.builder();
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(null);
		assertThatIllegalArgumentException().isThrownBy(() -> builder.messageConverters(converters));
	}

	@Test
	void defaultCookieAddsCookieToDefaultCookiesMap() {
		RestClient.Builder builder = RestClient.builder();

		builder.defaultCookie("myCookie", "testValue");

		assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(Map.entry("myCookie", List.of("testValue")));
	}

	@Test
	void defaultCookieWithMultipleValuesAddsCookieToDefaultCookiesMap() {
		RestClient.Builder builder = RestClient.builder();

		builder.defaultCookie("myCookie", "testValue1", "testValue2");

		assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(Map.entry("myCookie", List.of("testValue1", "testValue2")));
	}

	@Test
	void defaultCookiesAllowsToAddCookie() {
		RestClient.Builder builder = RestClient.builder();
		builder.defaultCookie("firstCookie", "firstValue");

		builder.defaultCookies(cookies -> cookies.add("secondCookie", "secondValue"));

		assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(
						Map.entry("firstCookie", List.of("firstValue")),
						Map.entry("secondCookie", List.of("secondValue"))
				);
	}

	@Test
	void defaultCookiesAllowsToRemoveCookie() {
		RestClient.Builder builder = RestClient.builder();
		builder.defaultCookie("firstCookie", "firstValue");
		builder.defaultCookie("secondCookie", "secondValue");

		builder.defaultCookies(cookies -> cookies.remove("firstCookie"));

		assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(Map.entry("secondCookie", List.of("secondValue")));
	}

	@Test
	void copyConstructorCopiesDefaultCookies() {
		DefaultRestClientBuilder sourceBuilder = new DefaultRestClientBuilder();
		sourceBuilder.defaultCookie("firstCookie", "firstValue");
		sourceBuilder.defaultCookie("secondCookie", "secondValue");

		DefaultRestClientBuilder copiedBuilder = new DefaultRestClientBuilder(sourceBuilder);

		assertThat(fieldValue("defaultCookies", copiedBuilder))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(
						Map.entry("firstCookie", List.of("firstValue")),
						Map.entry("secondCookie", List.of("secondValue"))
				);
	}

	@Test
	void copyConstructorCopiesDefaultCookiesImmutable() {
		DefaultRestClientBuilder sourceBuilder = new DefaultRestClientBuilder();
		sourceBuilder.defaultCookie("firstCookie", "firstValue");
		sourceBuilder.defaultCookie("secondCookie", "secondValue");
		DefaultRestClientBuilder copiedBuilder = new DefaultRestClientBuilder(sourceBuilder);

		sourceBuilder.defaultCookie("thirdCookie", "thirdValue");

		assertThat(fieldValue("defaultCookies", copiedBuilder))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(
						Map.entry("firstCookie", List.of("firstValue")),
						Map.entry("secondCookie", List.of("secondValue"))
				);
	}

	@Test
	void buildCopiesDefaultCookies() {
		RestClient.Builder builder = RestClient.builder();
		builder.defaultCookie("firstCookie", "firstValue");
		builder.defaultCookie("secondCookie", "secondValue");

		RestClient restClient = builder.build();

		assertThat(fieldValue("defaultCookies", restClient))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(
						Map.entry("firstCookie", List.of("firstValue")),
						Map.entry("secondCookie", List.of("secondValue"))
				);
	}

	@Test
	void buildCopiesDefaultCookiesImmutable() {
		RestClient.Builder builder = RestClient.builder();
		builder.defaultCookie("firstCookie", "firstValue");
		builder.defaultCookie("secondCookie", "secondValue");
		RestClient restClient = builder.build();

		builder.defaultCookie("thirdCookie", "thirdValue");
		builder.defaultCookie("firstCookie", "fourthValue");

		assertThat(fieldValue("defaultCookies", restClient))
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsExactly(
						Map.entry("firstCookie", List.of("firstValue")),
						Map.entry("secondCookie", List.of("secondValue"))
				);
	}

	private static @Nullable Object fieldValue(String name, DefaultRestClientBuilder instance) {
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

	private static @Nullable Object fieldValue(String name, RestClient instance) {
		try {
			Field field = DefaultRestClient.class.getDeclaredField(name);
			field.setAccessible(true);

			return field.get(instance);
		}
		catch (NoSuchFieldException | IllegalAccessException ex) {
			fail(ex.getMessage(), ex);
			return null;
		}
	}
}
