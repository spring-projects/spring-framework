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

import org.springframework.core.GenericCollectionTypeResolver;

class FieldDescriptor extends AbstractDescriptor {

	private final Field field;

	private final int nestingLevel;

	public FieldDescriptor(Field field) {
		this(field.getType(), field, 1, 0);
	}

	@Override
	public Annotation[] getAnnotations() {
		return TypeDescriptor.nullSafeAnnotations(field.getAnnotations());
	}
	
	@Override
	protected Class<?> getCollectionElementClass() {
		return GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.nestingLevel);
	}

	@Override
	protected Class<?> getMapKeyClass() {
		return GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.nestingLevel);
	}

	@Override
	protected Class<?> getMapValueClass() {
		return GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.nestingLevel);
	}

	@Override
	protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
		return new FieldDescriptor(type, this.field, this.nestingLevel + 1, typeIndex);
	}

	// internal
	
	private FieldDescriptor(Class<?> type, Field field, int nestingLevel, int typeIndex) {
		super(type);
		this.field = field;
		this.nestingLevel = nestingLevel;
	}

}