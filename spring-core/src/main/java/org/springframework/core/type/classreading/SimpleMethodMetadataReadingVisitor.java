/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.type.classreading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.ParameterMetadata;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;

/**
 * ASM method visitor that creates {@link SimpleMethodMetadata}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2
 */
final class SimpleMethodMetadataReadingVisitor extends MethodVisitor {

	@Nullable
	private final ClassLoader classLoader;

	private final Object source;

	private final List<MergedAnnotation<?>> annotations = new ArrayList<>(4);

	private final Map<Integer, List<MergedAnnotation<?>>> parameterAnnotations = new HashMap<>();

	private final String declaringClassName;

	private final int delcaringClassAccess;

	private final int access;

	private final String methodName;

	private final List<TypeMetadata> parameterTypes;

	private final TypeMetadata returnType;

	private final Object value;

	private final Consumer<SimpleMethodMetadata> methodConsumer;

	private final Consumer<SimpleConstructorMetadata> constructorConsumer;

	SimpleMethodMetadataReadingVisitor(@Nullable ClassLoader classLoader, SimpleSource source,
			String declaringClassName, int declaringClassAccess, int access, String methodName,
			List<TypeMetadata> parameterTypes, TypeMetadata returnType, Object value,
			Consumer<SimpleMethodMetadata> methodConsumer,
			Consumer<SimpleConstructorMetadata> constructorConsumer) {

		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.source = source;
		this.declaringClassName = declaringClassName;
		this.delcaringClassAccess = declaringClassAccess;
		this.access = access;
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
		this.returnType = returnType;
		this.value = value;
		this.methodConsumer = methodConsumer;
		this.constructorConsumer = constructorConsumer;
	}

	@Override
	@Nullable
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return MergedAnnotationReadingVisitor.get(this.classLoader, this.source, descriptor, visible, this.annotations::add);
	}

	@Override
	@Nullable
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		List<MergedAnnotation<?>> parameterAnnotations = this.parameterAnnotations.computeIfAbsent(parameter,
				k -> new ArrayList<>());
		return MergedAnnotationReadingVisitor.get(this.classLoader, this.source, descriptor, visible, parameterAnnotations::add);
	}

	@Override
	public void visitEnd() {
		String declaringClassName = this.declaringClassName;
		String methodName = this.methodName;
		int access = this.access;
		TypeMetadata returnType = this.returnType;

		List<ParameterMetadata> methodParameters = new ArrayList<>(this.parameterTypes.size());
		for (int i = 0; i < this.parameterTypes.size(); i++) {
			TypeMetadata parameterType = this.parameterTypes.get(i);
			MergedAnnotations annotations = MergedAnnotations
					.of(this.parameterAnnotations.getOrDefault(i, Collections.emptyList()));
			ParameterMetadata metadata = new SimpleParameterMetadata(declaringClassName, methodName, parameterType, i,
					this.source, annotations);
			methodParameters.add(metadata);
		}

		MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
		if (methodName.equals("<init>")) {
			SimpleConstructorMetadata metadata = new SimpleConstructorMetadata(access, declaringClassName, this.source,
					annotations, methodParameters);
			this.constructorConsumer.accept(metadata);
		}
		else {
			SimpleMethodMetadata metadata = new SimpleMethodMetadata(declaringClassName,
					this.delcaringClassAccess, methodName, access, returnType, this.source, annotations, methodParameters);
			this.methodConsumer.accept(metadata);
		}
	}

}
