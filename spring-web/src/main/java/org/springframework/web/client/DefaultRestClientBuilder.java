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

package org.springframework.web.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter;
import org.springframework.http.converter.cbor.KotlinSerializationCborHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.KotlinSerializationProtobufHttpMessageConverter;
import org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Default implementation of {@link RestClient.Builder}.
 *
 * @author Arjen Poutsma
 * @author Hyoungjune Kim
 * @author Sebastien Deleuze
 * @since 6.1
 */
final class DefaultRestClientBuilder implements RestClient.Builder {

	// request factories

	private static final boolean httpComponentsClientPresent;

	private static final boolean jettyClientPresent;

	private static final boolean reactorNettyClientPresent;

	private static final boolean jdkClientPresent;

	// message factories

	private static final boolean romePresent;

	private static final boolean jaxb2Present;

	private static final boolean jacksonPresent;

	private static final boolean jackson2Present;

	private static final boolean jacksonXmlPresent;

	private static final boolean jackson2XmlPresent;

	private static final boolean jacksonSmilePresent;

	private static final boolean jackson2SmilePresent;

	private static final boolean jacksonCborPresent;

	private static final boolean jackson2CborPresent;

	private static final boolean jacksonYamlPresent;

	private static final boolean jackson2YamlPresent;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean kotlinSerializationCborPresent;

	private static final boolean kotlinSerializationJsonPresent;

	private static final boolean kotlinSerializationProtobufPresent;

	static {
		ClassLoader loader = DefaultRestClientBuilder.class.getClassLoader();

		httpComponentsClientPresent = ClassUtils.isPresent("org.apache.hc.client5.http.classic.HttpClient", loader);
		jettyClientPresent = ClassUtils.isPresent("org.eclipse.jetty.client.HttpClient", loader);
		reactorNettyClientPresent = ClassUtils.isPresent("reactor.netty.http.client.HttpClient", loader);
		jdkClientPresent = ClassUtils.isPresent("java.net.http.HttpClient", loader);

		romePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", loader);
		jaxb2Present = ClassUtils.isPresent("jakarta.xml.bind.Binder", loader);
		jacksonPresent = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", loader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", loader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", loader);
		jacksonXmlPresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", loader);
		jackson2XmlPresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", loader);
		jacksonSmilePresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", loader);
		jackson2SmilePresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", loader);
		jacksonCborPresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", loader);
		jackson2CborPresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", loader);
		jacksonYamlPresent = jacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", loader);
		jackson2YamlPresent = jackson2Present && ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", loader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", loader);
		jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", loader);
		kotlinSerializationCborPresent = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", loader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", loader);
		kotlinSerializationProtobufPresent = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", loader);
	}

	private @Nullable String baseUrl;

	private @Nullable Map<String, ?> defaultUriVariables;

	private @Nullable UriBuilderFactory uriBuilderFactory;

	private @Nullable HttpHeaders defaultHeaders;

	private @Nullable MultiValueMap<String, String> defaultCookies;

	private @Nullable Object defaultApiVersion;

	private @Nullable ApiVersionInserter apiVersionInserter;

	private @Nullable Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest;

	private @Nullable List<StatusHandler> statusHandlers;

	private @Nullable List<ClientHttpRequestInterceptor> interceptors;

	private @Nullable BiPredicate<URI, HttpMethod> bufferingPredicate;

	private @Nullable List<ClientHttpRequestInitializer> initializers;

	private @Nullable ClientHttpRequestFactory requestFactory;

	private @Nullable List<HttpMessageConverter<?>> messageConverters;

	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	private @Nullable ClientRequestObservationConvention observationConvention;


	public DefaultRestClientBuilder() {
	}

	public DefaultRestClientBuilder(DefaultRestClientBuilder other) {
		Assert.notNull(other, "Other must not be null");

		this.baseUrl = other.baseUrl;
		this.defaultUriVariables = (other.defaultUriVariables != null ? new LinkedHashMap<>(other.defaultUriVariables) : null);
		this.uriBuilderFactory = other.uriBuilderFactory;
		if (other.defaultHeaders != null) {
			this.defaultHeaders = new HttpHeaders();
			this.defaultHeaders.putAll(other.defaultHeaders);
		}
		else {
			this.defaultHeaders = null;
		}
		this.defaultCookies = (other.defaultCookies != null ? new LinkedMultiValueMap<>(other.defaultCookies) : null);
		this.defaultApiVersion = other.defaultApiVersion;
		this.apiVersionInserter = other.apiVersionInserter;
		this.defaultRequest = other.defaultRequest;
		this.statusHandlers = (other.statusHandlers != null ? new ArrayList<>(other.statusHandlers) : null);
		this.interceptors = (other.interceptors != null) ? new ArrayList<>(other.interceptors) : null;
		this.bufferingPredicate = other.bufferingPredicate;
		this.initializers = (other.initializers != null) ? new ArrayList<>(other.initializers) : null;
		this.requestFactory = other.requestFactory;
		this.messageConverters = (other.messageConverters != null ? new ArrayList<>(other.messageConverters) : null);
		this.observationRegistry = other.observationRegistry;
		this.observationConvention = other.observationConvention;
	}

	public DefaultRestClientBuilder(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "RestTemplate must not be null");

		this.uriBuilderFactory = getUriBuilderFactory(restTemplate);
		this.statusHandlers = new ArrayList<>();
		this.statusHandlers.add(StatusHandler.fromErrorHandler(restTemplate.getErrorHandler()));
		if (!CollectionUtils.isEmpty(restTemplate.getInterceptors())) {
			this.interceptors = new ArrayList<>(restTemplate.getInterceptors());
		}
		this.bufferingPredicate = restTemplate.getBufferingPredicate();
		if (!CollectionUtils.isEmpty(restTemplate.getClientHttpRequestInitializers())) {
			this.initializers = new ArrayList<>(restTemplate.getClientHttpRequestInitializers());
		}
		this.requestFactory = getRequestFactory(restTemplate);
		this.messageConverters = new ArrayList<>(restTemplate.getMessageConverters());
		this.observationRegistry = restTemplate.getObservationRegistry();
		this.observationConvention = restTemplate.getObservationConvention();
	}

	private static @Nullable UriBuilderFactory getUriBuilderFactory(RestTemplate restTemplate) {
		UriTemplateHandler uriTemplateHandler = restTemplate.getUriTemplateHandler();
		if (uriTemplateHandler instanceof DefaultUriBuilderFactory builderFactory) {
			// only reuse the DefaultUriBuilderFactory if it has been customized
			if (hasRestTemplateDefaults(builderFactory)) {
				return null;
			}
			else {
				return builderFactory;
			}
		}
		else if (uriTemplateHandler instanceof UriBuilderFactory builderFactory) {
			return builderFactory;
		}
		else {
			return null;
		}
	}


	/**
	 * Indicate whether this {@code DefaultUriBuilderFactory} uses the default
	 * {@link org.springframework.web.client.RestTemplate RestTemplate} settings.
	 */
	private static boolean hasRestTemplateDefaults(DefaultUriBuilderFactory factory) {
		// see RestTemplate::initUriTemplateHandler
		return (!factory.hasBaseUri() &&
				factory.getEncodingMode() == DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT &&
				CollectionUtils.isEmpty(factory.getDefaultUriVariables()) &&
				factory.shouldParsePath());
	}

	private static ClientHttpRequestFactory getRequestFactory(RestTemplate restTemplate) {
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		if (requestFactory instanceof InterceptingClientHttpRequestFactory interceptingClientHttpRequestFactory) {
			return interceptingClientHttpRequestFactory.getDelegate();
		}
		else {
			return requestFactory;
		}
	}


	@Override
	public RestClient.Builder baseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		return this;
	}

	@Override
	public RestClient.Builder baseUrl(URI baseUrl) {
		this.baseUrl = baseUrl.toString();
		return this;
	}

	@Override
	public RestClient.Builder defaultUriVariables(Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables = defaultUriVariables;
		return this;
	}

	@Override
	public RestClient.Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory) {
		this.uriBuilderFactory = uriBuilderFactory;
		return this;
	}

	@Override
	public RestClient.Builder defaultHeader(String header, String... values) {
		initHeaders().put(header, Arrays.asList(values));
		return this;
	}

	@Override
	public RestClient.Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
		headersConsumer.accept(initHeaders());
		return this;
	}

	private HttpHeaders initHeaders() {
		if (this.defaultHeaders == null) {
			this.defaultHeaders = new HttpHeaders();
		}
		return this.defaultHeaders;
	}

	@Override
	public RestClient.Builder defaultCookie(String cookie, String... values) {
		initCookies().addAll(cookie, Arrays.asList(values));
		return this;
	}

	@Override
	public RestClient.Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
		cookiesConsumer.accept(initCookies());
		return this;
	}

	private MultiValueMap<String, String> initCookies() {
		if (this.defaultCookies == null) {
			this.defaultCookies = new LinkedMultiValueMap<>(3);
		}
		return this.defaultCookies;
	}

	@Override
	public RestClient.Builder defaultApiVersion(@Nullable Object version) {
		this.defaultApiVersion = version;
		return this;
	}

	@Override
	public RestClient.Builder apiVersionInserter(ApiVersionInserter apiVersionInserter) {
		this.apiVersionInserter = apiVersionInserter;
		return this;
	}

	@Override
	public RestClient.Builder defaultRequest(Consumer<RestClient.RequestHeadersSpec<?>> defaultRequest) {
		this.defaultRequest = this.defaultRequest != null ?
				this.defaultRequest.andThen(defaultRequest) : defaultRequest;
		return this;
	}

	@Override
	public RestClient.Builder defaultStatusHandler(Predicate<HttpStatusCode> statusPredicate, RestClient.ResponseSpec.ErrorHandler errorHandler) {
		return defaultStatusHandlerInternal(StatusHandler.of(statusPredicate, errorHandler));
	}

	@Override
	public RestClient.Builder defaultStatusHandler(ResponseErrorHandler errorHandler) {
		return defaultStatusHandlerInternal(StatusHandler.fromErrorHandler(errorHandler));
	}

	private RestClient.Builder defaultStatusHandlerInternal(StatusHandler statusHandler) {
		if (this.statusHandlers == null) {
			this.statusHandlers = new ArrayList<>();
		}
		this.statusHandlers.add(statusHandler);
		return this;
	}

	@Override
	public RestClient.Builder requestInterceptor(ClientHttpRequestInterceptor interceptor) {
		Assert.notNull(interceptor, "Interceptor must not be null");
		initInterceptors().add(interceptor);
		return this;
	}

	@Override
	public RestClient.Builder requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer) {
		interceptorsConsumer.accept(initInterceptors());
		return this;
	}

	private List<ClientHttpRequestInterceptor> initInterceptors() {
		if (this.interceptors == null) {
			this.interceptors = new ArrayList<>();
		}
		return this.interceptors;
	}

	@Override
	public RestClient.Builder bufferContent(BiPredicate<URI, HttpMethod> predicate) {
		this.bufferingPredicate = predicate;
		return this;
	}

	@Override
	public RestClient.Builder requestInitializer(ClientHttpRequestInitializer initializer) {
		Assert.notNull(initializer, "Initializer must not be null");
		initInitializers().add(initializer);
		return this;
	}

	@Override
	public RestClient.Builder requestInitializers(Consumer<List<ClientHttpRequestInitializer>> initializersConsumer) {
		initializersConsumer.accept(initInitializers());
		return this;
	}

	private List<ClientHttpRequestInitializer> initInitializers() {
		if (this.initializers == null) {
			this.initializers = new ArrayList<>();
		}
		return this.initializers;
	}


	@Override
	public RestClient.Builder requestFactory(ClientHttpRequestFactory requestFactory) {
		this.requestFactory = requestFactory;
		return this;
	}

	@Override
	public RestClient.Builder messageConverters(Consumer<List<HttpMessageConverter<?>>> configurer) {
		configurer.accept(initMessageConverters());
		validateConverters(this.messageConverters);
		return this;
	}

	@Override
	public RestClient.Builder messageConverters(List<HttpMessageConverter<?>> messageConverters) {
		validateConverters(messageConverters);
		this.messageConverters = Collections.unmodifiableList(messageConverters);
		return this;
	}

	@Override
	public RestClient.Builder observationRegistry(ObservationRegistry observationRegistry) {
		Assert.notNull(observationRegistry, "observationRegistry must not be null");
		this.observationRegistry = observationRegistry;
		return this;
	}

	@Override
	public RestClient.Builder observationConvention(ClientRequestObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
		return this;
	}

	@Override
	public RestClient.Builder apply(Consumer<RestClient.Builder> builderConsumer) {
		builderConsumer.accept(this);
		return this;
	}

	@SuppressWarnings("removal")
	private List<HttpMessageConverter<?>> initMessageConverters() {
		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<>();

			this.messageConverters.add(new ByteArrayHttpMessageConverter());
			this.messageConverters.add(new StringHttpMessageConverter());
			this.messageConverters.add(new ResourceHttpMessageConverter(false));
			this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());

			if (romePresent) {
				this.messageConverters.add(new AtomFeedHttpMessageConverter());
				this.messageConverters.add(new RssChannelHttpMessageConverter());
			}

			if (jacksonXmlPresent) {
				this.messageConverters.add(new JacksonXmlHttpMessageConverter());
			}
			else if (jackson2XmlPresent) {
				this.messageConverters.add(new MappingJackson2XmlHttpMessageConverter());
			}
			else if (jaxb2Present) {
				this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			}

			if (kotlinSerializationProtobufPresent) {
				this.messageConverters.add(new KotlinSerializationProtobufHttpMessageConverter());
			}

			if (jacksonPresent) {
				this.messageConverters.add(new JacksonJsonHttpMessageConverter());
			}
			else if (jackson2Present) {
				this.messageConverters.add(new MappingJackson2HttpMessageConverter());
			}
			else if (gsonPresent) {
				this.messageConverters.add(new GsonHttpMessageConverter());
			}
			else if (jsonbPresent) {
				this.messageConverters.add(new JsonbHttpMessageConverter());
			}
			else if (kotlinSerializationJsonPresent) {
				this.messageConverters.add(new KotlinSerializationJsonHttpMessageConverter());
			}

			if (jacksonSmilePresent) {
				this.messageConverters.add(new JacksonSmileHttpMessageConverter());
			}
			if (jackson2SmilePresent) {
				this.messageConverters.add(new MappingJackson2SmileHttpMessageConverter());
			}

			if (jacksonCborPresent) {
				this.messageConverters.add(new JacksonCborHttpMessageConverter());
			}
			else if (jackson2CborPresent) {
				this.messageConverters.add(new MappingJackson2CborHttpMessageConverter());
			}
			else if (kotlinSerializationCborPresent) {
				this.messageConverters.add(new KotlinSerializationCborHttpMessageConverter());
			}

			if (jacksonYamlPresent) {
				this.messageConverters.add(new JacksonYamlHttpMessageConverter());
			}
			else if (jackson2YamlPresent) {
				this.messageConverters.add(new MappingJackson2YamlHttpMessageConverter());
			}
		}
		return this.messageConverters;
	}

	private void validateConverters(@Nullable List<HttpMessageConverter<?>> messageConverters) {
		Assert.notEmpty(messageConverters, "At least one HttpMessageConverter is required");
		Assert.noNullElements(messageConverters, "The HttpMessageConverter list must not contain null elements");
	}


	@Override
	public RestClient.Builder clone() {
		return new DefaultRestClientBuilder(this);
	}

	@Override
	public RestClient build() {
		ClientHttpRequestFactory requestFactory = initRequestFactory();
		UriBuilderFactory uriBuilderFactory = initUriBuilderFactory();

		HttpHeaders defaultHeaders = copyDefaultHeaders();
		MultiValueMap<String, String> defaultCookies = copyDefaultCookies();

		List<HttpMessageConverter<?>> converters =
				(this.messageConverters != null ? this.messageConverters : initMessageConverters());

		return new DefaultRestClient(
				requestFactory, this.interceptors, this.bufferingPredicate, this.initializers,
				uriBuilderFactory, defaultHeaders, defaultCookies, this.defaultApiVersion,
				this.apiVersionInserter, this.defaultRequest,
				this.statusHandlers, converters,
				this.observationRegistry, this.observationConvention,
				new DefaultRestClientBuilder(this));
	}

	private ClientHttpRequestFactory initRequestFactory() {
		if (this.requestFactory != null) {
			return this.requestFactory;
		}
		else if (httpComponentsClientPresent) {
			return new HttpComponentsClientHttpRequestFactory();
		}
		else if (jettyClientPresent) {
			return new JettyClientHttpRequestFactory();
		}
		else if (reactorNettyClientPresent) {
			return new ReactorClientHttpRequestFactory();
		}
		else if (jdkClientPresent) {
			// java.net.http module might not be loaded, so we can't default to the JDK HttpClient
			return new JdkClientHttpRequestFactory();
		}
		else {
			return new SimpleClientHttpRequestFactory();
		}
	}

	private UriBuilderFactory initUriBuilderFactory() {
		if (this.uriBuilderFactory != null) {
			return this.uriBuilderFactory;
		}
		DefaultUriBuilderFactory factory = (this.baseUrl != null ?
				new DefaultUriBuilderFactory(this.baseUrl) : new DefaultUriBuilderFactory());
		factory.setDefaultUriVariables(this.defaultUriVariables);
		return factory;
	}

	private @Nullable HttpHeaders copyDefaultHeaders() {
		if (this.defaultHeaders == null) {
			return null;
		}
		HttpHeaders copy = new HttpHeaders();
		this.defaultHeaders.forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
		return HttpHeaders.readOnlyHttpHeaders(copy);
	}

	private @Nullable MultiValueMap<String, String> copyDefaultCookies() {
		if (this.defaultCookies == null) {
			return null;
		}
		MultiValueMap<String, String> copy = new LinkedMultiValueMap<>(this.defaultCookies.size());
		this.defaultCookies.forEach((key, values) -> copy.put(key, new ArrayList<>(values)));
		return CollectionUtils.unmodifiableMultiValueMap(copy);
	}

}
