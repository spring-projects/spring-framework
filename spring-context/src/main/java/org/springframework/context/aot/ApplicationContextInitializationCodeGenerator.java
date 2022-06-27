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

package org.springframework.context.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.javapoet.TypeSpec;

/**
 * Internal code generator to create the application context initializer.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class ApplicationContextInitializationCodeGenerator
		implements BeanFactoryInitializationCode {

	private static final String APPLICATION_CONTEXT_VARIABLE = "applicationContext";


	private final GeneratedMethods generatedMethods = new GeneratedMethods();

	private final List<MethodReference> initializers = new ArrayList<>();


	@Override
	public MethodGenerator getMethodGenerator() {
		return this.generatedMethods;
	}

	@Override
	public void addInitializer(MethodReference methodReference) {
		this.initializers.add(methodReference);
	}

	Consumer<TypeSpec.Builder> generateJavaFile() {
		return builder -> {
			builder.addJavadoc(
					"{@link $T} to restore an application context based on previous AOT processing.",
					ApplicationContextInitializer.class);
			builder.addModifiers(Modifier.PUBLIC);
			builder.addSuperinterface(ParameterizedTypeName.get(
					ApplicationContextInitializer.class, GenericApplicationContext.class));
			builder.addMethod(generateInitializeMethod());
			this.generatedMethods.doWithMethodSpecs(builder::addMethod);
		};
	}

	private MethodSpec generateInitializeMethod() {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(GenericApplicationContext.class,
				APPLICATION_CONTEXT_VARIABLE);
		builder.addCode(generateInitializeCode());
		return builder.build();
	}

	private CodeBlock generateInitializeCode() {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.addStatement("$T $L = $L.getDefaultListableBeanFactory()",
				DefaultListableBeanFactory.class, BEAN_FACTORY_VARIABLE,
				APPLICATION_CONTEXT_VARIABLE);
		builder.addStatement("$L.setAutowireCandidateResolver(new $T())",
				BEAN_FACTORY_VARIABLE, ContextAnnotationAutowireCandidateResolver.class);
		for (MethodReference initializer : this.initializers) {
			builder.addStatement(
					initializer.toInvokeCodeBlock(CodeBlock.of(BEAN_FACTORY_VARIABLE)));
		}
		return builder.build();
	}

}
