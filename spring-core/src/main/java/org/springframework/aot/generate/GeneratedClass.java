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

package org.springframework.aot.generate;

import java.util.function.Consumer;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.javapoet.TypeSpec.Builder;

/**
 * A generated class is a container for generated methods.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedClasses
 */
public final class GeneratedClass {

	private final Consumer<Builder> typeSpecCustomizer;

	private final ClassName name;

	private final GeneratedMethods methods;


	/**
	 * Create a new {@link GeneratedClass} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedClasses}.
	 * @param name the generated name
	 */
	GeneratedClass(Consumer<Builder> typeSpecCustomizer, ClassName name) {
		this.typeSpecCustomizer = typeSpecCustomizer;
		this.name = name;
		this.methods = new GeneratedMethods(new MethodNameGenerator());
	}


	/**
	 * Return the name of the generated class.
	 * @return the name of the generated class
	 */
	public ClassName getName() {
		return this.name;
	}

	/**
	 * Return the method generator that can be used for this generated class.
	 * @return the method generator
	 */
	public MethodGenerator getMethodGenerator() {
		return this.methods;
	}

	JavaFile generateJavaFile() {
		TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(this.name);
		this.typeSpecCustomizer.accept(typeSpecBuilder);
		this.methods.doWithMethodSpecs(typeSpecBuilder::addMethod);
		return JavaFile.builder(this.name.packageName(), typeSpecBuilder.build())
				.build();
	}

}
