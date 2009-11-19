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

import org.springframework.core.convert.TypeDescriptor;

/**
 * Generic converter interface for converting between two or more types.
 * <p>
 * This is the most flexible of the Converter SPI interfaces, but also the most complex.
 * It is flexible in that a GenericConverter may support converting between multiple source/target type pairs (see {@link #getConvertibleTypes()}.
 * In addition, GenericConverter implementations have access to source/target {@link TypeDescriptor field context} during the type conversion process.
 * This allows for resolving source and target field metadata such as annotations and generics information, which can be used influence the conversion logic.
 * <p>
 * This interface should generally not be used when the simpler {@link Converter} or {@link ConverterFactory} interfaces are sufficient.
 *
 * @author Keith Donald
 * @since 3.0
 * @see TypeDescriptor
 * @see Converter
 * @see ConverterFactory
 */
public interface GenericConverter {

	/**
	 * The source and target types this converter can convert between.
	 * Each entry in the returned array is a convertible source-to-target type pair, also expressed as an array.
	 * For each pair, the first array element is a sourceType that can be converted from, and the second array element is a targetType that can be converted to.
	 */
	public Class<?>[][] getConvertibleTypes();

	/**
	 * Convert the source to the targetType described by the TypeDescriptor.
	 * @param source the source object to convert (may be null)
	 * @param sourceType the type descriptor of the field we are converting from
	 * @param targetType the type descriptor of the field we are converting to
	 * @return the converted object
	 */
	Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);

}
