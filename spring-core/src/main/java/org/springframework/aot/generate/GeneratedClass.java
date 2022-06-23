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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.Assert;

/**
 * A single generated class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedClasses
 */
public final class GeneratedClass {

	private final ClassName name;

	private final GeneratedMethods methods;

	private final Consumer<TypeSpec.Builder> type;

	private final Map<MethodName, AtomicInteger> methodNameSequenceGenerator = new ConcurrentHashMap<>();


	/**
	 * Create a new {@link GeneratedClass} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedClasses}.
	 * @param name the generated name
	 * @param type a {@link Consumer} used to build the type
	 */
	GeneratedClass(ClassName name, Consumer<TypeSpec.Builder> type) {
		this.name = name;
		this.type = type;
		this.methods = new GeneratedMethods(this::generateSequencedMethodName);
	}


	/**
	 * Update this instance with a set of reserved method names that should not
	 * be used for generated methods. Reserved names are often needed when a
	 * generated class implements a specific interface.
	 * @param reservedMethodNames the reserved method names
	 */
	public void reserveMethodNames(String... reservedMethodNames) {
		for (String reservedMethodName : reservedMethodNames) {
			String generatedName = generateSequencedMethodName(MethodName.of(reservedMethodNames));
			Assert.state(generatedName.equals(reservedMethodName),
					() -> String.format("Unable to reserve method name '%s'", reservedMethodName));
		}
	}

	private String generateSequencedMethodName(MethodName name) {
		int sequence = this.methodNameSequenceGenerator
				.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name.toString() + sequence : name.toString();
	}

	/**
	 * Return the name of the generated class.
	 * @return the name of the generated class
	 */
	public ClassName getName() {
		return this.name;
	}

	/**
	 * Return generated methods for this instance.
	 * @return the generated methods
	 */
	public GeneratedMethods getMethods() {
		return this.methods;
	}

	JavaFile generateJavaFile() {
		TypeSpec.Builder type = getBuilder(this.type);
		this.methods.doWithMethodSpecs(type::addMethod);
		return JavaFile.builder(this.name.packageName(), type.build()).build();
	}

	private TypeSpec.Builder getBuilder(Consumer<TypeSpec.Builder> type) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(this.name);
		type.accept(builder);
		return builder;
	}

	void assertSameType(Consumer<TypeSpec.Builder> type) {
		Assert.state(type == this.type || getBuilder(this.type).build().equals(getBuilder(type).build()),
				"'type' consumer generated different result");
	}

}
