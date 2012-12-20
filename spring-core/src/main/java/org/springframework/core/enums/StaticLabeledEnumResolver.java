/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.enums;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.util.Assert;

/**
 * {@link LabeledEnumResolver} that resolves statically defined enumerations.
 * Static implies all enum instances were defined within Java code,
 * implementing the type-safe enum pattern.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
@SuppressWarnings({"rawtypes","unchecked"})
public class StaticLabeledEnumResolver extends AbstractCachingLabeledEnumResolver {

	/**
	 * Shared <code>StaticLabeledEnumResolver</code> singleton instance.
	 */
	private static final StaticLabeledEnumResolver INSTANCE = new StaticLabeledEnumResolver();


	/**
	 * Return the shared <code>StaticLabeledEnumResolver</code> singleton instance.
	 * Mainly for resolving unique StaticLabeledEnum references on deserialization.
	 * @see StaticLabeledEnum
	 */
	public static StaticLabeledEnumResolver instance() {
		return INSTANCE;
	}


	@Override
	protected Set<LabeledEnum> findLabeledEnums(Class type) {
		Set<LabeledEnum> typeEnums = new TreeSet<LabeledEnum>();
		for (Field field : type.getFields()) {
			if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
				if (type.isAssignableFrom(field.getType())) {
					try {
						Object value = field.get(null);
						Assert.isTrue(value instanceof LabeledEnum, "Field value must be a LabeledEnum instance");
						typeEnums.add((LabeledEnum) value);
					}
					catch (IllegalAccessException ex) {
						logger.warn("Unable to access field value: " + field, ex);
					}
				}
			}
		}
		return typeEnums;
	}

}
