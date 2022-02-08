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

package org.springframework.context.annotation;

import java.util.Optional;

import org.springframework.core.type.MethodMetadata;

public class MethodMetadataWrapper {

	private final MethodMetadata methodMetadata;

	public MethodMetadataWrapper(MethodMetadata methodMetadata) {
		this.methodMetadata = methodMetadata;
	}

	public String getBeanName() {
		return this.methodMetadata.getMethodName();
	}

	public Optional<Class<?>> getClassForDeclaredBean() {
		try {
			Class<?> clazz = Class.forName(this.methodMetadata.getDeclaringClassName());
			return Optional.of(clazz);
		}
		catch (ClassNotFoundException ex) {
			return Optional.empty();
		}
	}

	public Optional<? extends Class<?>> getSuperClassForDeclaredBean() {
		return getClassForDeclaredBean()
				.map(Class::getSuperclass)
				.filter(superClass -> !"java.lang.Object".equals(superClass.getName()));
	}

}
