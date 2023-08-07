/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.hint;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link TypeReference} based on a {@link Class}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 6.0
 */
final class ReflectionTypeReference extends AbstractTypeReference {

	private final Class<?> type;

	private ReflectionTypeReference(Class<?> type) {
		super(type.getPackageName(), type.getSimpleName(), getEnclosingClass(type));
		this.type = type;
	}

	@Nullable
	private static TypeReference getEnclosingClass(Class<?> type) {
		Class<?> candidate = (type.isArray() ? type.componentType().getEnclosingClass() :
				type.getEnclosingClass());
		return (candidate != null ? new ReflectionTypeReference(candidate) : null);
	}

	static ReflectionTypeReference of(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		Assert.notNull(type.getCanonicalName(), "'type.getCanonicalName()' must not be null");
		return new ReflectionTypeReference(type);
	}

	@Override
	public String getCanonicalName() {
		return this.type.getCanonicalName();
	}

	@Override
	protected boolean isPrimitive() {
		return this.type.isPrimitive() ||
				(this.type.isArray() && this.type.componentType().isPrimitive());
	}

}
