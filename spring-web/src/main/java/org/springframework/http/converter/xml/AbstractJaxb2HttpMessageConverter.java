/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.converter.xml;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;

/**
 * Abstract base class for {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters}
 * that use JAXB2. Creates {@link JAXBContext} object lazily.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <T> the converted object type
 */
public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);


	/**
	 * Create a new {@link Marshaller} for the given class.
	 * @param clazz the class to create the marshaller for
	 * @return the {@code Marshaller}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final Marshaller createMarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Marshaller marshaller = jaxbContext.createMarshaller();
			customizeMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException(
					"Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Customize the {@link Marshaller} created by this
	 * message converter before using it to write the object to the output.
	 * @param marshaller the marshaller to customize
	 * @since 4.0.3
	 * @see #createMarshaller(Class)
	 */
	protected void customizeMarshaller(Marshaller marshaller) {
	}

	/**
	 * Create a new {@link Unmarshaller} for the given class.
	 * @param clazz the class to create the unmarshaller for
	 * @return the {@code Unmarshaller}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			customizeUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException(
					"Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Customize the {@link Unmarshaller} created by this
	 * message converter before using it to read the object from the input.
	 * @param unmarshaller the unmarshaller to customize
	 * @since 4.0.3
	 * @see #createUnmarshaller(Class)
	 */
	protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
	}

	/**
	 * Return a {@link JAXBContext} for the given class.
	 * @param clazz the class to return the context for
	 * @return the {@code JAXBContext}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final JAXBContext getJaxbContext(Class<?> clazz) {
		return this.jaxbContexts.computeIfAbsent(clazz, key -> {
			try {
				return JAXBContext.newInstance(clazz);
			}
			catch (JAXBException ex) {
				throw new HttpMessageConversionException(
						"Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		});
	}

	/**
	 * Detect the charset from the given {@link HttpHeaders#getContentType()}.
	 * @param httpHeaders the current HTTP headers
	 * @return the charset defined in the content type header, or {@code null} if not found
	 */
	protected @Nullable Charset detectCharset(HttpHeaders httpHeaders) {
		MediaType contentType = httpHeaders.getContentType();
		if (contentType != null && contentType.getCharset() != null) {
			return contentType.getCharset();
		}
		else {
			return null;
		}
	}

}
