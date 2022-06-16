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

package org.springframework.aot.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

/**
 * Wrapper for a generated {@linkplain TypeSpec type}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class GeneratedType {

	private final ClassName className;

	private final TypeSpec.Builder type;

	private final List<MethodSpec> methods;

	GeneratedType(ClassName className, Consumer<TypeSpec.Builder> type) {
		this.className = className;
		this.type = TypeSpec.classBuilder(className);
		type.accept(this.type);
		this.methods = new ArrayList<>();
	}

	/**
	 * Create an instance for the specified {@link ClassName}, customizing the type with
	 * the specified {@link Consumer consumer callback}.
	 * @param className the class name
	 * @param type a callback to customize the type, i.e. to change default modifiers
	 * @return a new {@link GeneratedType}
	 */
	public static GeneratedType of(ClassName className, Consumer<TypeSpec.Builder> type) {
		return new GeneratedType(className, type);
	}

	/**
	 * Create an instance for the specified {@link  ClassName}, as a {@code public} type.
	 * @param className the class name
	 * @return a new {@link GeneratedType}
	 */
	public static GeneratedType of(ClassName className) {
		return of(className, type -> type.addModifiers(Modifier.PUBLIC));
	}

	/**
	 * Return the {@link ClassName} of this instance.
	 * @return the class name
	 */
	public ClassName getClassName() {
		return this.className;
	}

	/**
	 * Customize the type of this instance.
	 * @param type the consumer of the type builder
	 * @return this for method chaining
	 */
	public GeneratedType customizeType(Consumer<TypeSpec.Builder> type) {
		type.accept(this.type);
		return this;
	}

	/**
	 * Add a method using the state of the specified {@link MethodSpec.Builder},
	 * updating the name of the method if a similar method already exists.
	 * @param method a method builder representing the method to add
	 * @return the added method
	 */
	public MethodSpec addMethod(MethodSpec.Builder method) {
		MethodSpec methodToAdd = createUniqueNameIfNecessary(method.build());
		this.methods.add(methodToAdd);
		return methodToAdd;
	}

	/**
	 * Return a {@link JavaFile} with the state of this instance.
	 * @return a java file
	 */
	public JavaFile toJavaFile() {
		return JavaFile.builder(this.className.packageName(),
				this.type.addMethods(this.methods).build()).indent("\t").build();
	}

	private MethodSpec createUniqueNameIfNecessary(MethodSpec method) {
		List<MethodSpec> candidates = this.methods.stream().filter(isSimilar(method)).toList();
		if (candidates.isEmpty()) {
			return method;
		}
		MethodSpec updatedMethod = method.toBuilder().setName(method.name + "_").build();
		return createUniqueNameIfNecessary(updatedMethod);
	}

	private Predicate<MethodSpec> isSimilar(MethodSpec method) {
		return candidate -> method.name.equals(candidate.name)
				&& method.parameters.size() == candidate.parameters.size();
	}

}
