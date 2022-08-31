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

import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.util.Assert;

/**
 * A managed collection of generated methods.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedMethod
 */
public class GeneratedMethods {

	private final Function<MethodName, String> methodNameGenerator;

	private final MethodName prefix;

	private final List<GeneratedMethod> generatedMethods;

	/**
	 * Create a new {@link GeneratedMethods} using the specified method name
	 * generator.
	 * @param methodNameGenerator the method name generator
	 */
	GeneratedMethods(Function<MethodName, String> methodNameGenerator) {
		Assert.notNull(methodNameGenerator, "'methodNameGenerator' must not be null");
		this.methodNameGenerator = methodNameGenerator;
		this.prefix = MethodName.NONE;
		this.generatedMethods = new ArrayList<>();
	}

	private GeneratedMethods(Function<MethodName, String> methodNameGenerator,
			MethodName prefix, List<GeneratedMethod> generatedMethods) {

		this.methodNameGenerator = methodNameGenerator;
		this.prefix = prefix;
		this.generatedMethods = generatedMethods;
	}

	/**
	 * Add a new {@link GeneratedMethod}.
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
	 * @param suggestedNameParts the suggested name parts for the method
	 * @param method a {@link Consumer} used to build method
	 * @return the newly added {@link GeneratedMethod}
	 */
	public GeneratedMethod add(String[] suggestedNameParts, Consumer<Builder> method) {
		Assert.notNull(suggestedNameParts, "'suggestedNameParts' must not be null");
		Assert.notNull(method, "'method' must not be null");
		String generatedName = this.methodNameGenerator.apply(this.prefix.and(suggestedNameParts));
		GeneratedMethod generatedMethod = new GeneratedMethod(generatedName, method);
		this.generatedMethods.add(generatedMethod);
		return generatedMethod;
	}


	public GeneratedMethods withPrefix(String prefix) {
		Assert.notNull(prefix, "'prefix' must not be null");
		return new GeneratedMethods(this.methodNameGenerator, this.prefix.and(prefix), this.generatedMethods);
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
