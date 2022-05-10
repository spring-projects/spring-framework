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

import org.springframework.aot.generate.ClassGenerator.JavaFileGenerator;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.util.Assert;

/**
 * A generated class.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedClasses
 * @see ClassGenerator
 */
public final class GeneratedClass {

	private final JavaFileGenerator JavaFileGenerator;

	private final ClassName name;

	private final GeneratedMethods methods;


	/**
	 * Create a new {@link GeneratedClass} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedClasses}.
	 * @param name the generated name
	 */
	GeneratedClass(JavaFileGenerator javaFileGenerator, ClassName name) {
		MethodNameGenerator methodNameGenerator = new MethodNameGenerator(
				javaFileGenerator.getReservedMethodNames());
		this.JavaFileGenerator = javaFileGenerator;
		this.name = name;
		this.methods = new GeneratedMethods(methodNameGenerator);
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
		JavaFile javaFile = this.JavaFileGenerator.generateJavaFile(this.name,
				this.methods);
		Assert.state(this.name.packageName().equals(javaFile.packageName),
				() -> "Generated JavaFile should be in package '"
						+ this.name.packageName() + "'");
		Assert.state(this.name.simpleName().equals(javaFile.typeSpec.name),
				() -> "Generated JavaFile should be named '" + this.name.simpleName()
						+ "'");
		return javaFile;
	}

}
