/*
 * Copyright 2002-present the original author or authors.
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

import java.util.List;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.MethodSpec;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to
 * register bean definitions and aliases.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 * @see BeanRegistrationsAotProcessor
 */
class BeanRegistrationsAotContribution
		implements BeanFactoryInitializationAotContribution {

	private static final String BEAN_FACTORY_PARAMETER_NAME = "beanFactory";

	private static final ArgumentCodeGenerator argumentCodeGenerator = ArgumentCodeGenerator
			.of(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);

	private final List<Registration> registrations;


	BeanRegistrationsAotContribution(List<Registration> registrations) {
		this.registrations = registrations;
	}


	@Override
	public void applyTo(GenerationContext generationContext,
						BeanFactoryInitializationCode beanFactoryInitializationCode) {

		int partitionSize = 5000;
		int total = registrations.size();
		int parts = (total + partitionSize - 1) / partitionSize;

		for (int part = 0; part < parts; part++) {
			int start = part * partitionSize;
			int end = Math.min(start + partitionSize, total);
			List<Registration> slice = registrations.subList(start, end);

			final int partNumber = part;
			String feature = "BeanFactoryRegistrations" + (part > 0 ? (part + 1) : "1");
			GeneratedClass generatedClass = generationContext.getGeneratedClasses()
					.addForFeature(feature, type -> {
						type.addJavadoc("Register bean definitions (slice " + (partNumber + 1) + ").");
						type.addModifiers(Modifier.PUBLIC);
					});

			BeanRegistrationsCodeGenerator codeGen = new BeanRegistrationsCodeGenerator(generatedClass);
			BeanDefinitionsRegistrationGenerator generator = new BeanDefinitionsRegistrationGenerator(
					generationContext, codeGen, slice, start);

			GeneratedMethod generatedBeanDefinitionsMethod = generator.generateRegisterBeanDefinitionsMethod();
			MethodReference beanDefsRef = generatedBeanDefinitionsMethod.toMethodReference();
			beanFactoryInitializationCode.addInitializer(beanDefsRef);


			GeneratedMethod aliases = codeGen.getMethods()
					.add("registerAliases", method -> generateRegisterAliasesMethodForSlice(method, slice));
			beanFactoryInitializationCode.addInitializer(aliases.toMethodReference());
			generateRegisterHints(generationContext.getRuntimeHints(), slice);
		}
	}

	private void generateRegisterAliasesMethod(MethodSpec.Builder method) {
		method.addJavadoc("Register the aliases.");
		method.addModifiers(Modifier.PUBLIC);
		method.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
		CodeBlock.Builder code = CodeBlock.builder();
		this.registrations.forEach(registration -> {
			for (String alias : registration.aliases()) {
				code.addStatement("$L.registerAlias($S, $S)", BEAN_FACTORY_PARAMETER_NAME,
						registration.beanName(), alias);
			}
		});
		method.addCode(code.build());
	}

	private void generateRegisterAliasesMethodForSlice(MethodSpec.Builder method, List<Registration> slice) {
		method.addJavadoc("Register the aliases.");
		method.addModifiers(Modifier.PUBLIC);
		method.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
		CodeBlock.Builder code = CodeBlock.builder();
		slice.forEach(registration -> {
			for (String alias : registration.aliases()) {
				code.addStatement("$L.registerAlias($S, $S)", BEAN_FACTORY_PARAMETER_NAME,
						registration.beanName(), alias);
			}
		});
		method.addCode(code.build());
	}

	private void generateRegisterHints(RuntimeHints runtimeHints, List<Registration> registrations) {
		registrations.forEach(registration -> {
			ReflectionHints hints = runtimeHints.reflection();
			Class<?> beanClass = registration.registeredBean.getBeanClass();
			hints.registerType(beanClass);
			hints.registerForInterfaces(beanClass, TypeHint.Builder::withMembers);
		});
	}

	/**
	 * Gather the necessary information to register a particular bean.
	 * @param registeredBean the bean to register
	 * @param methodGenerator the {@link BeanDefinitionMethodGenerator} to use
	 * @param aliases the bean aliases, if any
	 */
	record Registration(RegisteredBean registeredBean, BeanDefinitionMethodGenerator methodGenerator, String[] aliases) {

		String beanName() {
			return this.registeredBean.getBeanName();
		}

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

	static final class BeanDefinitionsRegistrationGenerator {

		private final GenerationContext generationContext;

		private final BeanRegistrationsCodeGenerator codeGenerator;

		private final List<Registration> registrations;

		private final int globalOffset;


		BeanDefinitionsRegistrationGenerator(GenerationContext generationContext,
											 BeanRegistrationsCodeGenerator codeGenerator, List<Registration> registrations, int globalOffset) {

			this.generationContext = generationContext;
			this.codeGenerator = codeGenerator;
			this.registrations = registrations;
			this.globalOffset = globalOffset;
		}


		GeneratedMethod generateRegisterBeanDefinitionsMethod() {
			return this.codeGenerator.getMethods().add("registerBeanDefinitions", method -> {
				method.addJavadoc("Register the bean definitions.");
				method.addModifiers(Modifier.PUBLIC);
				method.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);

				int total = this.registrations.size();

				// Debug logs
				System.out.println("Generating registerBeanDefinitions: globalOffset = " + this.globalOffset +
						", registrations.size = " + total);

				if (total <= 1000) {
					System.out.println("Generating inline registrations (no slicing needed)...");
					generateRegisterBeanDefinitionMethods(method, this.registrations);
				} else {
					CodeBlock.Builder code = CodeBlock.builder();
					code.add("// Registration is sliced to avoid exceeding size limit\n");

					for (int index = 0, localStart = 0; localStart < total; index++, localStart += 1000) {
						int localEnd = Math.min(localStart + 1000, total);
						int globalStart = this.globalOffset + localStart;
						int globalEnd = this.globalOffset + localEnd;

						String methodName = "registerBeanDefinitions" + (index + 1);
						List<Registration> slice = this.registrations.subList(localStart, localEnd);

						System.out.println("Generating slice method: " + methodName +
								" (globalStart = " + globalStart + ", globalEnd = " + (globalEnd - 1) + ", size = " + slice.size() + ")");

						GeneratedMethod sliceMethod = generateSliceMethod(methodName, slice, globalStart, globalEnd);
						code.addStatement("$L($L)", methodName, BEAN_FACTORY_PARAMETER_NAME);
					}
					method.addCode(code.build());
				}
			});
		}

		private GeneratedMethod generateSliceMethod(String methodName, List<Registration> slice, int start, int end) {
			String description = "Register the bean definitions from %s to %s.".formatted(start, end - 1);
			System.out.println("Generating slice from " + start + " to " + (end - 1));
			return this.codeGenerator.getMethods().add(methodName, method -> {
				method.addJavadoc(description);
				method.addModifiers(Modifier.PRIVATE);
				method.addParameter(DefaultListableBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
				generateRegisterBeanDefinitionMethods(method, slice);
			});
		}

		private void generateRegisterBeanDefinitionMethods(MethodSpec.Builder method,
														   Iterable<Registration> registrations) {
			CodeBlock.Builder code = CodeBlock.builder();
			registrations.forEach(registration -> {
				try {
					CodeBlock methodInvocation = generateBeanRegistration(registration);
					code.addStatement("$L.registerBeanDefinition($S, $L)",
							BEAN_FACTORY_PARAMETER_NAME, registration.beanName(), methodInvocation);
				}
				catch (AotException ex) {
					throw ex;
				}
				catch (Exception ex) {
					throw new AotBeanProcessingException(registration.registeredBean,
							"failed to generate code for bean definition", ex);
				}
			});
			method.addCode(code.build());
		}

		private CodeBlock generateBeanRegistration(Registration registration) {
			MethodReference beanDefinitionMethod = registration.methodGenerator
					.generateBeanDefinitionMethod(this.generationContext, this.codeGenerator);
			return beanDefinitionMethod.toInvokeCodeBlock(
					ArgumentCodeGenerator.none(), this.codeGenerator.getClassName());
		}
	}

}