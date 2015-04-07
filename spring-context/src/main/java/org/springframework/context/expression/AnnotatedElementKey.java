/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.expression;

import java.lang.reflect.AnnotatedElement;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Represent an {@link AnnotatedElement} on a particular {@link Class}
 * and is suitable as a key.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 4.2.0
 * @see CachedExpressionEvaluator
 */
public final class AnnotatedElementKey {

	private final AnnotatedElement element;

	private final Class<?> targetClass;

	/**
	 * Create a new instance with the specified {@link AnnotatedElement} and
	 * optional target {@link Class}.
	 */
	public AnnotatedElementKey(AnnotatedElement element, Class<?> targetClass) {
		Assert.notNull(element, "AnnotatedElement must be set.");
		this.element = element;
		this.targetClass = targetClass;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotatedElementKey)) {
			return false;
		}
		AnnotatedElementKey otherKey = (AnnotatedElementKey) other;
		return (this.element.equals(otherKey.element) &&
				ObjectUtils.nullSafeEquals(this.targetClass, otherKey.targetClass));
	}

	@Override
	public int hashCode() {
		return this.element.hashCode() + (this.targetClass != null ? this.targetClass.hashCode() * 29 : 0);
	}

}
