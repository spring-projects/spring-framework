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
import java.util.function.UnaryOperator;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;

/**
 * A {@link BeanRegistrationCodeFragments} decorator implementation. Typically
 * used when part of the default code fragments have to customized, by extending
 * this class and using it as part of
 * {@link BeanRegistrationAotContribution#withCustomCodeFragments(UnaryOperator)}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public class BeanRegistrationCodeFragmentsDecorator implements BeanRegistrationCodeFragments {

	private final BeanRegistrationCodeFragments delegate;


	protected BeanRegistrationCodeFragmentsDecorator(BeanRegistrationCodeFragments delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	@Override
	public ClassName getTarget(RegisteredBean registeredBean, Executable constructorOrFactoryMethod) {
		return this.delegate.getTarget(registeredBean, constructorOrFactoryMethod);
	}

	@Override
	public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
			ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

		return this.delegate.generateNewBeanDefinitionCode(generationContext,
				beanType, beanRegistrationCode);
	}

	@Override
	public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
			Predicate<String> attributeFilter) {

		return this.delegate.generateSetBeanDefinitionPropertiesCode(
				generationContext, beanRegistrationCode, beanDefinition, attributeFilter);
	}

	@Override
	public CodeBlock generateSetBeanInstanceSupplierCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, CodeBlock instanceSupplierCode,
			List<MethodReference> postProcessors) {

		return this.delegate.generateSetBeanInstanceSupplierCode(generationContext,
				beanRegistrationCode, instanceSupplierCode, postProcessors);
	}

	@Override
	public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, Executable constructorOrFactoryMethod,
			boolean allowDirectSupplierShortcut) {

		return this.delegate.generateInstanceSupplierCode(generationContext,
				beanRegistrationCode, constructorOrFactoryMethod, allowDirectSupplierShortcut);
	}

	@Override
	public CodeBlock generateReturnCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode) {

		return this.delegate.generateReturnCode(generationContext, beanRegistrationCode);
	}

}
