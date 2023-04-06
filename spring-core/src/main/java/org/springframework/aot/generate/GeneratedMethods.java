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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.util.Assert;

/**
 * A managed collection of generated methods.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedMethod
 */
public class GeneratedMethods {

	private final ClassName className;

	private final Function<MethodName, String> methodNameGenerator;

	private final MethodName prefix;

	private final List<GeneratedMethod> generatedMethods;

	/**
	 * Create a new {@link GeneratedMethods} using the specified method name
	 * generator.
	 * @param className the declaring class name
	 * @param methodNameGenerator the method name generator
	 */
	GeneratedMethods(ClassName className, Function<MethodName, String> methodNameGenerator) {
		Assert.notNull(className, "'className' must not be null");
		Assert.notNull(methodNameGenerator, "'methodNameGenerator' must not be null");
		this.className = className;
		this.methodNameGenerator = methodNameGenerator;
		this.prefix = MethodName.NONE;
		this.generatedMethods = new ArrayList<>();
	}

	private GeneratedMethods(ClassName className, Function<MethodName, String> methodNameGenerator,
			MethodName prefix, List<GeneratedMethod> generatedMethods) {

		this.className = className;
		this.methodNameGenerator = methodNameGenerator;
		this.prefix = prefix;
		this.generatedMethods = generatedMethods;
	}

	/**
	 * Add a new {@link GeneratedMethod}.
	 * <p>The {@code suggestedName} should provide the unqualified form of what
	 * the method does. For instance, if the method returns an instance of a
	 * given type, {@code getInstance} can be used as it is automatically
	 * qualified using {@linkplain #withPrefix(String) the current prefix}.
	 * <p>The prefix is applied a little differently for suggested names that
	 * start with {@code get}, {@code set}, or {@code is}. Taking the previous
	 * example with a {@code myBean} prefix, the actual method name is
	 * {@code getMyBeanInstance}. Further processing of the method can happen
	 * to ensure uniqueness within a class.
	 * @param suggestedName the suggested name for the method
	 * @param method a {@link Consumer} used to build method
	 * @return the newly added {@link GeneratedMethod}
	 */
	public GeneratedMethod add(String suggestedName, Consumer<Builder> method) {
		Assert.notNull(suggestedName, "'suggestedName' must not be null");
		return add(new String[] { suggestedName }, method);
	}

	/**
	 * Add a new {@link GeneratedMethod}.
	 * <p>The {@code suggestedNameParts} should provide the unqualified form of
	 * what the method does. For instance, if the method returns an instance of
	 * a given type, {@code ["get", "instance"]} can be used as it is
	 * automatically qualified using {@linkplain #withPrefix(String) the current
	 * prefix}.
	 * <p>The prefix is applied a little differently for suggested name parts
	 * that start with {@code get}, {@code set}, or {@code is}. Taking the
	 * previous example with a {@code myBean} prefix, the actual method name is
	 * {@code getMyBeanInstance}. Further processing of the method can happen
	 * to ensure uniqueness within a class.
	 * @param suggestedNameParts the suggested name parts for the method
	 * @param method a {@link Consumer} used to build method
	 * @return the newly added {@link GeneratedMethod}
	 */
	public GeneratedMethod add(String[] suggestedNameParts, Consumer<Builder> method) {
		Assert.notNull(suggestedNameParts, "'suggestedNameParts' must not be null");
		Assert.notNull(method, "'method' must not be null");
		String generatedName = this.methodNameGenerator.apply(this.prefix.and(suggestedNameParts));
		GeneratedMethod generatedMethod = new GeneratedMethod(this.className, generatedName, method);
		this.generatedMethods.add(generatedMethod);
		return generatedMethod;
	}

	/**
	 * Specify the prefix to use for method names. The prefix applies to
	 * suggested method names, with special handling of {@code get}, {@code set},
	 * and {@code is} prefixes in the suggested name itself.
	 * @param prefix the prefix to add to suggested method names
	 * @return a new instance with the specified prefix
	 */
	public GeneratedMethods withPrefix(String prefix) {
		Assert.notNull(prefix, "'prefix' must not be null");
		return new GeneratedMethods(this.className, this.methodNameGenerator,
				this.prefix.and(prefix), this.generatedMethods);
	}

	/**
	 * Call the given action with each of the {@link MethodSpec MethodSpecs}
	 * that have been added to this collection.
	 * @param action the action to perform
	 */
	void doWithMethodSpecs(Consumer<MethodSpec> action) {
		stream().map(GeneratedMethod::getMethodSpec).forEach(action);
	}

	Stream<GeneratedMethod> stream() {
		return this.generatedMethods.stream();
	}

}
