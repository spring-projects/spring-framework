/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		Assert.state(this.name.equals(this.methodSpec.name()),
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
