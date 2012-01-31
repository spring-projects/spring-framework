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

/**
 * @author Keith Donald
 * @since 3.1
 */
class ClassDescriptor extends AbstractDescriptor {

	ClassDescriptor(Class<?> type) {
		super(type);
	}

	@Override
	public Annotation[] getAnnotations() {
		return TypeDescriptor.EMPTY_ANNOTATION_ARRAY;
	}

	@Override
	protected Class<?> resolveCollectionElementType() {
		return null;
	}

	@Override
	protected Class<?> resolveMapKeyType() {
		return null;
	}

	@Override
	protected Class<?> resolveMapValueType() {
		return null;
	}

	@Override
	protected AbstractDescriptor nested(Class<?> type, int typeIndex) {
		return new ClassDescriptor(type);
	}
	
}
