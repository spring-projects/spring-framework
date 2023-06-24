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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implements the invocation of an annotated
 * {@link HttpServiceProxyFactory#createClient(Class) HTTP service proxy} method
 * by delegating to an {@link HttpClientAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Freeman Lau
 * @since 6.0
 */
final class HttpServiceMethod {

	private final Method method;

	private final MethodParameter[] parameters;

	private final List<HttpServiceArgumentResolver> argumentResolvers;

	private final HttpRequestValuesInitializer requestValuesInitializer;

	private final ResponseFunction responseFunction;

	HttpServiceMethod(
			Method method, List<HttpServiceArgumentResolver> argumentResolvers,
			HttpRequestValuesInitializer requestValuesInitializer, HttpClientAdapter client,
			ReactiveAdapterRegistry reactiveRegistry, @Nullable Duration blockTimeout) {

		this.method = method;
		this.parameters = initMethodParameters(method);
		this.argumentResolvers = argumentResolvers;
		this.requestValuesInitializer = requestValuesInitializer;
		this.responseFunction = ResponseFunction.create(client, method, reactiveRegistry, blockTimeout);
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


	@Nullable
	public Object invoke(Object[] arguments) {
		HttpRequestValues.Builder requestValues = this.requestValuesInitializer.initializeRequestValuesBuilder();
		applyArguments(requestValues, arguments);
		return this.responseFunction.execute(requestValues.build());
	}

	private void applyArguments(HttpRequestValues.Builder requestValues, Object[] arguments) {
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
							this.parameters[index].getExecutable().toGenericString() +
							(StringUtils.hasText("No suitable resolver") ? ": " + "No suitable resolver" : ""));
		}
	}

	/**
	 * Function to execute a request, obtain a response, and adapt to the expected
	 * return type, blocking if necessary.
	 */
	private record ResponseFunction(
			Function<HttpRequestValues, Publisher<?>> responseFunction,
			@Nullable ReactiveAdapter returnTypeAdapter,
			boolean blockForOptional, @Nullable Duration blockTimeout) {

		@Nullable
		public Object execute(HttpRequestValues requestValues) {

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
		 * Create the {@code ResponseFunction} that matches the method's return type.
		 */
		public static ResponseFunction create(
				HttpClientAdapter client, Method method, ReactiveAdapterRegistry reactiveRegistry,
				@Nullable Duration blockTimeout) {

			MethodParameter returnParam = new MethodParameter(method, -1);
			Class<?> returnType = returnParam.getParameterType();
			boolean isSuspending = KotlinDetector.isSuspendingFunction(method);
			if (isSuspending) {
				returnType = Mono.class;
			}

			ReactiveAdapter reactiveAdapter = reactiveRegistry.getAdapter(returnType);

			MethodParameter actualParam = (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
			Class<?> actualType = isSuspending ? actualParam.getParameterType() : actualParam.getNestedParameterType();

			Function<HttpRequestValues, Publisher<?>> responseFunction;
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
				MethodParameter bodyParam = isSuspending ? actualParam : actualParam.nested();
				Class<?> bodyType = bodyParam.getNestedParameterType();
				if (bodyType.equals(Void.class)) {
					responseFunction = client::requestToBodilessEntity;
				}
				else {
					ReactiveAdapter bodyAdapter = reactiveRegistry.getAdapter(bodyType);
					responseFunction = initResponseEntityFunction(client, bodyParam, bodyAdapter, isSuspending);
				}
			}
			else {
				responseFunction = initBodyFunction(client, actualParam, reactiveAdapter, isSuspending);
			}

			boolean blockForOptional = returnType.equals(Optional.class);
			return new ResponseFunction(responseFunction, reactiveAdapter, blockForOptional, blockTimeout);
		}

		@SuppressWarnings("ConstantConditions")
		private static Function<HttpRequestValues, Publisher<?>> initResponseEntityFunction(HttpClientAdapter client,
				MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter, boolean isSuspending) {

			if (reactiveAdapter == null) {
				return request -> client.requestToEntity(
						request, ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType()));
			}

			Assert.isTrue(reactiveAdapter.isMultiValue(),
					"ResponseEntity body must be a concrete value or a multi-value Publisher");

			ParameterizedTypeReference<?> bodyType =
					ParameterizedTypeReference.forType(isSuspending ? methodParam.nested().getGenericParameterType() :
							methodParam.nested().getNestedGenericParameterType());

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

		private static Function<HttpRequestValues, Publisher<?>> initBodyFunction(HttpClientAdapter client,
				MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter, boolean isSuspending) {

			ParameterizedTypeReference<?> bodyType =
					ParameterizedTypeReference.forType(isSuspending ? methodParam.getGenericParameterType() :
							methodParam.getNestedGenericParameterType());

			return (reactiveAdapter != null && reactiveAdapter.isMultiValue() ?
					request -> client.requestToBodyFlux(request, bodyType) :
					request -> client.requestToBody(request, bodyType));
		}

	}

}
