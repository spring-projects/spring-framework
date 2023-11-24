/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;

/**
 * A simple {@link ReflectiveProcessor} implementation that registers only a
 * reflection hint for the annotated type. Can be sub-classed to customize
 * processing for a given {@link AnnotatedElement} type.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class SimpleReflectiveProcessor implements ReflectiveProcessor {

	@Override
	public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
		if (element instanceof Class<?> type) {
			registerTypeHint(hints, type);
		}
		else if (element instanceof Constructor<?> constructor) {
			registerConstructorHint(hints, constructor);
		}
		else if (element instanceof Field field) {
			registerFieldHint(hints, field);
		}
		else if (element instanceof Method method) {
			registerMethodHint(hints, method);
		}
	}

	/**
	 * Register {@link ReflectionHints} against the specified {@link Class}.
	 * @param hints the reflection hints instance to use
	 * @param type the class to process
	 */
	protected void registerTypeHint(ReflectionHints hints, Class<?> type) {
		hints.registerType(type);
	}

	/**
	 * Register {@link ReflectionHints} against the specified {@link Constructor}.
	 * @param hints the reflection hints instance to use
	 * @param constructor the constructor to process
	 */
	protected void registerConstructorHint(ReflectionHints hints, Constructor<?> constructor) {
		hints.registerConstructor(constructor, ExecutableMode.INVOKE);
	}

	/**
	 * Register {@link ReflectionHints} against the specified {@link Field}.
	 * @param hints the reflection hints instance to use
	 * @param field the field to process
	 */
	protected void registerFieldHint(ReflectionHints hints, Field field) {
		hints.registerField(field);
	}

	/**
	 * Register {@link ReflectionHints} against the specified {@link Method}.
	 * @param hints the reflection hints instance to use
	 * @param method the method to process
	 */
	protected void registerMethodHint(ReflectionHints hints, Method method) {
		hints.registerMethod(method, ExecutableMode.INVOKE);
	}

}
