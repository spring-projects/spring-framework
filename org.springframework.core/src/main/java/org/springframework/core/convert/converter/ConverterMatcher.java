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
 * A rule that determines if a particular Converter of S to T matches given a request to convert between a field of type S and a field of type T.
 * Often used to selectively apply custom type conversion logic based on the presence of a field annotation.
 * For example, when converting from a String to a Date field, an implementation might return true only if the target Date field has also been annotated with <code>@DateTimeFormat</code>.
 * @author Keith Donald
 * @since 3.0
 * TODO - consider collapsing into ConditionalGenericConverter
 */
public interface ConverterMatcher {

	/**
	 * Should the Converter from <code>sourceType</code> to <code>targetType</code> currently under consideration be selected?
	 * @param sourceType the type descriptor of the field we are converting from
	 * @param targetType the type descriptor of the field we are converting to
	 * @return true if conversion should be performed, false otherwise
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);
}
