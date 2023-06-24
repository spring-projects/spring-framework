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

/**
 * Resolve a method to generate http request metadata, such as http method, url, content type, accept media types.
 * @author Freeman Lau
 * @since 6.1.0
 */
public interface HttpRequestValuesResolver {

	/**
	 * Whether this resolver supports the given method.
	 * @param method the method to check
	 * @return {@code true} if this processor supports the given method
	 */
	boolean supports(Method method);

	/**
	 * Resolve the given method and return the {@link HttpRequestValuesInitializer}.
	 * @param method      the method to process
	 * @param serviceType http client interface type
	 * @return the {@link HttpRequestValuesInitializer} to use for the given method
	 */
	HttpRequestValuesInitializer resolve(Method method, Class<?> serviceType);

}
