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

package org.springframework.http.codec.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
final class JaxbContextContainer {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts =
			new ConcurrentHashMap<>(64);

	public Marshaller createMarshaller(Class<?> clazz) throws JAXBException {
		JAXBContext jaxbContext = getJaxbContext(clazz);
		return jaxbContext.createMarshaller();
	}

	public Unmarshaller createUnmarshaller(Class<?> clazz) throws JAXBException {
		JAXBContext jaxbContext = getJaxbContext(clazz);
		return jaxbContext.createUnmarshaller();
	}

	private JAXBContext getJaxbContext(Class<?> clazz) throws JAXBException {
		Assert.notNull(clazz, "'clazz' must not be null");
		JAXBContext jaxbContext = this.jaxbContexts.get(clazz);
		if (jaxbContext == null) {
			jaxbContext = JAXBContext.newInstance(clazz);
			this.jaxbContexts.putIfAbsent(clazz, jaxbContext);
		}
		return jaxbContext;
	}

}
