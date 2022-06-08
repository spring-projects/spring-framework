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

import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A generated method.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedMethods
 * @see MethodGenerator
 */
public final class GeneratedMethod {

	private final String name;

	@Nullable
	private MethodSpec spec;


	/**
	 * Create a new {@link GeneratedMethod} instance with the given name. This
	 * constructor is package-private since names should only be generated via a
	 * {@link GeneratedMethods}.
	 * @param name the generated name
	 */
	GeneratedMethod(String name) {
		this.name = name;
	}


	/**
	 * Return the generated name of the method.
	 * @return the name of the generated method
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the {@link MethodSpec} for this generated method.
	 * @return the method spec
	 * @throws IllegalStateException if one of the {@code generateBy(...)}
	 * methods has not been called
	 */
	public MethodSpec getSpec() {
		Assert.state(this.spec != null,
				() -> String.format("Method '%s' has no method spec defined", this.name));
		return this.spec;
	}

	/**
	 * Generate the method using the given consumer.
	 * @param builder a consumer that will accept a method spec builder and
	 * configure it as necessary
	 * @return this instance
	 */
	public GeneratedMethod using(Consumer<MethodSpec.Builder> builder) {
		Builder builderToUse = MethodSpec.methodBuilder(this.name);
		builder.accept(builderToUse);
		MethodSpec spec = builderToUse.build();
		assertNameHasNotBeenChanged(spec);
		this.spec = spec;
		return this;
	}

	private void assertNameHasNotBeenChanged(MethodSpec spec) {
		Assert.isTrue(this.name.equals(spec.name), () -> String
				.format("'spec' must use the generated name \"%s\"", this.name));
	}

	@Override
	public String toString() {
		return (this.spec != null) ? this.spec.toString() : this.name.toString();
	}

}
