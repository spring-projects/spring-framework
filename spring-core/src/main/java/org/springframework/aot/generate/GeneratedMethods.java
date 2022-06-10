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
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.javapoet.MethodSpec;
import org.springframework.util.Assert;

/**
 * A managed collection of generated methods.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedMethod
 */
public class GeneratedMethods implements Iterable<GeneratedMethod>, MethodGenerator {

	private final MethodNameGenerator methodNameGenerator;

	private final List<GeneratedMethod> generatedMethods = new ArrayList<>();


	/**
	 * Create a new {@link GeneratedMethods} instance backed by a new
	 * {@link MethodNameGenerator}.
	 */
	public GeneratedMethods() {
		this(new MethodNameGenerator());
	}

	/**
	 * Create a new {@link GeneratedMethods} instance backed by the given
	 * {@link MethodNameGenerator}.
	 * @param methodNameGenerator the method name generator
	 */
	public GeneratedMethods(MethodNameGenerator methodNameGenerator) {
		Assert.notNull(methodNameGenerator, "'methodNameGenerator' must not be null");
		this.methodNameGenerator = methodNameGenerator;
	}


	@Override
	public GeneratedMethod generateMethod(Object... methodNameParts) {
		return add(methodNameParts);
	}

	/**
	 * Add a new {@link GeneratedMethod}. The returned instance must define the
	 * method spec by calling {@code using(builder -> ...)}.
	 * @param methodNameParts the method name parts that should be used to
	 * generate a unique method name
	 * @return the newly added {@link GeneratedMethod}
	 */
	public GeneratedMethod add(Object... methodNameParts) {
		GeneratedMethod method = new GeneratedMethod(
				this.methodNameGenerator.generateMethodName(methodNameParts));
		this.generatedMethods.add(method);
		return method;
	}

	/**
	 * Call the given action with each of the {@link MethodSpec MethodSpecs}
	 * that have been added to this collection.
	 * @param action the action to perform
	 */
	public void doWithMethodSpecs(Consumer<MethodSpec> action) {
		stream().map(GeneratedMethod::getSpec).forEach(action);
	}

	@Override
	public Iterator<GeneratedMethod> iterator() {
		return this.generatedMethods.iterator();
	}

	/**
	 * Return a {@link Stream} of all the methods in this collection.
	 * @return a stream of {@link GeneratedMethod} instances
	 */
	public Stream<GeneratedMethod> stream() {
		return this.generatedMethods.stream();
	}

}
