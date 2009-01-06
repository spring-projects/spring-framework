/*
 * Copyright 2007 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Extension of the {@link Marshaller} interface that supports Java 5 generics. More specifically, this marshaller adds
 * support for the new {@link Type} hierarchy, returned by methods such as {@link Method#getGenericParameterTypes()} and
 * {@link Method#getGenericReturnType()}.
 *
 * @author Arjen Poutsma
 * @since 1.0.2
 */
public interface GenericMarshaller extends Marshaller {

    /**
     * Indicates whether this marshaller can marshal instances of the supplied type.
     *
     * @param type the type that this marshaller is being asked if it can marshal
     * @return <code>true</code> if this marshaller can indeed marshal instances of the supplied type;
     *         <code>false</code> otherwise
     */
    boolean supports(Type type);
}
