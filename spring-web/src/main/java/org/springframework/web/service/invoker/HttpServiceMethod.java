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


import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.service.annotation.HttpRequest;


/**
 * Implements the invocation of an {@link HttpRequest @HttpRequest} annotated,
 * {@link HttpServiceProxyFactory#createService(Class) HTTP Service proxy} method
 * by delegating to an {@link HttpClientAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
final class HttpServiceMethod {

	private final Method method;

	private final MethodParameter[] parameters;

	private final List<HttpServiceMethodArgumentResolver> argumentResolvers;

	private final HttpRequestDefinitionFactory requestDefinitionFactory;

	private final ResponseFunction responseFunction;


	HttpServiceMethod(
			Method method, Class<?> containingClass, List<HttpServiceMethodArgumentResolver> argumentResolvers,
			HttpClientAdapter client, ReactiveAdapterRegistry reactiveRegistry,
			Duration blockTimeout) {

		this.method = method;
		this.parameters = initMethodParameters(method);
		this.argumentResolvers = argumentResolvers;
		this.requestDefinitionFactory = HttpRequestDefinitionFactory.create(method, containingClass);
		this.responseFunction = ResponseFunction.create(client, method, reactiveRegistry, blockTimeout);
	}

	private static MethodParameter[] initMethodParameters(Method method) {
		int count = method.getParameterCount();
		MethodParameter[] parameters = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			parameters[i] = new MethodParameter(method, i);
		}
		return parameters;
	}


	public Method getMethod() {
		return this.method;
	}


	@Nullable
	public Object invoke(Object[] arguments) {
		HttpRequestDefinition requestDefinition = this.requestDefinitionFactory.initializeRequest();
		applyArguments(requestDefinition, arguments);
		requestDefinition.setComplete();
		return this.responseFunction.execute(requestDefinition);
	}

	private void applyArguments(HttpRequestDefinition requestDefinition, Object[] arguments) {
		Assert.isTrue(arguments.length == this.parameters.length, "Method argument mismatch");
		for (int i = 0; i < this.parameters.length; i++) {
			Object argumentValue = arguments[i];
			for (HttpServiceMethodArgumentResolver resolver : this.argumentResolvers) {
				resolver.resolve(argumentValue, this.parameters[i], requestDefinition);
			}
		}
	}


	/**
	 * Factory for an {@link HttpRequestDefinition} with values extracted from
	 * the type and method-level {@link HttpRequest @HttpRequest} annotations.
	 */
	private record HttpRequestDefinitionFactory(
			@Nullable HttpMethod httpMethod, @Nullable String url,
			@Nullable MediaType contentType, @Nullable List<MediaType> acceptMediaTypes) {

		private HttpRequestDefinitionFactory(
				@Nullable HttpMethod httpMethod, @Nullable String url,
				@Nullable MediaType contentType, @Nullable List<MediaType> acceptMediaTypes) {

			this.url = url;
			this.httpMethod = httpMethod;
			this.contentType = contentType;
			this.acceptMediaTypes = acceptMediaTypes;
		}

		public HttpRequestDefinition initializeRequest() {
			HttpRequestDefinition requestDefinition = new HttpRequestDefinition();
			if (this.httpMethod != null) {
				requestDefinition.setHttpMethod(this.httpMethod);
			}
			if (this.url != null) {
				requestDefinition.setUriTemplate(this.url);
			}
			if (this.contentType != null) {
				requestDefinition.getHeaders().setContentType(this.contentType);
			}
			if (this.acceptMediaTypes != null) {
				requestDefinition.getHeaders().setAccept(this.acceptMediaTypes);
			}
			return requestDefinition;
		}


		/**
		 * Introspect the method and create the request factory for it.
		 */
		public static HttpRequestDefinitionFactory create(Method method, Class<?> containingClass) {

			HttpRequest annot1 = AnnotatedElementUtils.findMergedAnnotation(containingClass, HttpRequest.class);
			HttpRequest annot2 = AnnotatedElementUtils.findMergedAnnotation(method, HttpRequest.class);

			Assert.notNull(annot2, "Expected HttpRequest annotation");

			HttpMethod httpMethod = initHttpMethod(annot1, annot2);
			String url = initUrl(annot1, annot2);
			MediaType contentType = initContentType(annot1, annot2);
			List<MediaType> acceptableMediaTypes = initAccept(annot1, annot2);

			return new HttpRequestDefinitionFactory(httpMethod, url, contentType, acceptableMediaTypes);
		}


		@Nullable
		private static HttpMethod initHttpMethod(@Nullable HttpRequest typeAnnot, HttpRequest annot) {

			String value1 = (typeAnnot != null ? typeAnnot.method() : null);
			String value2 = annot.method();

			if (StringUtils.hasText(value2)) {
				return HttpMethod.valueOf(value2);
			}

			if (StringUtils.hasText(value1)) {
				return HttpMethod.valueOf(value1);
			}

			return null;
		}

		@Nullable
		private static String initUrl(@Nullable HttpRequest typeAnnot, HttpRequest annot) {

			String url1 = (typeAnnot != null ? typeAnnot.url() : null);
			String url2 = annot.url();

			boolean hasUrl1 = StringUtils.hasText(url1);
			boolean hasUrl2 = StringUtils.hasText(url2);

			if (hasUrl1 && hasUrl2) {
				return (url1 + (!url1.endsWith("/") && !url2.startsWith("/") ? "/" : "") + url2);
			}

			if (!hasUrl1 && !hasUrl2) {
				return null;
			}

			return (hasUrl2 ? url2 : url1);
		}

		@Nullable
		private static MediaType initContentType(@Nullable HttpRequest typeAnnot, HttpRequest annot) {

			String value1 = (typeAnnot != null ? typeAnnot.contentType() : null);
			String value2 = annot.contentType();

			if (StringUtils.hasText(value2)) {
				return MediaType.parseMediaType(value2);
			}

			if (StringUtils.hasText(value1)) {
				return MediaType.parseMediaType(value1);
			}

			return null;
		}

		@Nullable
		private static List<MediaType> initAccept(@Nullable HttpRequest typeAnnot, HttpRequest annot) {

			String[] value1 = (typeAnnot != null ? typeAnnot.accept() : null);
			String[] value2 = annot.accept();

			if (!ObjectUtils.isEmpty(value2)) {
				return MediaType.parseMediaTypes(Arrays.asList(value2));
			}

			if (!ObjectUtils.isEmpty(value1)) {
				return MediaType.parseMediaTypes(Arrays.asList(value1));
			}

			return null;
		}

	}


	/**
	 * Function to execute a request, obtain a response, and adapt to the expected
	 * return type blocking if necessary.
	 */
	private record ResponseFunction(
			Function<HttpRequestDefinition, Publisher<?>> responseFunction,
			@Nullable ReactiveAdapter returnTypeAdapter,
			boolean blockForOptional, Duration blockTimeout) {

		private ResponseFunction(
				Function<HttpRequestDefinition, Publisher<?>> responseFunction,
				@Nullable ReactiveAdapter returnTypeAdapter,
				boolean blockForOptional, Duration blockTimeout) {

			this.responseFunction = responseFunction;
			this.returnTypeAdapter = returnTypeAdapter;
			this.blockForOptional = blockForOptional;
			this.blockTimeout = blockTimeout;
		}

		@Nullable
		public Object execute(HttpRequestDefinition requestDefinition) {

			Publisher<?> responsePublisher = this.responseFunction.apply(requestDefinition);

			if (this.returnTypeAdapter != null) {
				return this.returnTypeAdapter.fromPublisher(responsePublisher);
			}

			return (this.blockForOptional ?
					((Mono<?>) responsePublisher).blockOptional(this.blockTimeout) :
					((Mono<?>) responsePublisher).block(this.blockTimeout));
		}


		/**
		 * Create the {@code ResponseFunction} that matches method return type.
		 */
		public static ResponseFunction create(
				HttpClientAdapter client, Method method, ReactiveAdapterRegistry reactiveRegistry,
				Duration blockTimeout) {

			MethodParameter returnParam = new MethodParameter(method, -1);
			Class<?> returnType = returnParam.getParameterType();
			ReactiveAdapter reactiveAdapter = reactiveRegistry.getAdapter(returnType);

			MethodParameter actualParam = (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
			Class<?> actualType = actualParam.getNestedParameterType();

			Function<HttpRequestDefinition, Publisher<?>> responseFunction;
			if (actualType.equals(void.class) || actualType.equals(Void.class)) {
				responseFunction = client::requestToVoid;
			}
			else if (reactiveAdapter != null && reactiveAdapter.isNoValue()) {
				responseFunction = client::requestToVoid;
			}
			else if (actualType.equals(HttpHeaders.class)) {
				responseFunction = client::requestToHeaders;
			}
			else if (actualType.equals(ResponseEntity.class)) {
				MethodParameter bodyParam = actualParam.nested();
				Class<?> bodyType = bodyParam.getNestedParameterType();
				if (bodyType.equals(Void.class)) {
					responseFunction = client::requestToBodilessEntity;
				}
				else {
					ReactiveAdapter bodyAdapter = reactiveRegistry.getAdapter(bodyType);
					responseFunction = initResponseEntityFunction(client, bodyParam, bodyAdapter);
				}
			}
			else {
				responseFunction = initBodyFunction(client, actualParam, reactiveAdapter);
			}

			boolean blockForOptional = actualType.equals(Optional.class);
			return new ResponseFunction(responseFunction, reactiveAdapter, blockForOptional, blockTimeout);
		}

		@SuppressWarnings("ConstantConditions")
		private static Function<HttpRequestDefinition, Publisher<?>> initResponseEntityFunction(
				HttpClientAdapter client, MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter) {

			if (reactiveAdapter == null) {
				return request -> client.requestToEntity(
						request, ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType()));
			}

			Assert.isTrue(reactiveAdapter.isMultiValue(),
					"ResponseEntity body must be a concrete value or a multi-value Publisher");

			ParameterizedTypeReference<?> bodyType =
					ParameterizedTypeReference.forType(methodParam.nested().getNestedGenericParameterType());

			// Shortcut for Flux
			if (reactiveAdapter.getReactiveType().equals(Flux.class)) {
				return request -> client.requestToEntityFlux(request, bodyType);
			}

			return request -> client.requestToEntityFlux(request, bodyType)
					.map(entity -> {
						Object body = reactiveAdapter.fromPublisher(entity.getBody());
						return new ResponseEntity<>(body, entity.getHeaders(), entity.getStatusCode());
					});
		}

		private static Function<HttpRequestDefinition, Publisher<?>> initBodyFunction(
				HttpClientAdapter client, MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter) {

			ParameterizedTypeReference<?> bodyType =
					ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType());

			return (reactiveAdapter != null && reactiveAdapter.isMultiValue() ?
					request -> client.requestToBodyFlux(request, bodyType) :
					request -> client.requestToBody(request, bodyType));
		}

	}

}
