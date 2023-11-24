/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.oxm;

import java.io.IOException;

import javax.xml.transform.Result;

/**
 * Defines the contract for Object XML Mapping Marshallers. Implementations of this interface
 * can serialize a given Object to an XML Stream.
 *
 * <p>Although the {@code marshal} method accepts a {@code java.lang.Object} as its
 * first parameter, most {@code Marshaller} implementations cannot handle arbitrary
 * {@code Object}s. Instead, an object class must be registered with the marshaller,
 * or have a common base class.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see Unmarshaller
 */
public interface Marshaller {

	/**
	 * Indicate whether this marshaller can marshal instances of the supplied type.
	 * @param clazz the class that this marshaller is being asked if it can marshal
	 * @return {@code true} if this marshaller can indeed marshal instances of the supplied class;
	 * {@code false} otherwise
	 */
	boolean supports(Class<?> clazz);

	/**
	 * Marshal the object graph with the given root into the provided {@link Result}.
	 * @param graph the root of the object graph to marshal
	 * @param result the result to marshal to
	 * @throws IOException if an I/O error occurs
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 */
	void marshal(Object graph, Result result) throws IOException, XmlMappingException;

}
