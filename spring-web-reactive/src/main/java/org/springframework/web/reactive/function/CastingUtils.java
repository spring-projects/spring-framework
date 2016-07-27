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

package org.springframework.web.reactive.function;

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * @author Arjen Poutsma
 */
@SuppressWarnings("unchecked")
abstract class CastingUtils {

	public static <T> HttpMessageReader<T> cast(HttpMessageReader<?> messageReader) {
		return (HttpMessageReader<T>) messageReader;
	}

	public static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}

	public static <T> HandlerFunction<T> cast(HandlerFunction<?> handlerFunction) {
		return (HandlerFunction<T>) handlerFunction;
	}


}
