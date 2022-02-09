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

package org.springframework.aot.hint;

import org.springframework.lang.Nullable;

/**
 * A {@link TypeReference} based on a {@link Class}.
 *
 * @author Stephane Nicoll
 */
final class ReflectionTypeReference extends AbstractTypeReference {

	private final Class<?> type;

	@Nullable
	private final TypeReference enclosing;


	private ReflectionTypeReference(Class<?> type) {
		this.type = type;
		this.enclosing = (type.getEnclosingClass() != null
				? TypeReference.of(type.getEnclosingClass()) : null);
	}

	static ReflectionTypeReference of(Class<?> type) {
		return new ReflectionTypeReference(type);
	}

	@Override
	public String getCanonicalName() {
		return this.type.getCanonicalName();
	}

	@Override
	public String getPackageName() {
		return this.type.getPackageName();
	}

	@Override
	public String getSimpleName() {
		return this.type.getSimpleName();
	}

	@Override
	public TypeReference getEnclosingType() {
		return this.enclosing;
	}

}
