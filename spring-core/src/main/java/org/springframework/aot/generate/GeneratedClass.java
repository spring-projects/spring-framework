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
import org.springframework.lang.Nullable;
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

	@Nullable
	private final GeneratedClass enclosingClass;

	private final ClassName name;

	private final GeneratedMethods methods;

	private final Consumer<TypeSpec.Builder> type;

	private final Map<ClassName, GeneratedClass> declaredClasses;

	private final Map<MethodName, AtomicInteger> methodNameSequenceGenerator;


	/**
	 * Create a new {@link GeneratedClass} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedClasses}.
	 * @param name the generated name
	 * @param type a {@link Consumer} used to build the type
	 */
	GeneratedClass(ClassName name, Consumer<TypeSpec.Builder> type) {
		this(null, name, type);
	}

	private GeneratedClass(@Nullable GeneratedClass enclosingClass, ClassName name,
			Consumer<TypeSpec.Builder> type) {
		this.enclosingClass = enclosingClass;
		this.name = name;
		this.type = type;
		this.methods = new GeneratedMethods(name, this::generateSequencedMethodName);
		this.declaredClasses = new ConcurrentHashMap<>();
		this.methodNameSequenceGenerator = new ConcurrentHashMap<>();
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
	 * Return the enclosing {@link GeneratedClass} or {@code null} if this
	 * instance represents a top-level class.
	 * @return the enclosing generated class, if any
	 */
	@Nullable
	public GeneratedClass getEnclosingClass() {
		return this.enclosingClass;
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

	/**
	 * Get or add a nested generated class with the specified name. If this method
	 * has previously been called with the given {@code name}, the existing class
	 * will be returned, otherwise a new class will be generated.
	 * @param name the name of the nested class
	 * @param type a {@link Consumer} used to build the type
	 * @return an existing or newly generated class whose enclosing class is this class
	 */
	public GeneratedClass getOrAdd(String name, Consumer<TypeSpec.Builder> type) {
		ClassName className = this.name.nestedClass(name);
		return this.declaredClasses.computeIfAbsent(className,
				key -> new GeneratedClass(this, className, type));
	}

	JavaFile generateJavaFile() {
		Assert.state(getEnclosingClass() == null,
				"Java file cannot be generated for an inner class");
		TypeSpec.Builder type = apply();
		return JavaFile.builder(this.name.packageName(), type.build()).build();
	}

	private TypeSpec.Builder apply() {
		TypeSpec.Builder type = getBuilder(this.type);
		this.methods.doWithMethodSpecs(type::addMethod);
		this.declaredClasses.values().forEach(declaredClass ->
				type.addType(declaredClass.apply().build()));
		return type;
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
