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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Internal {@link BeanRegistrationCodeFragments} implementation used by
 * default.
 *
 * @author Phillip Webb
 */
class DefaultBeanRegistrationCodeFragments extends BeanRegistrationCodeFragments {

	/**
	 * The variable name used to hold the bean type.
	 */
	private static final String BEAN_TYPE_VARIABLE = "beanType";


	private final BeanRegistrationsCode beanRegistrationsCode;

	private final RegisteredBean registeredBean;

	private final BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory;


	DefaultBeanRegistrationCodeFragments(BeanRegistrationsCode beanRegistrationsCode,
			RegisteredBean registeredBean,
			BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory) {

		this.beanRegistrationsCode = beanRegistrationsCode;
		this.registeredBean = registeredBean;
		this.beanDefinitionMethodGeneratorFactory = beanDefinitionMethodGeneratorFactory;
	}


	@Override
	public Class<?> getTarget(RegisteredBean registeredBean,
			Executable constructorOrFactoryMethod) {

		Class<?> target = ClassUtils
				.getUserClass(constructorOrFactoryMethod.getDeclaringClass());
		while (target.getName().startsWith("java.") && registeredBean.isInnerBean()) {
			target = registeredBean.getParent().getBeanClass();
		}
		return target;
	}

	@Override
	public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
			ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

		CodeBlock.Builder builder = CodeBlock.builder();
		builder.addStatement(generateBeanTypeCode(beanType));
		builder.addStatement("$T $L = new $T($L)", RootBeanDefinition.class,
				BEAN_DEFINITION_VARIABLE, RootBeanDefinition.class, BEAN_TYPE_VARIABLE);
		return builder.build();
	}

	private CodeBlock generateBeanTypeCode(ResolvableType beanType) {
		if (!beanType.hasGenerics()) {
			return CodeBlock.of("$T<?> $L = $T.class", Class.class, BEAN_TYPE_VARIABLE,
					ClassUtils.getUserClass(beanType.toClass()));
		}
		return CodeBlock.of("$T $L = $L", ResolvableType.class, BEAN_TYPE_VARIABLE,
				ResolvableTypeCodeGenerator.generateCode(beanType));
	}

	@Override
	public CodeBlock generateSetBeanDefinitionPropertiesCode(
			GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
			Predicate<String> attributeFilter) {

		return new BeanDefinitionPropertiesCodeGenerator(
				generationContext.getRuntimeHints(), attributeFilter,
				beanRegistrationCode.getMethodGenerator(),
				(name, value) -> generateValueCode(generationContext, name, value))
						.generateCode(beanDefinition);
	}

	@Nullable
	protected CodeBlock generateValueCode(GenerationContext generationContext,
			String name, Object value) {

		RegisteredBean innerRegisteredBean = getInnerRegisteredBean(value);
		if (innerRegisteredBean != null) {
			BeanDefinitionMethodGenerator methodGenerator = this.beanDefinitionMethodGeneratorFactory
					.getBeanDefinitionMethodGenerator(innerRegisteredBean, name);
			Assert.state(methodGenerator != null, "Unexpected filtering of inner-bean");
			MethodReference generatedMethod = methodGenerator
					.generateBeanDefinitionMethod(generationContext,
							this.beanRegistrationsCode);
			return generatedMethod.toInvokeCodeBlock();
		}
		return null;
	}

	@Nullable
	private RegisteredBean getInnerRegisteredBean(Object value) {
		if (value instanceof BeanDefinitionHolder beanDefinitionHolder) {
			return RegisteredBean.ofInnerBean(this.registeredBean, beanDefinitionHolder);
		}
		if (value instanceof BeanDefinition beanDefinition) {
			return RegisteredBean.ofInnerBean(this.registeredBean, beanDefinition);
		}
		return null;
	}

	@Override
	public CodeBlock generateSetBeanInstanceSupplierCode(
			GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode, CodeBlock instanceSupplierCode,
			List<MethodReference> postProcessors) {

		CodeBlock.Builder builder = CodeBlock.builder();
		if (postProcessors.isEmpty()) {
			builder.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE,
					instanceSupplierCode);
			return builder.build();
		}
		builder.addStatement("$T $L = $L",
				ParameterizedTypeName.get(InstanceSupplier.class,
						this.registeredBean.getBeanClass()),
				INSTANCE_SUPPLIER_VARIABLE, instanceSupplierCode);
		for (MethodReference postProcessor : postProcessors) {
			builder.addStatement("$L = $L.andThen($L)", INSTANCE_SUPPLIER_VARIABLE,
					INSTANCE_SUPPLIER_VARIABLE, postProcessor.toCodeBlock());
		}
		builder.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE,
				INSTANCE_SUPPLIER_VARIABLE);
		return builder.build();
	}

	@Override
	public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode,
			Executable constructorOrFactoryMethod, boolean allowDirectSupplierShortcut) {

		return new InstanceSupplierCodeGenerator(generationContext,
				beanRegistrationCode.getClassName(),
				beanRegistrationCode.getMethodGenerator(), allowDirectSupplierShortcut)
						.generateCode(this.registeredBean, constructorOrFactoryMethod);
	}

	@Override
	public CodeBlock generateReturnCode(GenerationContext generationContext,
			BeanRegistrationCode beanRegistrationCode) {

		CodeBlock.Builder builder = CodeBlock.builder();
		builder.addStatement("return $L", BEAN_DEFINITION_VARIABLE);
		return builder.build();
	}

}
