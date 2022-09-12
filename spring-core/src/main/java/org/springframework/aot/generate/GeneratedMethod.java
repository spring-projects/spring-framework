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
import org.springframework.javapoet.MethodSpec;
import org.springframework.util.Assert;

/**
 * A generated method.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedMethods
 */
public final class GeneratedMethod {

	private final ClassName className;

	private final String name;

	private final MethodSpec methodSpec;


	/**
	 * Create a new {@link GeneratedMethod} instance with the given name. This
	 * constructor is package-private since names should only be generated via
	 * {@link GeneratedMethods}.
	 * @param className the declaring class of the method
	 * @param name the generated method name
	 * @param method consumer to generate the method
	 */
	GeneratedMethod(ClassName className, String name, Consumer<MethodSpec.Builder> method) {
		this.className = className;
		this.name = name;
		MethodSpec.Builder builder = MethodSpec.methodBuilder(this.name);
		method.accept(builder);
		this.methodSpec = builder.build();
		Assert.state(this.name.equals(this.methodSpec.name),
				"'method' consumer must not change the generated method name");
	}


	/**
	 * Return the generated name of the method.
	 * @return the name of the generated method
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return a {@link MethodReference} to this generated method.
	 * @return a method reference
	 */
	public MethodReference toMethodReference() {
		return new DefaultMethodReference(this.methodSpec, this.className);
	}

	/**
	 * Return the {@link MethodSpec} for this generated method.
	 * @return the method spec
	 * @throws IllegalStateException if one of the {@code generateBy(...)}
	 * methods has not been called
	 */
	MethodSpec getMethodSpec() {
		return this.methodSpec;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
