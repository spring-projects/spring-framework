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

package org.springframework.beans.factory.generator;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.aot.generator.ProtectedAccess;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;

/**
 * The initialization of a {@link BeanFactory}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 6.0
 */
public class BeanFactoryInitialization {

	private final GeneratedTypeContext generatedTypeContext;

	private final CodeBlock.Builder codeContributions;

	public BeanFactoryInitialization(GeneratedTypeContext generatedTypeContext) {
		this.generatedTypeContext = generatedTypeContext;
		this.codeContributions = CodeBlock.builder();
	}

	/**
	 * Return the {@link GeneratedTypeContext} to use to contribute
	 * additional methods or hints.
	 * @return the generation context
	 */
	public GeneratedTypeContext generatedTypeContext() {
		return this.generatedTypeContext;
	}

	/**
	 * Contribute code that initializes the bean factory and that does not
	 * require any privileged access.
	 * @param code the code to contribute
	 */
	public void contribute(Consumer<Builder> code) {
		CodeBlock.Builder builder = CodeBlock.builder();
		code.accept(builder);
		CodeBlock codeBlock = builder.build();
		this.codeContributions.add(codeBlock);
		if (!codeBlock.toString().endsWith("\n")) {
			this.codeContributions.add("\n");
		}
	}

	/**
	 * Contribute code that initializes the bean factory. If privileged access
	 * is required, a public method in the target package is created and
	 * invoked, rather than contributing the code directly.
	 * @param protectedAccess the {@link ProtectedAccess} instance to use
	 * @param methodName a method name to use if privileged access is required
	 * @param methodBody the contribution
	 */
	public void contribute(ProtectedAccess protectedAccess, Supplier<String> methodName,
			Consumer<Builder> methodBody) {
		String targetPackageName = this.generatedTypeContext.getMainGeneratedType().getClassName().packageName();
		String protectedPackageName = protectedAccess.getPrivilegedPackageName(targetPackageName);
		if (protectedPackageName != null) {
			GeneratedType type = this.generatedTypeContext.getGeneratedType(protectedPackageName);
			MethodSpec.Builder method = MethodSpec.methodBuilder(methodName.get())
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addParameter(DefaultListableBeanFactory.class, "beanFactory");
			CodeBlock.Builder code = CodeBlock.builder();
			methodBody.accept(code);
			method.addCode(code.build());
			contribute(main -> main.addStatement("$T.$N(beanFactory)", type.getClassName(), type.addMethod(method)));
		}
		else {
			contribute(methodBody);
		}
	}

	/**
	 * Return the code that has been contributed to this instance.
	 * @return the code
	 */
	public CodeBlock toCodeBlock() {
		return this.codeContributions.build();
	}

}
