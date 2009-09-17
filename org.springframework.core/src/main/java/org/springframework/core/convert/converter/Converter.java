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

package org.springframework.core.convert.converter;

/**
 * A converter converts a source object of type S to a target of type T.
 * Implementations of this interface are thread-safe and can be shared.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface Converter<S, T> {

	/**
	 * Convert the source of type S to target type T.
	 * @param source the source object to convert, which must be an instance of S
	 * @return the converted object, which must be an instance of T
	 * @throws IllegalArgumentException if the source could not be converted to the desired target type
	 */
	T convert(S source);

}
