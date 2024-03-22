/*
 * Copyright 2002-2024 the original author or authors.
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

import javax.lang.model.element.Modifier;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default {@link MethodReference} implementation based on a {@link MethodSpec}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
public class DefaultMethodReference implements MethodReference {

	private final MethodSpec method;

	@Nullable
	private final ClassName declaringClass;


	public DefaultMethodReference(MethodSpec method, @Nullable ClassName declaringClass) {
		this.method = method;
		this.declaringClass = declaringClass;
	}


	@Override
	public CodeBlock toCodeBlock() {
		String methodName = this.method.name;
		if (isStatic()) {
			Assert.state(this.declaringClass != null, "Static method reference must define a declaring class");
			return CodeBlock.of("$T::$L", this.declaringClass, methodName);
		}
		else {
			return CodeBlock.of("this::$L", methodName);
		}
	}

	@Override
	public CodeBlock toInvokeCodeBlock(ArgumentCodeGenerator argumentCodeGenerator,
			@Nullable ClassName targetClassName) {

		String methodName = this.method.name;
		CodeBlock.Builder code = CodeBlock.builder();
		if (isStatic()) {
			Assert.state(this.declaringClass != null, "Static method reference must define a declaring class");
			if (this.declaringClass.equals(targetClassName)) {
				code.add("$L", methodName);
			}
			else {
				code.add("$T.$L", this.declaringClass, methodName);
			}
		}
		else {
			if (this.declaringClass != null && !this.declaringClass.equals(targetClassName)) {
				code.add(instantiateDeclaringClass(this.declaringClass));
			}
			code.add("$L", methodName);
		}
		code.add("(");
		addArguments(code, argumentCodeGenerator);
		code.add(")");
		return code.build();
	}

	/**
	 * Add the code for the method arguments using the specified
	 * {@link ArgumentCodeGenerator} if necessary.
	 * @param code the code builder to use to add method arguments
	 * @param argumentCodeGenerator the code generator to use
	 */
	protected void addArguments(CodeBlock.Builder code, ArgumentCodeGenerator argumentCodeGenerator) {
		List<CodeBlock> arguments = new ArrayList<>();
		TypeName[] argumentTypes = this.method.parameters.stream()
				.map(parameter -> parameter.type).toArray(TypeName[]::new);
		for (int i = 0; i < argumentTypes.length; i++) {
			TypeName argumentType = argumentTypes[i];
			CodeBlock argumentCode = argumentCodeGenerator.generateCode(argumentType);
			if (argumentCode == null) {
				throw new IllegalArgumentException("Could not generate code for " + this
						+ ": parameter " + i + " of type " + argumentType + " is not supported");
			}
			arguments.add(argumentCode);
		}
		code.add(CodeBlock.join(arguments, ", "));
	}

	protected CodeBlock instantiateDeclaringClass(ClassName declaringClass) {
		return CodeBlock.of("new $T().", declaringClass);
	}

	private boolean isStatic() {
		return this.method.modifiers.contains(Modifier.STATIC);
	}

	@Override
	public String toString() {
		String methodName = this.method.name;
		if (isStatic()) {
			return this.declaringClass + "::" + methodName;
		}
		else {
			return (this.declaringClass != null ?
					"<" + this.declaringClass + ">" : "<instance>") + "::" + methodName;
		}
	}

}
