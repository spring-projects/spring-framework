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

package org.springframework.beans.factory.aot;

import java.util.Map;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to
 * register bean definitions.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see BeanRegistrationsAotProcessor
 */
class BeanRegistrationsAotContribution
		implements BeanFactoryInitializationAotContribution {

	private static final String BEAN_FACTORY_PARAMETER_NAME = "beanFactory";

	private final Map<String, BeanDefinitionMethodGenerator> registrations;


	BeanRegistrationsAotContribution(
			Map<String, BeanDefinitionMethodGenerator> registrations) {

		this.registrations = registrations;
	}


	@Override
	public void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode) {

		GeneratedClass generatedClass = generationContext.getGeneratedClasses()
				.addForFeature("BeanFactoryRegistrations", type -> {
					type.addJavadoc("Register bean definitions for the bean factory.");
					type.addModifiers(Modifier.PUBLIC);
				});
		BeanRegistrationsCodeGenerator codeGenerator = new BeanRegistrationsCodeGenerator(generatedClass);
		GeneratedMethod generatedMethod = codeGenerator.getMethods().add("registerBeanDefinitions", method ->
				generateRegisterMethod(method, generationContext, codeGenerator));
		beanFactoryInitializationCode.addInitializer(
				MethodReference.of(generatedClass.getName(), generatedMethod.getName()));
	}

	private void generateRegisterMethod(MethodSpec.Builder method,
			GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode) {

		method.addJavadoc("Register the bean definitions.");
		method.addModifiers(Modifier.PUBLIC);
		method.addParameter(DefaultListableBeanFactory.class,
				BEAN_FACTORY_PARAMETER_NAME);
		CodeBlock.Builder code = CodeBlock.builder();
		this.registrations.forEach((beanName, beanDefinitionMethodGenerator) -> {
			MethodReference beanDefinitionMethod = beanDefinitionMethodGenerator
					.generateBeanDefinitionMethod(generationContext,
							beanRegistrationsCode);
			code.addStatement("$L.registerBeanDefinition($S, $L)",
					BEAN_FACTORY_PARAMETER_NAME, beanName,
					beanDefinitionMethod.toInvokeCodeBlock());
		});
		method.addCode(code.build());
	}


	/**
	 * {@link BeanRegistrationsCode} with generation support.
	 */
	static class BeanRegistrationsCodeGenerator implements BeanRegistrationsCode {

		private final GeneratedClass generatedClass;

		public BeanRegistrationsCodeGenerator(GeneratedClass generatedClass) {
			this.generatedClass = generatedClass;
		}


		@Override
		public ClassName getClassName() {
			return this.generatedClass.getName();
		}

		@Override
		public GeneratedMethods getMethods() {
			return this.generatedClass.getMethods();
		}

	}

}
