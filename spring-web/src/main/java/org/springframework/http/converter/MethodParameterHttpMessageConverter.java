/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.http.converter;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * An HttpMessageConverter that supports converting the value returned from a
 * method by incorporating {@link org.springframework.core.MethodParameter}
 * information into the conversion. Such a converter can for example take into
 * account information from method-level annotations.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface MethodParameterHttpMessageConverter<T> extends HttpMessageConverter<T> {

	/**
	 * This method mirrors {@link HttpMessageConverter#canRead(Class, MediaType)}
	 * with an additional {@code MethodParameter}.
	 */
	boolean canRead(Class<?> clazz, MediaType mediaType, MethodParameter parameter);

	/**
	 * This method mirrors {@link HttpMessageConverter#canWrite(Class, MediaType)}
	 * with an additional {@code MethodParameter}.
	 */
	boolean canWrite(Class<?> clazz, MediaType mediaType, MethodParameter parameter);

	/**
	 * This method mirrors {@link HttpMessageConverter#read(Class, HttpInputMessage)}
	 * with an additional {@code MethodParameter}.
	 */
	T read(Class<? extends T> clazz, HttpInputMessage inputMessage, MethodParameter parameter)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * This method mirrors {@link HttpMessageConverter#write(Object, MediaType, HttpOutputMessage)}
	 * with an additional {@code MethodParameter}.
	 */
	void write(T t, MediaType contentType, HttpOutputMessage outputMessage, MethodParameter parameter)
			throws IOException, HttpMessageNotWritableException;

}
