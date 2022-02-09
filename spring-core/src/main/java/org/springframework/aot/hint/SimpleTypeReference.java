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
import org.springframework.util.Assert;

/**
 * A {@link TypeReference} based on fully qualified name.
 *
 * @author Stephane Nicoll
 */
final class SimpleTypeReference extends AbstractTypeReference {

	private String canonicalName;

	private final String packageName;

	private final String simpleName;

	@Nullable
	private final TypeReference enclosingType;


	SimpleTypeReference(String packageName, String simpleName, @Nullable TypeReference enclosingType) {
		this.packageName = packageName;
		this.simpleName = simpleName;
		this.enclosingType = enclosingType;
	}

	static SimpleTypeReference of(String className) {
		Assert.notNull(className, "ClassName must not be null");
		if (!className.contains("$")) {
			return createTypeReference(className);
		}
		String[] elements = className.split("\\$");
		SimpleTypeReference typeReference = createTypeReference(elements[0]);
		for (int i = 1; i < elements.length; i++) {
			typeReference = new SimpleTypeReference(typeReference.getPackageName(), elements[i], typeReference);
		}
		return typeReference;
	}

	private static SimpleTypeReference createTypeReference(String className) {
		int i = className.lastIndexOf('.');
		return new SimpleTypeReference(className.substring(0, i), className.substring(i + 1), null);
	}

	@Override
	public String getCanonicalName() {
		if (this.canonicalName == null) {
			StringBuilder names = new StringBuilder();
			buildName(this, names);
			this.canonicalName = this.packageName + "." + names;
		}
		return this.canonicalName;
	}

	private static void buildName(TypeReference type, StringBuilder sb) {
		if (type == null) {
			return;
		}
		String typeName = (type.getEnclosingType() != null) ? "." + type.getSimpleName() : type.getSimpleName();
		sb.insert(0, typeName);
		buildName(type.getEnclosingType(), sb);
	}

	@Override
	public String getPackageName() {
		return this.packageName;
	}

	@Override
	public String getSimpleName() {
		return this.simpleName;
	}

	@Override
	public TypeReference getEnclosingType() {
		return this.enclosingType;
	}

}
