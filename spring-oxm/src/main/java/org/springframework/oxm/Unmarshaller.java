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
import javax.xml.transform.Source;

/**
 * Defines the contract for Object XML Mapping unmarshallers. Implementations of this
 * interface can deserialize a given XML Stream to an Object graph.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see Marshaller
 */
public interface Unmarshaller {

	/**
	 * Indicate whether this unmarshaller can unmarshal instances of the supplied type.
	 * @param clazz the class that this unmarshaller is being asked if it can marshal
	 * @return {@code true} if this unmarshaller can indeed unmarshal to the supplied class;
	 * {@code false} otherwise
	 */
	boolean supports(Class<?> clazz);

	/**
	 * Unmarshal the given {@link Source} into an object graph.
	 * @param source the source to marshal from
	 * @return the object graph
	 * @throws IOException if an I/O error occurs
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 */
	Object unmarshal(Source source) throws IOException, XmlMappingException;

}
