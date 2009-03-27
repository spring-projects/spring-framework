/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

/**
 * Strategy interface that specifies a converter can convert from and to HTTP requests and responses.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface HttpMessageConverter<T> {

	/**
	 * Indicate whether the given class is supported by this converter.
	 *
	 * @param clazz the class to test for support
	 * @return <code>true</code> if supported; <code>false</code> otherwise
	 */
	boolean supports(Class<? extends T> clazz);

	/** Return the list of {@link MediaType} objects supported by this converter. */
	List<MediaType> getSupportedMediaTypes();

	/**
	 * Read an object of the given type form the given input message, and returns it.
	 *
	 * @param clazz the type of object to return
	 * @param inputMessage the HTTP input message to read from
	 * @return the converted object
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotReadableException in case of conversion errors
	 */
	T read(Class<T> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException;

	/**
	 * Write an given object to the given output message.
	 *
	 * @param t the object to write to the output message
	 * @param outputMessage the message to write to
	 * @throws IOException in case of I/O errors
	 * @throws HttpMessageNotWritableException in case of conversion errors
	 */
	void write(T t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException;

}
