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

package org.springframework.beans.factory.generator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.util.ReflectionUtils;

/**
 * Support for generating {@link Field} access.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class BeanFieldGenerator {

	/**
	 * The {@link Options} to use to access a field.
	 */
	public static final Options FIELD_OPTIONS = Options.defaults()
			.useReflection(member -> Modifier.isPrivate(member.getModifiers())).build();


	/**
	 * Generate the necessary code to set the specified field. Use reflection
	 * using {@link ReflectionUtils} if necessary.
	 * @param field the field to set
	 * @param value a code representation of the field value
	 * @return the code to set the specified field
	 */
	public MultiStatement generateSetValue(String target, Field field, CodeBlock value) {
		MultiStatement statement = new MultiStatement();
		boolean useReflection = Modifier.isPrivate(field.getModifiers());
		if (useReflection) {
			String fieldName = String.format("%sField", field.getName());
			statement.addStatement("$T $L = $T.findField($T.class, $S)", Field.class, fieldName, ReflectionUtils.class,
					field.getDeclaringClass(), field.getName());
			statement.addStatement("$T.makeAccessible($L)", ReflectionUtils.class, fieldName);
			statement.addStatement("$T.setField($L, $L, $L)", ReflectionUtils.class, fieldName, target, value);
		}
		else {
			statement.addStatement("$L.$L = $L", target, field.getName(), value);
		}
		return statement;
	}

}
