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

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A reference to a static or instance method.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public final class MethodReference {

	private final Kind kind;

	@Nullable
	private final ClassName declaringClass;

	private final String methodName;


	private MethodReference(Kind kind, @Nullable ClassName declaringClass,
			String methodName) {
		this.kind = kind;
		this.declaringClass = declaringClass;
		this.methodName = methodName;
	}


	/**
	 * Create a new method reference that refers to the given instance method.
	 * @param methodName the method name
	 * @return a new {@link MethodReference} instance
	 */
	public static MethodReference of(String methodName) {
		Assert.hasLength(methodName, "'methodName' must not be empty");
		return new MethodReference(Kind.INSTANCE, null, methodName);
	}

	/**
	 * Create a new method reference that refers to the given instance method.
	 * @param declaringClass the declaring class
	 * @param methodName the method name
	 * @return a new {@link MethodReference} instance
	 */
	public static MethodReference of(Class<?> declaringClass, String methodName) {
		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.hasLength(methodName, "'methodName' must not be empty");
		return new MethodReference(Kind.INSTANCE, ClassName.get(declaringClass),
				methodName);
	}

	/**
	 * Create a new method reference that refers to the given instance method.
	 * @param declaringClass the declaring class
	 * @param methodName the method name
	 * @return a new {@link MethodReference} instance
	 */
	public static MethodReference of(ClassName declaringClass, String methodName) {
		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.hasLength(methodName, "'methodName' must not be empty");
		return new MethodReference(Kind.INSTANCE, declaringClass, methodName);
	}

	/**
	 * Create a new method reference that refers to the given static method.
	 * @param declaringClass the declaring class
	 * @param methodName the method name
	 * @return a new {@link MethodReference} instance
	 */
	public static MethodReference ofStatic(Class<?> declaringClass, String methodName) {
		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.hasLength(methodName, "'methodName' must not be empty");
		return new MethodReference(Kind.STATIC, ClassName.get(declaringClass),
				methodName);
	}

	/**
	 * Create a new method reference that refers to the given static method.
	 * @param declaringClass the declaring class
	 * @param methodName the method name
	 * @return a new {@link MethodReference} instance
	 */
	public static MethodReference ofStatic(ClassName declaringClass, String methodName) {
		Assert.notNull(declaringClass, "'declaringClass' must not be null");
		Assert.hasLength(methodName, "'methodName' must not be empty");
		return new MethodReference(Kind.STATIC, declaringClass, methodName);
	}


	/**
	 * Return the referenced declaring class.
	 * @return the declaring class
	 */
	@Nullable
	public ClassName getDeclaringClass() {
		return this.declaringClass;
	}

	/**
	 * Return the referenced method name.
	 * @return the method name
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * Return this method reference as a {@link CodeBlock}. If the reference is
	 * to an instance method then {@code this::<method name>} will be returned.
	 * @return a code block for the method reference.
	 * @see #toCodeBlock(String)
	 */
	public CodeBlock toCodeBlock() {
		return toCodeBlock(null);
	}

	/**
	 * Return this method reference as a {@link CodeBlock}. If the reference is
	 * to an instance method and {@code instanceVariable} is {@code null} then
	 * {@code this::<method name>} will be returned. No {@code instanceVariable}
	 * can be specified for static method references.
	 * @param instanceVariable the instance variable or {@code null}
	 * @return a code block for the method reference.
	 * @see #toCodeBlock(String)
	 */
	public CodeBlock toCodeBlock(@Nullable String instanceVariable) {
		return switch (this.kind) {
			case INSTANCE -> toCodeBlockForInstance(instanceVariable);
			case STATIC -> toCodeBlockForStatic(instanceVariable);
		};
	}

	private CodeBlock toCodeBlockForInstance(@Nullable String instanceVariable) {
		instanceVariable = (instanceVariable != null) ? instanceVariable : "this";
		return CodeBlock.of("$L::$L", instanceVariable, this.methodName);
	}

	private CodeBlock toCodeBlockForStatic(@Nullable String instanceVariable) {
		Assert.isTrue(instanceVariable == null,
				"'instanceVariable' must be null for static method references");
		return CodeBlock.of("$T::$L", this.declaringClass, this.methodName);
	}

	/**
	 * Return this method reference as an invocation {@link CodeBlock}.
	 * @param arguments the method arguments
	 * @return a code back to invoke the method
	 */
	public CodeBlock toInvokeCodeBlock(CodeBlock... arguments) {
		return toInvokeCodeBlock(null, arguments);
	}

	/**
	 * Return this method reference as an invocation {@link CodeBlock}.
	 * @param instanceVariable the instance variable or {@code null}
	 * @param arguments the method arguments
	 * @return a code back to invoke the method
	 */
	public CodeBlock toInvokeCodeBlock(@Nullable String instanceVariable,
			CodeBlock... arguments) {

		return switch (this.kind) {
			case INSTANCE -> toInvokeCodeBlockForInstance(instanceVariable, arguments);
			case STATIC -> toInvokeCodeBlockForStatic(instanceVariable, arguments);
		};
	}

	private CodeBlock toInvokeCodeBlockForInstance(@Nullable String instanceVariable,
			CodeBlock[] arguments) {

		CodeBlock.Builder code = CodeBlock.builder();
		if (instanceVariable != null) {
			code.add("$L.", instanceVariable);
		}
		else if (this.declaringClass != null) {
			code.add("new $T().", this.declaringClass);
		}
		code.add("$L", this.methodName);
		addArguments(code, arguments);
		return code.build();
	}

	private CodeBlock toInvokeCodeBlockForStatic(@Nullable String instanceVariable,
			CodeBlock[] arguments) {

		Assert.isTrue(instanceVariable == null,
				"'instanceVariable' must be null for static method references");
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("$T.$L", this.declaringClass, this.methodName);
		addArguments(code, arguments);
		return code.build();
	}

	private void addArguments(CodeBlock.Builder code, CodeBlock[] arguments) {
		code.add("(");
		for (int i = 0; i < arguments.length; i++) {
			if (i != 0) {
				code.add(", ");
			}
			code.add(arguments[i]);
		}
		code.add(")");
	}

	@Override
	public String toString() {
		return switch (this.kind) {
			case INSTANCE -> ((this.declaringClass != null) ? "<" + this.declaringClass + ">"
					: "<instance>") + "::" + this.methodName;
			case STATIC -> this.declaringClass + "::" + this.methodName;
		};
	}


	private enum Kind {
		INSTANCE, STATIC
	}

}
