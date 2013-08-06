/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.util.List;

import org.springframework.core.convert.TypeDescriptor;

/**
 * Utility methods (formatters, etc) used during parsing and evaluation.
 *
 * @author Andy Clement
 */
public class FormatHelper {

	/**
	 * Produce a nice string for a given method name with specified arguments.
	 * @param name the name of the method
	 * @param argumentTypes the types of the arguments to the method
	 * @return nicely formatted string, eg. foo(String,int)
	 */
	public static String formatMethodForMessage(String name, List<TypeDescriptor> argumentTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		for (int i = 0; i < argumentTypes.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			TypeDescriptor typeDescriptor = argumentTypes.get(i);
			if (typeDescriptor != null) {
				sb.append(formatClassNameForMessage(typeDescriptor.getType()));
			}
			else {
				sb.append(formatClassNameForMessage(null));
			}
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Produce a nice string for a given class object.
	 * For example, a string array will have the formatted name "java.lang.String[]".
	 * @param clazz The class whose name is to be formatted
	 * @return a formatted string suitable for message inclusion
	 */
	public static String formatClassNameForMessage(Class<?> clazz) {
		if (clazz == null) {
			return "null";
		}
		StringBuilder fmtd = new StringBuilder();
		if (clazz.isArray()) {
			int dims = 1;
			Class baseClass = clazz.getComponentType();
			while (baseClass.isArray()) {
				baseClass = baseClass.getComponentType();
				dims++;
			}
			fmtd.append(baseClass.getName());
			for (int i = 0; i < dims; i++) {
				fmtd.append("[]");
			}
		}
		else {
			fmtd.append(clazz.getName());
		}
		return fmtd.toString();
	}

}
