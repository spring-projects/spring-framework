/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import java.util.List;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;

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
