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

package org.springframework.http.client.reactive;

import java.lang.reflect.Method;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Used to support Jetty 10.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.3.26
 */
abstract class Jetty10HttpFieldsHelper {

	private static final boolean jetty10Present;

	private static final Method requestGetHeadersMethod;

	private static final Method responseGetHeadersMethod;

	private static final Method getNameMethod;

	private static final Method getValueMethod;


	static {
		try {
			ClassLoader classLoader = JettyClientHttpResponse.class.getClassLoader();
			Class<?> httpFieldsClass = classLoader.loadClass("org.eclipse.jetty.http.HttpFields");
			jetty10Present = httpFieldsClass.isInterface();
			requestGetHeadersMethod = Request.class.getMethod("getHeaders");
			responseGetHeadersMethod = Response.class.getMethod("getHeaders");
			Class<?> httpFieldClass = classLoader.loadClass("org.eclipse.jetty.http.HttpField");
			getNameMethod = httpFieldClass.getMethod("getName");
			getValueMethod = httpFieldClass.getMethod("getValue");
		}
		catch (ClassNotFoundException | NoSuchMethodException ex) {
			throw new IllegalStateException("No compatible Jetty version found", ex);
		}
	}


	public static boolean jetty10Present() {
		return jetty10Present;
	}

	public static HttpHeaders getHttpHeaders(Request request) {
		Iterable<?> iterator = (Iterable<?>)
				ReflectionUtils.invokeMethod(requestGetHeadersMethod, request);
		return getHttpHeadersInternal(iterator);
	}

	public static HttpHeaders getHttpHeaders(Response response) {
		Iterable<?> iterator = (Iterable<?>)
				ReflectionUtils.invokeMethod(responseGetHeadersMethod, response);
		return getHttpHeadersInternal(iterator);
	}

	private static HttpHeaders getHttpHeadersInternal(@Nullable Iterable<?> iterator) {
		Assert.notNull(iterator, "Iterator must not be null");
		HttpHeaders headers = new HttpHeaders();
		for (Object field : iterator) {
			String name = (String) ReflectionUtils.invokeMethod(getNameMethod, field);
			Assert.notNull(name, "Header name must not be null");
			String value = (String) ReflectionUtils.invokeMethod(getValueMethod, field);
			headers.add(name, value);
		}
		return headers;
	}
}
