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

import java.lang.reflect.Executable;
import java.util.List;

import javax.lang.model.element.Modifier;

import org.springframework.aot.generate.ClassGenerator.JavaFileGenerator;
import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodNameGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;

/**
 * Generates a method that returns a {@link BeanDefinition} to be registered.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see BeanDefinitionMethodGeneratorFactory
 */
class BeanDefinitionMethodGenerator {

	private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

	private final RegisteredBean registeredBean;

	private final Executable constructorOrFactoryMethod;

	@Nullable
	private final String innerBeanPropertyName;

	private final List<BeanRegistrationAotContribution> aotContributions;


	/**
	 * Create a new {@link BeanDefinitionMethodGenerator} instance.
	 * @param methodGeneratorFactory the method generator factory
	 * @param registeredBean the registered bean
	 * @param innerBeanPropertyName the inner bean property name
	 * @param aotContributions the AOT contributions
	 */
	BeanDefinitionMethodGenerator(
			BeanDefinitionMethodGeneratorFactory methodGeneratorFactory,
			RegisteredBean registeredBean, @Nullable String innerBeanPropertyName,
			List<BeanRegistrationAotContribution> aotContributions) {

		this.methodGeneratorFactory = methodGeneratorFactory;
		this.registeredBean = registeredBean;
		this.constructorOrFactoryMethod = ConstructorOrFactoryMethodResolver
				.resolve(registeredBean);
		this.innerBeanPropertyName = innerBeanPropertyName;
		this.aotContributions = aotContributions;
	}

	/**
	 * Generate the method that returns the {@link BeanDefinition} to be
	 * registered.
	 * @param generationContext the generation context
	 * @param featureNamePrefix the prefix to use for the feature name
	 * @param beanRegistrationsCode the bean registrations code
	 * @return a reference to the generated method.
	 */
	MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
			String featureNamePrefix, BeanRegistrationsCode beanRegistrationsCode) {

		BeanRegistrationCodeFragments codeFragments = getCodeFragments(generationContext,
				beanRegistrationsCode, featureNamePrefix);
		Class<?> target = codeFragments.getTarget(this.registeredBean,
				this.constructorOrFactoryMethod);
		if (!target.getName().startsWith("java.")) {
			String featureName = featureNamePrefix + "BeanDefinitions";
			GeneratedClass generatedClass = generationContext.getClassGenerator()
					.getOrGenerateClass(new BeanDefinitionsJavaFileGenerator(target),
							target, featureName);
			MethodGenerator methodGenerator = generatedClass.getMethodGenerator()
					.withName(getName());
			GeneratedMethod generatedMethod = generateBeanDefinitionMethod(
					generationContext, generatedClass.getName(), methodGenerator,
					codeFragments, Modifier.PUBLIC);
			return MethodReference.ofStatic(generatedClass.getName(),
					generatedMethod.getName());
		}
		MethodGenerator methodGenerator = beanRegistrationsCode.getMethodGenerator()
				.withName(getName());
		GeneratedMethod generatedMethod = generateBeanDefinitionMethod(generationContext,
				beanRegistrationsCode.getClassName(), methodGenerator, codeFragments,
				Modifier.PRIVATE);
		return MethodReference.ofStatic(beanRegistrationsCode.getClassName(),
				generatedMethod.getName().toString());
	}

	private BeanRegistrationCodeFragments getCodeFragments(GenerationContext generationContext,
			BeanRegistrationsCode beanRegistrationsCode, String featureNamePrefix) {

		BeanRegistrationCodeFragments codeFragments = new DefaultBeanRegistrationCodeFragments(
				beanRegistrationsCode, this.registeredBean, this.methodGeneratorFactory,
				featureNamePrefix);
		for (BeanRegistrationAotContribution aotContribution : this.aotContributions) {
			codeFragments = aotContribution.customizeBeanRegistrationCodeFragments(generationContext, codeFragments);
		}
		return codeFragments;
	}

	private GeneratedMethod generateBeanDefinitionMethod(
			GenerationContext generationContext, ClassName className,
			MethodGenerator methodGenerator, BeanRegistrationCodeFragments codeFragments,
			Modifier modifier) {

		BeanRegistrationCodeGenerator codeGenerator = new BeanRegistrationCodeGenerator(
				className, methodGenerator, this.registeredBean,
				this.constructorOrFactoryMethod, codeFragments);
		GeneratedMethod method = methodGenerator.generateMethod("get", "bean", "definition");
		this.aotContributions.forEach(aotContribution -> aotContribution
				.applyTo(generationContext, codeGenerator));
		return method.using(builder -> {
			builder.addJavadoc("Get the $L definition for '$L'",
					(!this.registeredBean.isInnerBean()) ? "bean" : "inner-bean",
					getName());
			builder.addModifiers(modifier, Modifier.STATIC);
			builder.returns(BeanDefinition.class);
			builder.addCode(codeGenerator.generateCode(generationContext));
		});
	}

	private String getName() {
		if (this.innerBeanPropertyName != null) {
			return this.innerBeanPropertyName;
		}
		if (!this.registeredBean.isGeneratedBeanName()) {
			return getSimpleBeanName(this.registeredBean.getBeanName());
		}
		RegisteredBean nonGeneratedParent = this.registeredBean;
		while (nonGeneratedParent != null && nonGeneratedParent.isGeneratedBeanName()) {
			nonGeneratedParent = nonGeneratedParent.getParent();
		}
		return (nonGeneratedParent != null)
				? MethodNameGenerator.join(
						getSimpleBeanName(nonGeneratedParent.getBeanName()), "innerBean")
				: "innerBean";
	}

	private String getSimpleBeanName(String beanName) {
		int lastDot = beanName.lastIndexOf('.');
		beanName = (lastDot != -1) ? beanName.substring(lastDot + 1) : beanName;
		int lastDollar = beanName.lastIndexOf('$');
		beanName = (lastDollar != -1) ? beanName.substring(lastDollar + 1) : beanName;
		return beanName;
	}


	/**
	 * {@link BeanDefinitionsJavaFileGenerator} to create the
	 * {@code BeanDefinitions} file.
	 */
	private static class BeanDefinitionsJavaFileGenerator implements JavaFileGenerator {

		private final Class<?> target;


		BeanDefinitionsJavaFileGenerator(Class<?> target) {
			this.target = target;
		}


		@Override
		public JavaFile generateJavaFile(ClassName className, GeneratedMethods methods) {
			TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
			classBuilder.addJavadoc("Bean definitions for {@link $T}", this.target);
			classBuilder.addModifiers(Modifier.PUBLIC);
			methods.doWithMethodSpecs(classBuilder::addMethod);
			return JavaFile.builder(className.packageName(), classBuilder.build())
					.build();
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return getClass() == obj.getClass();
		}

	}

}
