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
 * A generic converter that conditionally executes.
 *
 * <p>Applies a rule that determines if a converter between a set of
 * {@link #getConvertibleTypes() convertible types} matches given a client request to
 * convert between a source field of convertible type S and a target field of convertible type T.
 *
 * <p>Often used to selectively match custom conversion logic based on the presence of
 * a field or class-level characteristic, such as an annotation or method. For example,
 * when converting from a String field to a Date field, an implementation might return
 * <code>true</code> if the target field has also been annotated with <code>@DateTimeFormat</code>.
 *
 * <p>As another example, when converting from a String field to an Account field,
 * an implementation might return true if the target Account class defines a
 * <code>public static findAccount(String)</code> method.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface ConditionalGenericConverter extends GenericConverter {

	/**
	 * Should the converter from <code>sourceType</code> to <code>targetType</code>
	 * currently under consideration be selected?
	 * @param sourceType the type descriptor of the field we are converting from
	 * @param targetType the type descriptor of the field we are converting to
	 * @return true if conversion should be performed, false otherwise
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);

}
