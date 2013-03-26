/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.oxm;

import java.lang.reflect.Type;

/**
 * Subinterface of {@link Unmarshaller} that has support for Java 5 generics.
 *
 * @author Arjen Poutsma
 * @since 3.0.1
 */
public interface GenericUnmarshaller extends Unmarshaller {

	/**
	 * Indicates whether this marshaller can marshal instances of the supplied generic type.
	 * @param genericType the type that this marshaller is being asked if it can marshal
	 * @return {@code true} if this marshaller can indeed marshal instances of the supplied type;
	 * {@code false} otherwise
	 */
	boolean supports(Type genericType);

}
