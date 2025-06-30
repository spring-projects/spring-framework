/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.springframework.core.codec.CodecException;

/**
 * Holder for {@link JAXBContext} instances.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
final class JaxbContextContainer {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);


	public Marshaller createMarshaller(Class<?> clazz) throws CodecException, JAXBException {
		JAXBContext jaxbContext = getJaxbContext(clazz);
		return jaxbContext.createMarshaller();
	}

	public Unmarshaller createUnmarshaller(Class<?> clazz) throws CodecException, JAXBException {
		JAXBContext jaxbContext = getJaxbContext(clazz);
		return jaxbContext.createUnmarshaller();
	}

	private JAXBContext getJaxbContext(Class<?> clazz) throws CodecException {
		return this.jaxbContexts.computeIfAbsent(clazz, key -> {
			try {
				return createJaxbContext(clazz);
			}
			catch (JAXBException ex) {
				throw new CodecException(
						"Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		});
	}

	/**
	 * Create a {@link JAXBContext} for the given type, exposing the class
	 * ClassLoader as current thread context ClassLoader for the time of
	 * creating the context.
	 */
	private JAXBContext createJaxbContext(Class<?> clazz) throws JAXBException {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
			return JAXBContext.newInstance(clazz);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

}
