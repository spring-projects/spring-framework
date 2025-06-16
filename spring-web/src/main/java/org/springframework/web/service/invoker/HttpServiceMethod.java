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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotationPredicates;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Implements the invocation of an {@link HttpExchange @HttpExchange}-annotated,
 * {@link HttpServiceProxyFactory#createClient(Class) HTTP service proxy} method
 * by delegating to an {@link HttpExchangeAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @author Sam Brannen
 * @since 6.0
 */
final class HttpServiceMethod {

	private static final boolean REACTOR_PRESENT =
			ClassUtils.isPresent("reactor.core.publisher.Mono", HttpServiceMethod.class.getClassLoader());


	private final Method method;

	private final MethodParameter[] parameters;

	private final List<HttpServiceArgumentResolver> argumentResolvers;

	private final HttpRequestValues.Processor requestValuesProcessor;

	private final HttpRequestValuesInitializer requestValuesInitializer;

	private final ResponseFunction responseFunction;


	HttpServiceMethod(
			Method method, Class<?> containingClass, List<HttpServiceArgumentResolver> argumentResolvers,
			HttpRequestValues.Processor valuesProcessor, HttpExchangeAdapter adapter,
			@Nullable StringValueResolver embeddedValueResolver) {

		this.method = method;
		this.parameters = initMethodParameters(method);
		this.argumentResolvers = argumentResolvers;
		this.requestValuesProcessor = valuesProcessor;

		boolean isReactorAdapter = (REACTOR_PRESENT && adapter instanceof ReactorHttpExchangeAdapter);

		this.requestValuesInitializer =
				HttpRequestValuesInitializer.create(
						method, containingClass, embeddedValueResolver,
						(isReactorAdapter ? ReactiveHttpRequestValues::builder : HttpRequestValues::builder));

		this.responseFunction = (isReactorAdapter ?
				ReactorExchangeResponseFunction.create((ReactorHttpExchangeAdapter) adapter, method) :
				ExchangeResponseFunction.create(adapter, method));
	}

	private static MethodParameter[] initMethodParameters(Method method) {
		int count = method.getParameterCount();
		if (count == 0) {
			return new MethodParameter[0];
		}
		if (KotlinDetector.isSuspendingFunction(method)) {
			count -= 1;
		}

		DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
		MethodParameter[] parameters = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			parameters[i] = new SynthesizingMethodParameter(method, i);
			parameters[i].initParameterNameDiscovery(nameDiscoverer);
		}
		return parameters;
	}


	public Method getMethod() {
		return this.method;
	}


	public @Nullable Object invoke(@Nullable Object[] arguments) {
		HttpRequestValues.Builder requestValues = this.requestValuesInitializer.initializeRequestValuesBuilder();
		applyArguments(requestValues, arguments);
		this.requestValuesProcessor.process(this.method, arguments, requestValues);
		return this.responseFunction.execute(requestValues.build());
	}

	private void applyArguments(HttpRequestValues.Builder requestValues, @Nullable Object[] arguments) {
		Assert.isTrue(arguments.length == this.parameters.length, "Method argument mismatch");
		for (int i = 0; i < arguments.length; i++) {
			Object value = arguments[i];
			boolean resolved = false;
			for (HttpServiceArgumentResolver resolver : this.argumentResolvers) {
				if (resolver.resolve(value, this.parameters[i], requestValues)) {
					resolved = true;
					break;
				}
			}
			int index = i;
			Assert.state(resolved, () ->
					"Could not resolve parameter [" + this.parameters[index].getParameterIndex() + "] in " +
							this.parameters[index].getExecutable().toGenericString() + ": No suitable resolver");
		}
	}


	/**
	 * Factory for {@link HttpRequestValues} with values extracted from the type
	 * and method-level {@link HttpExchange @HttpExchange} annotations.
	 */
	private record HttpRequestValuesInitializer(
			@Nullable HttpMethod httpMethod, @Nullable String url,
			@Nullable MediaType contentType, @Nullable List<MediaType> acceptMediaTypes,
			MultiValueMap<String, String> headers, @Nullable String version,
			Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

		public HttpRequestValues.Builder initializeRequestValuesBuilder() {
			HttpRequestValues.Builder requestValues = this.requestValuesSupplier.get();
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
			this.headers.forEach((name, values) ->
					values.forEach(value -> requestValues.addHeader(name, value)));
			if (this.version != null) {
				requestValues.setApiVersion(this.version);
			}
			return requestValues;
		}


		/**
		 * Introspect the method and create the request factory for it.
		 */
		public static HttpRequestValuesInitializer create(
				Method method, Class<?> containingClass, @Nullable StringValueResolver embeddedValueResolver,
				Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

			List<AnnotationDescriptor> methodHttpExchanges = getAnnotationDescriptors(method);
			Assert.state(!methodHttpExchanges.isEmpty(),
					() -> "Expected @HttpExchange annotation on method " + method);
			Assert.state(methodHttpExchanges.size() == 1,
					() -> "Multiple @HttpExchange annotations found on method %s, but only one is allowed: %s"
							.formatted(method, methodHttpExchanges));

			List<AnnotationDescriptor> typeHttpExchanges = getAnnotationDescriptors(containingClass);
			Assert.state(typeHttpExchanges.size() <= 1,
					() -> "Multiple @HttpExchange annotations found on %s, but only one is allowed: %s"
							.formatted(containingClass, typeHttpExchanges));

			HttpExchange methodAnnotation = methodHttpExchanges.get(0).httpExchange;
			HttpExchange typeAnnotation = (!typeHttpExchanges.isEmpty() ? typeHttpExchanges.get(0).httpExchange : null);

			HttpMethod httpMethod = initHttpMethod(typeAnnotation, methodAnnotation);
			String url = initUrl(typeAnnotation, methodAnnotation, embeddedValueResolver);
			MediaType contentType = initContentType(typeAnnotation, methodAnnotation);
			List<MediaType> acceptableMediaTypes = initAccept(typeAnnotation, methodAnnotation);
			MultiValueMap<String, String> headers = initHeaders(typeAnnotation, methodAnnotation, embeddedValueResolver);
			String version = initVersion(typeAnnotation, methodAnnotation);

			return new HttpRequestValuesInitializer(
					httpMethod, url, contentType, acceptableMediaTypes, headers, version,
					requestValuesSupplier);
		}

		private static @Nullable HttpMethod initHttpMethod(@Nullable HttpExchange typeAnnotation, HttpExchange methodAnnotation) {
			String methodLevelMethod = methodAnnotation.method();
			if (StringUtils.hasText(methodLevelMethod)) {
				return HttpMethod.valueOf(methodLevelMethod);
			}

			String typeLevelMethod = (typeAnnotation != null ? typeAnnotation.method() : null);
			if (StringUtils.hasText(typeLevelMethod)) {
				return HttpMethod.valueOf(typeLevelMethod);
			}

			return null;
		}

		@SuppressWarnings("NullAway") // Dataflow analysis limitation
		private static @Nullable String initUrl(
				@Nullable HttpExchange typeAnnotation, HttpExchange methodAnnotation,
				@Nullable StringValueResolver embeddedValueResolver) {

			String typeLevelUrl = (typeAnnotation != null ? typeAnnotation.url() : null);
			String methodLevelUrl = methodAnnotation.url();

			if (embeddedValueResolver != null) {
				typeLevelUrl = (typeLevelUrl != null ? embeddedValueResolver.resolveStringValue(typeLevelUrl) : null);
				methodLevelUrl = embeddedValueResolver.resolveStringValue(methodLevelUrl);
			}

			boolean hasTypeLevelUrl = StringUtils.hasText(typeLevelUrl);
			boolean hasMethodLevelUrl = StringUtils.hasText(methodLevelUrl);

			if (hasTypeLevelUrl && hasMethodLevelUrl) {
				return (typeLevelUrl + (!typeLevelUrl.endsWith("/") && !methodLevelUrl.startsWith("/") ? "/" : "") + methodLevelUrl);
			}

			if (!hasTypeLevelUrl && !hasMethodLevelUrl) {
				return null;
			}

			return (hasMethodLevelUrl ? methodLevelUrl : typeLevelUrl);
		}

		private static @Nullable MediaType initContentType(
				@Nullable HttpExchange typeAnnotation, HttpExchange methodAnnotation) {

			String methodLevelContentType = methodAnnotation.contentType();
			if (StringUtils.hasText(methodLevelContentType)) {
				return MediaType.parseMediaType(methodLevelContentType);
			}

			String typeLevelContentType = (typeAnnotation != null ? typeAnnotation.contentType() : null);
			if (StringUtils.hasText(typeLevelContentType)) {
				return MediaType.parseMediaType(typeLevelContentType);
			}

			return null;
		}

		private static @Nullable List<MediaType> initAccept(
				@Nullable HttpExchange typeAnnotation, HttpExchange methodAnnotation) {

			String[] methodLevelAccept = methodAnnotation.accept();
			if (!ObjectUtils.isEmpty(methodLevelAccept)) {
				return MediaType.parseMediaTypes(List.of(methodLevelAccept));
			}

			String[] typeLevelAccept = (typeAnnotation != null ? typeAnnotation.accept() : null);
			if (!ObjectUtils.isEmpty(typeLevelAccept)) {
				return MediaType.parseMediaTypes(List.of(typeLevelAccept));
			}

			return null;
		}

		private static MultiValueMap<String, String> initHeaders(
				@Nullable HttpExchange typeAnnotation, HttpExchange methodAnnotation,
				@Nullable StringValueResolver embeddedValueResolver) {

			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			if (typeAnnotation != null) {
				addHeaders(typeAnnotation.headers(), embeddedValueResolver, headers);
			}
			addHeaders(methodAnnotation.headers(), embeddedValueResolver, headers);
			return headers;
		}

		private static @Nullable String initVersion(
				@Nullable HttpExchange typeAnnotation, HttpExchange methodAnnotation) {

			if (StringUtils.hasText(methodAnnotation.version())) {
				return methodAnnotation.version();
			}
			if (typeAnnotation != null && StringUtils.hasText(typeAnnotation.version())) {
				return typeAnnotation.version();
			}
			return null;
		}

		private static void addHeaders(
				String[] rawValues, @Nullable StringValueResolver embeddedValueResolver,
				MultiValueMap<String, String> outputHeaders) {

			for (String rawValue: rawValues) {
				String[] pair = StringUtils.split(rawValue, "=");
				if (pair == null) {
					continue;
				}
				String name = pair[0].trim();
				List<String> values = new ArrayList<>();
				for (String value : StringUtils.commaDelimitedListToSet(pair[1])) {
					if (embeddedValueResolver != null) {
						value = embeddedValueResolver.resolveStringValue(value);
					}
					if (value != null) {
						value = value.trim();
						values.add(value);
					}
				}
				if (!values.isEmpty()) {
					outputHeaders.addAll(name, values);
				}
			}
		}

		private static List<AnnotationDescriptor> getAnnotationDescriptors(AnnotatedElement element) {
			return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
					.stream(HttpExchange.class)
					.filter(MergedAnnotationPredicates.firstRunOf(MergedAnnotation::getAggregateIndex))
					.map(AnnotationDescriptor::new)
					.distinct()
					.toList();
		}


		private static class AnnotationDescriptor {

			private final HttpExchange httpExchange;
			private final MergedAnnotation<?> root;

			AnnotationDescriptor(MergedAnnotation<HttpExchange> mergedAnnotation) {
				this.httpExchange = mergedAnnotation.synthesize();
				this.root = mergedAnnotation.getRoot();
			}

			@Override
			public boolean equals(Object obj) {
				return (obj instanceof AnnotationDescriptor that && this.httpExchange.equals(that.httpExchange));
			}

			@Override
			public int hashCode() {
				return this.httpExchange.hashCode();
			}

			@Override
			public String toString() {
				return this.root.synthesize().toString();
			}
		}

	}


	/**
	 * Execute a request, obtain a response, and adapt to the expected return type.
	 */
	private interface ResponseFunction {

		@Nullable Object execute(HttpRequestValues requestValues);

	}

	private record ExchangeResponseFunction(
			Function<HttpRequestValues, @Nullable Object> responseFunction) implements ResponseFunction {

		@Override
		public @Nullable Object execute(HttpRequestValues requestValues) {
			return this.responseFunction.apply(requestValues);
		}


		/**
		 * Create the {@code ResponseFunction} that matches the method return type.
		 */
		public static ResponseFunction create(HttpExchangeAdapter client, Method method) {
			if (KotlinDetector.isSuspendingFunction(method)) {
				throw new IllegalStateException(
						"Kotlin Coroutines are only supported with reactive implementations");
			}

			MethodParameter param = new MethodParameter(method, -1).nestedIfOptional();
			Class<?> paramType = param.getNestedParameterType();

			Function<HttpRequestValues, @Nullable Object> responseFunction;
			if (ClassUtils.isVoidType(paramType)) {
				responseFunction = requestValues -> {
					client.exchange(requestValues);
					return null;
				};
			}
			else if (paramType.equals(HttpHeaders.class)) {
				responseFunction = request -> asOptionalIfNecessary(client.exchangeForHeaders(request), param);
			}
			else if (paramType.equals(ResponseEntity.class)) {
				MethodParameter bodyParam = param.nested();
				if (bodyParam.getNestedParameterType().equals(Void.class)) {
					responseFunction = request ->
							asOptionalIfNecessary(client.exchangeForBodilessEntity(request), param);
				}
				else {
					ParameterizedTypeReference<?> bodyTypeRef =
							ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
					responseFunction = request ->
							asOptionalIfNecessary(client.exchangeForEntity(request, bodyTypeRef), param);
				}
			}
			else {
				ParameterizedTypeReference<?> bodyTypeRef =
						ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
				responseFunction = request ->
						asOptionalIfNecessary(client.exchangeForBody(request, bodyTypeRef), param);
			}

			return new ExchangeResponseFunction(responseFunction);
		}

		private static @Nullable Object asOptionalIfNecessary(@Nullable Object response, MethodParameter param) {
			return param.getParameterType().equals(Optional.class) ? Optional.ofNullable(response) : response;
		}
	}


	/**
	 * {@link ResponseFunction} for {@link ReactorHttpExchangeAdapter}.
	 */
	private record ReactorExchangeResponseFunction(
			Function<HttpRequestValues, Publisher<?>> responseFunction,
			@Nullable ReactiveAdapter returnTypeAdapter,
			boolean blockForOptional, @Nullable Duration blockTimeout) implements ResponseFunction {

		@Override
		public @Nullable Object execute(HttpRequestValues requestValues) {

			Publisher<?> responsePublisher = this.responseFunction.apply(requestValues);

			if (this.returnTypeAdapter != null) {
				return this.returnTypeAdapter.fromPublisher(responsePublisher);
			}

			if (this.blockForOptional) {
				return (this.blockTimeout != null ?
						((Mono<?>) responsePublisher).blockOptional(this.blockTimeout) :
						((Mono<?>) responsePublisher).blockOptional());
			}
			else {
				return (this.blockTimeout != null ?
						((Mono<?>) responsePublisher).block(this.blockTimeout) :
						((Mono<?>) responsePublisher).block());
			}
		}


		/**
		 * Create the {@code ResponseFunction} that matches the method return type.
		 */
		public static ResponseFunction create(ReactorHttpExchangeAdapter client, Method method) {
			MethodParameter returnParam = new MethodParameter(method, -1);
			Class<?> returnType = returnParam.getParameterType();
			boolean isSuspending = KotlinDetector.isSuspendingFunction(method);
			if (isSuspending) {
				returnType = Mono.class;
			}

			ReactiveAdapter reactiveAdapter = client.getReactiveAdapterRegistry().getAdapter(returnType);

			MethodParameter actualParam = (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
			Class<?> actualType = isSuspending ? actualParam.getParameterType() : actualParam.getNestedParameterType();

			Function<HttpRequestValues, Publisher<?>> responseFunction;
			if (ClassUtils.isVoidType(actualType)) {
				responseFunction = client::exchangeForMono;
			}
			else if (reactiveAdapter != null && reactiveAdapter.isNoValue()) {
				responseFunction = client::exchangeForMono;
			}
			else if (actualType.equals(HttpHeaders.class)) {
				responseFunction = client::exchangeForHeadersMono;
			}
			else if (actualType.equals(ResponseEntity.class)) {
				MethodParameter bodyParam = isSuspending ? actualParam : actualParam.nested();
				Class<?> bodyType = bodyParam.getNestedParameterType();
				if (bodyType.equals(Void.class)) {
					responseFunction = client::exchangeForBodilessEntityMono;
				}
				else {
					ReactiveAdapter bodyAdapter = client.getReactiveAdapterRegistry().getAdapter(bodyType);
					responseFunction = initResponseEntityFunction(client, bodyParam, bodyAdapter, isSuspending);
				}
			}
			else {
				responseFunction = initBodyFunction(client, actualParam, reactiveAdapter, isSuspending);
			}

			return new ReactorExchangeResponseFunction(
					responseFunction, reactiveAdapter, returnType.equals(Optional.class), client.getBlockTimeout());
		}

		@SuppressWarnings("ConstantConditions")
		private static Function<HttpRequestValues, Publisher<?>> initResponseEntityFunction(
				ReactorHttpExchangeAdapter client, MethodParameter methodParam,
				@Nullable ReactiveAdapter reactiveAdapter, boolean isSuspending) {

			if (reactiveAdapter == null) {
				return request -> client.exchangeForEntityMono(
						request, ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType()));
			}

			Assert.isTrue(reactiveAdapter.isMultiValue(),
					"ResponseEntity body must be a concrete value or a multi-value Publisher");

			ParameterizedTypeReference<?> bodyType =
					ParameterizedTypeReference.forType(isSuspending ? methodParam.nested().getGenericParameterType() :
							methodParam.nested().getNestedGenericParameterType());

			// Shortcut for Flux
			if (reactiveAdapter.getReactiveType().equals(Flux.class)) {
				return request -> client.exchangeForEntityFlux(request, bodyType);
			}

			return request -> client.exchangeForEntityFlux(request, bodyType)
					.map(entity -> {
						Flux<?> entityBody = entity.getBody();
						Assert.state(entityBody != null, "Entity body must not be null");
						Object body = reactiveAdapter.fromPublisher(entityBody);
						return new ResponseEntity<>(body, entity.getHeaders(), entity.getStatusCode());
					});
		}

		private static Function<HttpRequestValues, Publisher<?>> initBodyFunction(
				ReactorHttpExchangeAdapter client, MethodParameter methodParam,
				@Nullable ReactiveAdapter reactiveAdapter, boolean isSuspending) {

			ParameterizedTypeReference<?> bodyType =
					ParameterizedTypeReference.forType(isSuspending ? methodParam.getGenericParameterType() :
							methodParam.getNestedGenericParameterType());

			return (reactiveAdapter != null && reactiveAdapter.isMultiValue() ?
					request -> client.exchangeForBodyFlux(request, bodyType) :
					request -> client.exchangeForBodyMono(request, bodyType));
		}
	}

}
