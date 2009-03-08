/*
 * Copyright 2004-2009 the original author or authors.
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

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;

/**
 * A converter converts a source object of type S to a target of type T and back.
 * <p>
 * Implementations of this interface are thread-safe and can be shared. Converters are typically registered with and
 * accessed through a {@link ConversionService}.
 * </p>
 * @author Keith Donald
 */
public interface Converter<S, T> {

	/**
	 * Convert the source of type S to target type T.
	 * @param source the source object to convert, which must be an instance of S
	 * @return the converted object, which must be an instance of T
	 * @throws Exception an exception occurred performing the conversion; may be any checked exception, the conversion
	 * system will handle wrapping the failure in a {@link ConversionException} that provides a consistent type
	 * conversion error context
	 */
	public T convert(S source) throws Exception;

	/**
	 * Convert the target of type T back to source type S.
	 * @param target the target object to convert, which must be an instance of T
	 * @return the converted object, which must be an instance of S
	 * @throws Exception an exception occurred performing the conversion; may be any checked exception, the conversion
	 * system will handle wrapping the failure in a {@link ConversionException} that provides a consistent type
	 * conversion error context
	 */
	public S convertBack(T target) throws Exception;

}