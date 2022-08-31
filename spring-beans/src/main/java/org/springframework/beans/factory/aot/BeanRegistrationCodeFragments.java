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
import java.util.function.Predicate;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;

/**
 * Class used to generate the various fragments of code needed to register a
 * bean.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public abstract class BeanRegistrationCodeFragments {

	/**
	 * The variable name to used when creating the bean definition.
	 */
	protected static final String BEAN_DEFINITION_VARIABLE = "beanDefinition";

	/**
	 * The variable name to used when creating the bean definition.
	 */
	protected static final String INSTANCE_SUPPLIER_VARIABLE = "instanceSupplier";


	private final BeanRegistrationCodeFragments codeFragments;


	protected BeanRegistrationCodeFragments(BeanRegistrationCodeFragments codeFragments) {
		Assert.notNull(codeFragments, "'codeFragments' must not be null");
		this.codeFragments = codeFragments;
	}


	/**
	 * Package-private constructor exclusively for
	 * {@link DefaultBeanRegistrationCodeFragments}. All methods are overridden
	 * so {@code this.codeFragments} is never actually used.
	 */
	BeanRegistrationCodeFragments() {
		this.codeFragments = this;
	}

	/**
	 * Return the target for the registration. Used to determine where to write
	 * the code.
	 * @param registeredBean the registered bean
	 * @param constructorOrFactoryMethod the constructor or factory method
	 * @return the target class
	 */
	public Class<?> getTarget(RegisteredBean registeredBean,
			Executable constructorOrFactoryMethod) {

		return this.codeFragments.getTarget(registeredBean, constructorOrFactoryMethod);
	}

	/**
	 * Generate the code that defines the new bean definition instance.
	 * @param generationContext the generation context
	 * @param beanType the bean type
	 * @param beanRegistrationCode the bean registration code
	 * @return the generated code
	 */
	public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
			ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

		return this.codeFragments.generateNewBeanDefinitionCode(generationContext,
				beanType, beanRegistrationCode);

	}

	/**
	 * Generate the code that sets the properties of the bean definition.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @param attributeFilter any attribute filtering that should be applied
	 * @return the generated code
	 */
	public CodeBlock generateSetBeanDefinitionPropertiesCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

		return this.codeFragments.generateSetBeanDefinitionPropertiesCode(
				generationContext, beanRegistrationCode, beanDefinition, attributeFilter);

	}

	/**
	 * Generate the code that sets the instance supplier on the bean definition.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @param instanceSupplierCode the instance supplier code supplier code
	 * @param postProcessors any instance post processors that should be applied
	 * @return the generated code
	 * @see #generateInstanceSupplierCode
	 */
	public CodeBlock generateSetBeanInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			CodeBlock instanceSupplierCode, List<MethodReference> postProcessors) {

		return this.codeFragments.generateSetBeanInstanceSupplierCode(generationContext,
				beanRegistrationCode, instanceSupplierCode, postProcessors);
	}

	/**
	 * Generate the instance supplier code.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @param constructorOrFactoryMethod the constructor or factory method for
	 * the bean
	 * @param allowDirectSupplierShortcut if direct suppliers may be used rather
	 * than always needing an {@link InstanceSupplier}
	 * @return the generated code
	 */
	public CodeBlock generateInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			Executable constructorOrFactoryMethod, boolean allowDirectSupplierShortcut) {

		return this.codeFragments.generateInstanceSupplierCode(generationContext,
				beanRegistrationCode, constructorOrFactoryMethod, allowDirectSupplierShortcut);
	}

	/**
	 * Generate the return statement.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @return the generated code
	 */
	public CodeBlock generateReturnCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		return this.codeFragments.generateReturnCode(generationContext, beanRegistrationCode);
	}

}
