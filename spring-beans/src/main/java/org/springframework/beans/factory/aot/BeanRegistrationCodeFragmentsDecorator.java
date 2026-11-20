/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

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
	public ClassName getTarget(RegisteredBean registeredBean) {
		return this.delegate.getTarget(registeredBean);
	}

	@Override
	public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
			ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

		return this.delegate.generateNewBeanDefinitionCode(generationContext, beanType, beanRegistrationCode);
	}

	@Override
	public CodeBlock generateSetBeanDefinitionPropertiesCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

		return this.delegate.generateSetBeanDefinitionPropertiesCode(
				generationContext, beanRegistrationCode, beanDefinition, attributeFilter);
	}

	@Override
	public CodeBlock generateSetBeanInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			CodeBlock instanceSupplierCode, List<MethodReference> postProcessors) {

		return this.delegate.generateSetBeanInstanceSupplierCode(generationContext,
				beanRegistrationCode, instanceSupplierCode, postProcessors);
	}

	@Override
	public CodeBlock generateInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			boolean allowDirectSupplierShortcut) {

		return this.delegate.generateInstanceSupplierCode(generationContext,
				beanRegistrationCode, allowDirectSupplierShortcut);
	}

	@Override
	public CodeBlock generateReturnCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		return this.delegate.generateReturnCode(generationContext, beanRegistrationCode);
	}

}
