/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cache.interceptor;

/**
 * Wrapper/Unwrapper, it allows to unwrap values to be cached and wraps it back
 * in order to be consumed.
 * @author Pablo Diaz-Lopez
 */
interface CacheResultWrapper {
	/**
	 * Wraps to the wrapping target class
	 * @param value the value to wrap
	 * @return the value wrapped
	 */
	Object wrap(Object value);

	/**
	 * Unwraps a value and returns it decorated if it needs in order to
	 * notify the result, this will be the case if the wrapped value is not
	 * available at the moment (it is calculated asynchronously
	 * @param valueWrapped the value wrapped
	 * @param asyncResult it will call it when the value it's available
	 * @return the same value wrapped or a version decorated.
	 */
	Object unwrap(Object valueWrapped, AsyncWrapResult asyncResult);

	/**
	 * @return The target class the Wrapper can handle
	 */
	Class<?> getWrapClass();
}
