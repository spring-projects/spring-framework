/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.aot.generate.AccessControl;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.aot.generate.ValueCodeGenerator;
import org.springframework.aot.generate.ValueCodeGenerator.Delegate;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.AotServices.Loader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * Internal {@link BeanRegistrationCodeFragments} implementation used by default.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefaultBeanRegistrationCodeFragments implements BeanRegistrationCodeFragments {

	private static final ValueCodeGenerator valueCodeGenerator = ValueCodeGenerator.withDefaults();

	private final BeanRegistrationsCode beanRegistrationsCode;

	private final RegisteredBean registeredBean;

	private final BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory;

	private final Supplier<InstantiationDescriptor> instantiationDescriptor;


	DefaultBeanRegistrationCodeFragments(BeanRegistrationsCode beanRegistrationsCode,
			RegisteredBean registeredBean,
			BeanDefinitionMethodGeneratorFactory beanDefinitionMethodGeneratorFactory) {

		this.beanRegistrationsCode = beanRegistrationsCode;
		this.registeredBean = registeredBean;
		this.beanDefinitionMethodGeneratorFactory = beanDefinitionMethodGeneratorFactory;
		this.instantiationDescriptor = SingletonSupplier.of(registeredBean::resolveInstantiationDescriptor);
	}


	@Override
	public ClassName getTarget(RegisteredBean registeredBean) {
		if (hasInstanceSupplier()) {
			throw new AotBeanProcessingException(registeredBean, "instance supplier is not supported");
		}
		Class<?> target = extractDeclaringClass(registeredBean, this.instantiationDescriptor.get());
		while (target.getName().startsWith("java.") && registeredBean.isInnerBean()) {
			RegisteredBean parent = registeredBean.getParent();
			Assert.state(parent != null, "No parent available for inner bean");
			target = parent.getBeanClass();
		}
		return (target.isArray() ? ClassName.get(target.getComponentType()) : ClassName.get(target));
	}

	private Class<?> extractDeclaringClass(RegisteredBean registeredBean, InstantiationDescriptor instantiationDescriptor) {
		Class<?> declaringClass = ClassUtils.getUserClass(instantiationDescriptor.targetClass());
		if (instantiationDescriptor.executable() instanceof Constructor<?> ctor &&
				AccessControl.forMember(ctor).isPublic() && FactoryBean.class.isAssignableFrom(declaringClass)) {
			return extractTargetClassFromFactoryBean(declaringClass, registeredBean.getBeanType());
		}
		return declaringClass;
	}

	/**
	 * Extract the target class of a public {@link FactoryBean} based on its
	 * constructor. If the implementation does not resolve the target class
	 * because it itself uses a generic, attempt to extract it from the bean type.
	 * @param factoryBeanType the factory bean type
	 * @param beanType the bean type
	 * @return the target class to use
	 */
	private Class<?> extractTargetClassFromFactoryBean(Class<?> factoryBeanType, ResolvableType beanType) {
		ResolvableType target = ResolvableType.forType(factoryBeanType).as(FactoryBean.class).getGeneric(0);
		if (target.getType().equals(Class.class)) {
			return target.toClass();
		}
		else if (factoryBeanType.isAssignableFrom(beanType.toClass())) {
			return beanType.as(FactoryBean.class).getGeneric(0).toClass();
		}
		return beanType.toClass();
	}

	@Override
	public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
			ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

		CodeBlock.Builder code = CodeBlock.builder();
		RootBeanDefinition mbd = this.registeredBean.getMergedBeanDefinition();
		Class<?> beanClass = (mbd.hasBeanClass() ? ClassUtils.getUserClass(mbd.getBeanClass()) : null);
		CodeBlock beanClassCode = generateBeanClassCode(
				beanRegistrationCode.getClassName().packageName(),
				(beanClass != null ? beanClass : beanType.toClass()));
		code.addStatement("$T $L = new $T($L)", RootBeanDefinition.class,
				BEAN_DEFINITION_VARIABLE, RootBeanDefinition.class, beanClassCode);
		if (targetTypeNecessary(beanType, beanClass)) {
			code.addStatement("$L.setTargetType($L)", BEAN_DEFINITION_VARIABLE, generateBeanTypeCode(beanType));
		}
		return code.build();
	}

	private CodeBlock generateBeanClassCode(String targetPackage, Class<?> beanClass) {
		if (Modifier.isPublic(beanClass.getModifiers()) || targetPackage.equals(beanClass.getPackageName())) {
			return CodeBlock.of("$T.class", beanClass);
		}
		else {
			return CodeBlock.of("$S", beanClass.getName());
		}
	}

	private CodeBlock generateBeanTypeCode(ResolvableType beanType) {
		if (!beanType.hasGenerics()) {
			return valueCodeGenerator.generateCode(ClassUtils.getUserClass(beanType.toClass()));
		}
		return valueCodeGenerator.generateCode(beanType);
	}

	private boolean targetTypeNecessary(ResolvableType beanType, @Nullable Class<?> beanClass) {
		if (beanType.hasGenerics()) {
			return true;
		}
		if (beanClass != null && this.registeredBean.getMergedBeanDefinition().getFactoryMethodName() != null) {
			return true;
		}
		return (beanClass != null && !beanType.toClass().equals(beanClass));
	}

	@Override
	public CodeBlock generateSetBeanDefinitionPropertiesCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

		Loader loader = AotServices.factories(this.registeredBean.getBeanFactory().getBeanClassLoader());
		List<Delegate> additionalDelegates = loader.load(Delegate.class).asList();

		return new BeanDefinitionPropertiesCodeGenerator(
				generationContext.getRuntimeHints(), attributeFilter,
				beanRegistrationCode.getMethods(), additionalDelegates,
				(name, value) -> generateValueCode(generationContext, name, value))
				.generateCode(beanDefinition);
	}

	@Nullable
	protected CodeBlock generateValueCode(GenerationContext generationContext, String name, Object value) {
		RegisteredBean innerRegisteredBean = getInnerRegisteredBean(value);
		if (innerRegisteredBean != null) {
			BeanDefinitionMethodGenerator methodGenerator = this.beanDefinitionMethodGeneratorFactory
					.getBeanDefinitionMethodGenerator(innerRegisteredBean, name);
			Assert.state(methodGenerator != null, "Unexpected filtering of inner-bean");
			MethodReference generatedMethod = methodGenerator
					.generateBeanDefinitionMethod(generationContext, this.beanRegistrationsCode);
			return generatedMethod.toInvokeCodeBlock(ArgumentCodeGenerator.none());
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
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			CodeBlock instanceSupplierCode, List<MethodReference> postProcessors) {

		CodeBlock.Builder code = CodeBlock.builder();
		if (postProcessors.isEmpty()) {
			code.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE, instanceSupplierCode);
			return code.build();
		}
		code.addStatement("$T $L = $L",
				ParameterizedTypeName.get(InstanceSupplier.class, this.registeredBean.getBeanClass()),
				INSTANCE_SUPPLIER_VARIABLE, instanceSupplierCode);
		for (MethodReference postProcessor : postProcessors) {
			code.addStatement("$L = $L.andThen($L)", INSTANCE_SUPPLIER_VARIABLE,
					INSTANCE_SUPPLIER_VARIABLE, postProcessor.toCodeBlock());
		}
		code.addStatement("$L.setInstanceSupplier($L)", BEAN_DEFINITION_VARIABLE,
				INSTANCE_SUPPLIER_VARIABLE);
		return code.build();
	}

	@Override
	public CodeBlock generateInstanceSupplierCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
			boolean allowDirectSupplierShortcut) {

		if (hasInstanceSupplier()) {
			throw new AotBeanProcessingException(this.registeredBean, "instance supplier is not supported");
		}
		return new InstanceSupplierCodeGenerator(generationContext,
				beanRegistrationCode.getClassName(), beanRegistrationCode.getMethods(), allowDirectSupplierShortcut)
				.generateCode(this.registeredBean, this.instantiationDescriptor.get());
	}

	@Override
	public CodeBlock generateReturnCode(
			GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("return $L", BEAN_DEFINITION_VARIABLE);
		return code.build();
	}

	private boolean hasInstanceSupplier() {
		return this.registeredBean.getMergedBeanDefinition().getInstanceSupplier() != null;
	}

}
