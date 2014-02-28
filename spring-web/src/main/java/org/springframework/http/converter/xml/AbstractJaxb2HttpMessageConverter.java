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

package org.springframework.http.converter.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverters}
 * that use JAXB2. Creates {@link JAXBContext} object lazily.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<Class<?>, JAXBContext>(64);


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
	 * @see #createMarshaller(Class)
	 * @since 4.0.3
	 */
	protected void customizeMarshaller(Marshaller marshaller) {
	}

	/**
	 * Create a new {@link Unmarshaller} for the given class.
	 * @param clazz the class to create the unmarshaller for
	 * @return the {@code Unmarshaller}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final Unmarshaller createUnmarshaller(Class<?> clazz) throws JAXBException {
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
	 * @see #createUnmarshaller(Class)
	 * @since 4.0.3
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
		Assert.notNull(clazz, "'clazz' must not be null");
		JAXBContext jaxbContext = this.jaxbContexts.get(clazz);
		if (jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(clazz);
				this.jaxbContexts.putIfAbsent(clazz, jaxbContext);
			}
			catch (JAXBException ex) {
				throw new HttpMessageConversionException(
						"Could not instantiate JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		}
		return jaxbContext;
	}

}
