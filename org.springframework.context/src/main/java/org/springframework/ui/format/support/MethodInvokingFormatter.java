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
package org.springframework.ui.format.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.springframework.ui.format.Formatter;
import org.springframework.util.ReflectionUtils;

/**
 * A generic Formatter that reflectively invokes methods on a class to carry out format and parse operations.
 * The format method should be a public member method that returns a String and either accepts no arguments or a single Locale argument.
 * The parse method should be a declared public static method that returns the formattedObjectType and either accepts a single String argument or String and Locale arguments.
 * Throws an {@link IllegalArgumentException} if either the format method or parse method could not be resolved.
 * Useful for invoking existing Formatter logic on a formattable type without needing a dedicated Formatter class.
 *  
 * @author Keith Donald
 */
public class MethodInvokingFormatter implements Formatter {

	private Class<?> formattedObjectType;

	private Method formatMethod;

	private boolean formatMethodLocaleArgumentPresent;

	private Method parseMethod;

	private boolean parseMethodLocaleArgumentPresent;

	/**
	 * Creates a new reflective method invoking formatter.
	 * @param formattedObjectType the object type that contains format and parse methods
	 * @param formatMethodName the format method name e.g. "toString"
	 * @param parseMethodName the parse method name e.g. "valueOf"
	 */
	public MethodInvokingFormatter(Class<?> formattedObjectType, String formatMethodName, String parseMethodName) {
		this.formattedObjectType = formattedObjectType;
		resolveFormatMethod(formatMethodName);
		resolveParseMethod(parseMethodName);
	}

	public String format(Object object, Locale locale) {
		if (this.formatMethodLocaleArgumentPresent) {
			return (String) ReflectionUtils.invokeMethod(this.formatMethod, object, locale);
		} else {
			return (String) ReflectionUtils.invokeMethod(this.formatMethod, object);
		}
	}

	public Object parse(String formatted, Locale locale) {
		if (this.parseMethodLocaleArgumentPresent) {
			return ReflectionUtils.invokeMethod(this.parseMethod, null, formatted, locale);
		} else {
			return ReflectionUtils.invokeMethod(this.parseMethod, null, formatted);
		}
	}

	private void resolveFormatMethod(String methodName) {
		Method[] methods = this.formattedObjectType.getMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName) && method.getReturnType().equals(String.class)) {
				if (method.getParameterTypes().length == 0) {
					this.formatMethod = method;
				} else if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(Locale.class)) {
					this.formatMethod = method;
					this.formatMethodLocaleArgumentPresent = true;
				}
			}
		}
		if (this.formatMethod == null) {
			throw new IllegalArgumentException("Unable to resolve format method '" + methodName + "' on class ["
					+ this.formattedObjectType.getName()
					+ "] method should have signature [public String <methodName>()] "
					+ "or [public String <methodName>(Locale)]");
		}
	}

	private void resolveParseMethod(String methodName) {
		Method[] methods = this.formattedObjectType.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName) && method.getReturnType().equals(this.formattedObjectType)
					&& Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
				if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class)) {
					this.parseMethod = method;
				} else if (method.getParameterTypes().length == 2 && method.getParameterTypes()[0].equals(String.class)
						&& method.getParameterTypes()[1].equals(Locale.class)) {
					this.parseMethod = method;
					this.parseMethodLocaleArgumentPresent = true;
				}
			}
		}
		if (this.parseMethod == null) {
			throw new IllegalArgumentException("Unable to resolve parse method '" + methodName + "' on class ["
					+ this.formattedObjectType.getName()
					+ "]; method should have signature [public static T <methodName>(String)] "
					+ "or [public static T <methodName>(String, Locale)]");
		}
	}
}
