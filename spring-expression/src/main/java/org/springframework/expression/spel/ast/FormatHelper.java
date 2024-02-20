/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * Utility methods (formatters, etc) used during parsing and evaluation.
 *
 * @author Andy Clement
 * @author Sam Brannen
 */
abstract class FormatHelper {

	/**
	 * Produce a readable representation for a given method name with specified arguments.
	 * @param name the name of the method
	 * @param argumentTypes the types of the arguments to the method
	 * @return a nicely formatted representation &mdash; for example, {@code foo(java.lang.String,int)}
	 */
	static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (TypeDescriptor typeDescriptor : argumentTypes) {
			String className = (typeDescriptor != null ? formatClassNameForMessage(typeDescriptor.getType()) : "null");
			sj.add(className);
		}
		return name + sj;
	}

	/**
	 * Determine a readable name for a given Class object.
	 * <p>A String array will have the formatted name "java.lang.String[]".
	 * @param clazz the Class whose name is to be formatted
	 * @return a formatted String suitable for message inclusion
	 */
	static String formatClassNameForMessage(@Nullable Class<?> clazz) {
		return (clazz != null ? clazz.getTypeName() : "null");
	}

}
