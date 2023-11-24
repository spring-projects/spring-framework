/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;

/**
 * Generate the various fragments of code needed to register a bean.
 * <p>
 * A default implementation is provided that suits most needs and custom code
 * fragments are only expected to be used by library authors having built custom
 * arrangement on top of the core container.
 * <p>
 * Users are not expected to implement this interface directly, but rather extends
 * from {@link BeanRegistrationCodeFragmentsDecorator} and only override the
 * necessary method(s).
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see BeanRegistrationCodeFragmentsDecorator
 * @see BeanRegistrationAotContribution#withCustomCodeFragments(UnaryOperator)
 */
public interface BeanRegistrationCodeFragments {

	/**
	 * The variable name to used when creating the bean definition.
	 */
	String BEAN_DEFINITION_VARIABLE = "beanDefinition";

	/**
	 * The variable name to used when creating the bean definition.
	 */
	String INSTANCE_SUPPLIER_VARIABLE = "instanceSupplier";


	/**
	 * Return the target for the registration. Used to determine where to write
	 * the code. This should take into account visibility issue, such as
	 * package access of an element of the bean to register.
	 * @param registeredBean the registered bean
	 * @return the target {@link ClassName}
	 */
	ClassName getTarget(RegisteredBean registeredBean);

	/**
	 * Generate the code that defines the new bean definition instance.
	 * <p>
	 * This should declare a variable named {@value BEAN_DEFINITION_VARIABLE}
	 * so that further fragments can refer to the variable to further tune
	 * the bean definition.
	 * @param generationContext the generation context
	 * @param beanType the bean type
	 * @param beanRegistrationCode the bean registration code
	 * @return the generated code
	 */
	CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
			ResolvableType beanType, BeanRegistrationCode beanRegistrationCode);

	/**
	 * Generate the code that sets the properties of the bean definition.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @param attributeFilter any attribute filtering that should be applied
	 * @return the generated code
	 */
	CodeBlock generateSetBeanDefinitionPropertiesCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			RootBeanDefinition beanDefinition, Predicate<String> attributeFilter);

	/**
	 * Generate the code that sets the instance supplier on the bean definition.
	 * <p>
	 * The {@code postProcessors} represent methods to be exposed once the
	 * instance has been created to further configure it. Each method should
	 * accept two parameters, the {@link RegisteredBean} and the bean
	 * instance, and should return the modified bean instance.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @param instanceSupplierCode the instance supplier code supplier code
	 * @param postProcessors any instance post processors that should be applied
	 * @return the generated code
	 * @see #generateInstanceSupplierCode
	 */
	CodeBlock generateSetBeanInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			CodeBlock instanceSupplierCode, List<MethodReference> postProcessors);

	/**
	 * Generate the instance supplier code.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @param allowDirectSupplierShortcut if direct suppliers may be used rather
	 * than always needing an {@link InstanceSupplier}
	 * @return the generated code
	 */
	CodeBlock generateInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			boolean allowDirectSupplierShortcut);

	/**
	 * Generate the return statement.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the bean registration code
	 * @return the generated code
	 */
	CodeBlock generateReturnCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

}
