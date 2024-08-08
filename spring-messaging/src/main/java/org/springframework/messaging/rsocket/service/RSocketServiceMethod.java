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

package org.springframework.messaging.rsocket.service;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Implements the invocation of an {@link RSocketExchange @RSocketExchange}-annotated,
 * {@link RSocketServiceProxyFactory#createClient(Class) RSocket service proxy} method
 * by delegating to an {@link RSocketRequester} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
final class RSocketServiceMethod {

	private final Method method;

	private final MethodParameter[] parameters;

	private final List<RSocketServiceArgumentResolver> argumentResolvers;

	@Nullable
	private final String route;

	private final Function<RSocketRequestValues, Object> responseFunction;


	RSocketServiceMethod(
			Method method, Class<?> containingClass, List<RSocketServiceArgumentResolver> argumentResolvers,
			RSocketRequester rsocketRequester, @Nullable StringValueResolver embeddedValueResolver,
			ReactiveAdapterRegistry reactiveRegistry, @Nullable Duration blockTimeout) {

		this.method = method;
		this.parameters = initMethodParameters(method);
		this.argumentResolvers = argumentResolvers;
		this.route = initRoute(method, containingClass, rsocketRequester.strategies(), embeddedValueResolver);
		this.responseFunction = initResponseFunction(rsocketRequester, method, reactiveRegistry, blockTimeout);
	}

	private static MethodParameter[] initMethodParameters(Method method) {
		int count = method.getParameterCount();
		if (count == 0) {
			return new MethodParameter[0];
		}
		DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
		MethodParameter[] parameters = new MethodParameter[count];
		for (int i = 0; i < count; i++) {
			parameters[i] = new SynthesizingMethodParameter(method, i);
			parameters[i].initParameterNameDiscovery(nameDiscoverer);
		}
		return parameters;
	}

	@Nullable
	@SuppressWarnings("NullAway")
	private static String initRoute(
			Method method, Class<?> containingClass, RSocketStrategies strategies,
			@Nullable StringValueResolver embeddedValueResolver) {

		RSocketExchange annot1 = AnnotatedElementUtils.findMergedAnnotation(containingClass, RSocketExchange.class);
		RSocketExchange annot2 = AnnotatedElementUtils.findMergedAnnotation(method, RSocketExchange.class);

		Assert.notNull(annot2, "Expected RSocketExchange annotation");

		String route1 = (annot1 != null ? annot1.value() : null);
		String route2 = annot2.value();

		if (embeddedValueResolver != null) {
			route1 = (route1 != null ? embeddedValueResolver.resolveStringValue(route1) : null);
			route2 = embeddedValueResolver.resolveStringValue(route2);
		}

		boolean hasRoute1 = StringUtils.hasText(route1);
		boolean hasRoute2 = StringUtils.hasText(route2);

		if (hasRoute1 && hasRoute2) {
			return strategies.routeMatcher().combine(route1, route2);
		}

		if (!hasRoute1 && !hasRoute2) {
			return null;
		}

		return (hasRoute2 ? route2 : route1);
	}

	private static Function<RSocketRequestValues, Object> initResponseFunction(
			RSocketRequester requester, Method method,
			ReactiveAdapterRegistry reactiveRegistry, @Nullable Duration blockTimeout) {

		MethodParameter returnParam = new MethodParameter(method, -1);
		Class<?> returnType = returnParam.getParameterType();
		ReactiveAdapter reactiveAdapter = reactiveRegistry.getAdapter(returnType);

		MethodParameter actualParam = (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
		Class<?> actualType = actualParam.getNestedParameterType();

		Function<RSocketRequestValues, Publisher<?>> responseFunction;
		if (ClassUtils.isVoidType(actualType) || (reactiveAdapter != null && reactiveAdapter.isNoValue())) {
			responseFunction = values -> {
				RSocketRequester.RetrieveSpec retrieveSpec = initRequest(requester, values);
				return (values.getPayload() == null && values.getPayloadValue() == null ?
						((RSocketRequester.RequestSpec) retrieveSpec).sendMetadata() : retrieveSpec.send());
			};
		}
		else if (reactiveAdapter == null) {
			responseFunction = values -> initRequest(requester, values).retrieveMono(actualType);
		}
		else {
			ParameterizedTypeReference<?> payloadType =
					ParameterizedTypeReference.forType(actualParam.getNestedGenericParameterType());

			responseFunction = values -> (
					reactiveAdapter.isMultiValue() ?
							initRequest(requester, values).retrieveFlux(payloadType) :
							initRequest(requester, values).retrieveMono(payloadType));
		}

		boolean blockForOptional = returnType.equals(Optional.class);

		return responseFunction.andThen(responsePublisher -> {
			if (reactiveAdapter != null) {
				return reactiveAdapter.fromPublisher(responsePublisher);
			}
			if (blockForOptional) {
				return (blockTimeout != null ?
						((Mono<?>) responsePublisher).blockOptional(blockTimeout) :
						((Mono<?>) responsePublisher).blockOptional());
			}
			else {
				return (blockTimeout != null ?
						((Mono<?>) responsePublisher).block(blockTimeout) :
						((Mono<?>) responsePublisher).block());
			}
		});
	}

	@SuppressWarnings("ReactiveStreamsUnusedPublisher")
	private static RSocketRequester.RetrieveSpec initRequest(
			RSocketRequester requester, RSocketRequestValues requestValues) {

		RSocketRequester.RequestSpec spec;
		String route = requestValues.getRoute();
		Map<Object, MimeType> metadata = requestValues.getMetadata();

		if (StringUtils.hasText(route)) {
			spec = requester.route(route, requestValues.getRouteVariables());
			for (Map.Entry<Object, MimeType> entry : metadata.entrySet()) {
				spec.metadata(entry.getKey(), entry.getValue());
			}
		}
		else {
			Iterator<Map.Entry<Object, MimeType>> iterator = metadata.entrySet().iterator();
			Assert.isTrue(iterator.hasNext(), "Neither route nor metadata provided");

			Map.Entry<Object, MimeType> entry = iterator.next();
			spec = requester.metadata(entry.getKey(), entry.getValue());

			while (iterator.hasNext()) {
				spec.metadata(entry.getKey(), entry.getValue());
			}
		}

		if (requestValues.getPayloadValue() != null) {
			spec.data(requestValues.getPayloadValue());
		}
		else if (requestValues.getPayload() != null) {
			Assert.notNull(requestValues.getPayloadElementType(), "Publisher body element type is required");
			spec.data(requestValues.getPayload(), requestValues.getPayloadElementType());
		}

		return spec;
	}


	public Method getMethod() {
		return this.method;
	}

	@Nullable
	public Object invoke(Object[] arguments) {
		RSocketRequestValues.Builder requestValues = RSocketRequestValues.builder(this.route);
		applyArguments(requestValues, arguments);
		return this.responseFunction.apply(requestValues.build());
	}

	private void applyArguments(RSocketRequestValues.Builder requestValues, Object[] arguments) {
		Assert.isTrue(arguments.length == this.parameters.length, "Method argument mismatch");
		for (int i = 0; i < arguments.length; i++) {
			Object value = arguments[i];
			boolean resolved = false;
			for (RSocketServiceArgumentResolver resolver : this.argumentResolvers) {
				if (resolver.resolve(value, this.parameters[i], requestValues)) {
					resolved = true;
					break;
				}
			}
			int index = i;
			Assert.state(resolved, () -> formatArgumentError(this.parameters[index], "No suitable resolver"));
		}
	}

	@SuppressWarnings("SameParameterValue")
	private static String formatArgumentError(MethodParameter param, String message) {
		return "Could not resolve parameter [" + param.getParameterIndex() + "] in " +
				param.getExecutable().toGenericString() + (StringUtils.hasText(message) ? ": " + message : "");
	}

}
