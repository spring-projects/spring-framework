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

package org.springframework.web.client.builder.parse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.builder.parse.model.InvocationDetails;
import org.springframework.web.client.builder.parse.model.RequestDetails;

public final class InvocationParser {

	private InvocationParser() {
	}

	public static RequestMethod getRequestMethod(final Method method) {
		if (findAnnotation(method, GetMapping.class).isPresent()) {
			return RequestMethod.GET;
		}
		else if (findAnnotation(method, PostMapping.class).isPresent()) {
			return RequestMethod.POST;
		}
		else if (findAnnotation(method, PutMapping.class).isPresent()) {
			return RequestMethod.PUT;
		}
		else if (findAnnotation(method, DeleteMapping.class).isPresent()) {
			return RequestMethod.DELETE;
		}
		else if (findAnnotation(method, PatchMapping.class).isPresent()) {
			return RequestMethod.PATCH;
		}
		else {
			final Optional<RequestMapping> requestMapping = findAnnotation(method, RequestMapping.class);
			if (requestMapping.isPresent()) {
				return requestMapping.get().method()[0];
			}
		}
		throw new RuntimeException("Cannot find request method of " + method.getName());
	}

	public static <T> Optional<T> findAnnotation(final Method method, final Class<T> findAnnotation) {
		final Annotation[] methodAnnotations = method.getAnnotations();
		return findAnnotation(methodAnnotations, findAnnotation);
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<T> findAnnotation(
			final Annotation[] methodAnnotations, final Class<T> annotations) {
		for (final Annotation annotation : methodAnnotations) {
			if (annotation.annotationType() == annotations) {
				return Optional.of((T) annotation);
			}
		}
		return Optional.empty();
	}

	public static <T> T getAnnotation(
			final Method method, final Class<T> clazz, final String message) {
		final Optional<T> requestMapping = InvocationParser.findAnnotation(method, clazz);
		if (!requestMapping.isPresent()) {
			throw new RuntimeException(message);
		}
		return requestMapping.get();
	}

	public static Map<String, String> getPathVariables(final Method method, final Object[] args) {
		final Map<String, String> map = new HashMap<>();

		for (int i = 0; i < method.getParameterCount(); i++) {
			final Parameter p = method.getParameters()[i];

			final PathVariable pv = p.getAnnotation(PathVariable.class);
			if (pv != null) {
				map.put(pv.value(), args[i].toString());
			}
		}
		return map;
	}

	public static MultiValueMap<String, String> getRequestVariables(
			final Method method, final Object[] args) {
		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		for (int i = 0; i < method.getParameterCount(); i++) {
			final Parameter p = method.getParameters()[i];

			final RequestParam rp = p.getAnnotation(RequestParam.class);
			if (rp != null) {
				final Object arg = args[i];
				if (arg instanceof List) {
					@SuppressWarnings("unchecked") final List<Object> arr = (List<Object>) arg;
					for (final Object element : arr) {
						map.add(rp.value(), element.toString());
					}
				}
				else if (arg.getClass().isArray()) {
					final Object[] arr = (Object[]) arg;
					for (final Object element : arr) {
						map.add(rp.value(), element.toString());
					}
				}
				else {
					map.add(rp.value(), args[i].toString());
				}
			}
		}
		return map;
	}

	private static MultiValueMap<String, String> getHeaderVariables(
			final Method method, final Object[] args) {
		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		for (int i = 0; i < method.getParameterCount(); i++) {
			final Parameter p = method.getParameters()[i];
			final RequestHeader rh = p.getAnnotation(RequestHeader.class);
			if (rh != null) {
				map.add(rh.value(), args[i].toString());
			}
		}
		return map;
	}

	public static Optional<Object> findReqestBody(final Method method, final Object[] args) {
		for (int i = 0; i < method.getParameterCount(); i++) {
			final Parameter p = method.getParameters()[i];
			final RequestBody rb = p.getAnnotation(RequestBody.class);
			if (rb != null) {
				return Optional.of(args[i]);
			}
		}
		return Optional.empty();
	}

	public static Type getGenericTypeOfMethod(final Object proxy, final Method method) {
		final ResolvableType r = ResolvableType.forMethodReturnType(method);
		return r.getGeneric(0).getType();
	}

	public static InvocationDetails getInvocationDetails(
			final Object proxy, final Method method, final Object[] args) throws ClassNotFoundException {
		final RequestDetails requestDetails = getRequestDetails(method);

		final MultiValueMap<String, String> queryParams =
				InvocationParser.getRequestVariables(method, args);

		final Map<String, String> pathVariables = InvocationParser.getPathVariables(method, args);

		final Optional<Object> requestBody = InvocationParser.findReqestBody(method, args);

		final boolean methodReurnTypeIsResponseEntity =
				method.getReturnType().isAssignableFrom(ResponseEntity.class);

		ParameterizedTypeReference<?> responseType = null;
		if (methodReurnTypeIsResponseEntity) {
			responseType =
					new ParameterizedTypeReference<Type>() {
						@Override
						public Type getType() {
							return InvocationParser.getGenericTypeOfMethod(proxy, method);
						}
					};
		}
		else {
			responseType =
					new ParameterizedTypeReference<Type>() {
						@Override
						public Type getType() {
							return method.getGenericReturnType();
						}
					};
		}

		final HttpHeaders headers = requestDetails.getHttpHeaders();
		final MultiValueMap<String, String> headerParams =
				InvocationParser.getHeaderVariables(method, args);
		for (final Entry<String, List<String>> header : headerParams.entrySet()) {
			for (final String value : header.getValue()) {
				headers.add(header.getKey(), value);
			}
		}

		return new InvocationDetails(
				requestDetails,
				queryParams,
				pathVariables,
				requestBody.orElse(null),
				methodReurnTypeIsResponseEntity,
				responseType,
				headers);
	}

	private static RequestDetails getRequestDetails(final Method method) {
		final Optional<RequestMapping> requestMapping =
				InvocationParser.findAnnotation(method, RequestMapping.class);
		if (requestMapping.isPresent()) {
			return RequestMappingParser.getRequestDetails(requestMapping.get());
		}
		throw new RuntimeException("Only RequestMapping is, currently, implemented. PR:s are welcome.");
	}
}
