/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.GenericCollectionTypeResolver;

/**
 * @author Keith Donald
 * @since 3.1
 */
class FieldDescriptor extends AbstractDescriptor {

	private final Field field;

	private final int nestingLevel;

	private Map<Integer, Integer> typeIndexesPerLevel;


	public FieldDescriptor(Field field) {
		super(field.getType());
		this.field = field;
		this.nestingLevel = 1;
	}

	private FieldDescriptor(Class<?> type, Field field, int nestingLevel, int typeIndex, Map<Integer, Integer> typeIndexesPerLevel) {
		super(type);
		this.field = field;
		this.nestingLevel = nestingLevel;
		this.typeIndexesPerLevel = typeIndexesPerLevel;
		this.typeIndexesPerLevel.put(nestingLevel, typeIndex);
	}


	@Override
	public Annotation[] getAnnotations() {
		return TypeDescriptor.nullSafeAnnotations(this.field.getAnnotations());
	}

	@Override
	protected Class<?> resolveCollectionElementType() {
		return GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.nestingLevel, this.typeIndexesPerLevel);
	}

	@Override
	protected Class<?> resolveMapKeyType() {
		return GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.nestingLevel, this.typeIndexesPerLevel);
	}

	@Override
	protected Class<?> resolveMapValueType() {
		return GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.nestingLevel, this.typeIndexesPerLevel);
	}

	@Override
	protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
		if (this.typeIndexesPerLevel == null) {
			this.typeIndexesPerLevel = new HashMap<Integer, Integer>(4);
		}
		return new FieldDescriptor(type, this.field, this.nestingLevel + 1, typeIndex, this.typeIndexesPerLevel);
	}

}
