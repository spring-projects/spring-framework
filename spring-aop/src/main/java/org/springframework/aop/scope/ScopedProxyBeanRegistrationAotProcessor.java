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

package org.springframework.aop.scope;

import java.util.function.Predicate;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GeneratedMethod;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.Nullable;

/**
 * {@link BeanRegistrationAotProcessor} for {@link ScopedProxyFactoryBean}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
class ScopedProxyBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private static final Log logger = LogFactory.getLog(ScopedProxyBeanRegistrationAotProcessor.class);


	@Override
	@Nullable
	@SuppressWarnings("NullAway")
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		if (beanClass.equals(ScopedProxyFactoryBean.class)) {
			String targetBeanName = getTargetBeanName(registeredBean.getMergedBeanDefinition());
			BeanDefinition targetBeanDefinition =
					getTargetBeanDefinition(registeredBean.getBeanFactory(), targetBeanName);
			if (targetBeanDefinition == null) {
				logger.warn("Could not handle " + ScopedProxyFactoryBean.class.getSimpleName() +
						": no target bean definition found with name " + targetBeanName);
				return null;
			}
			return BeanRegistrationAotContribution.withCustomCodeFragments(codeFragments ->
					new ScopedProxyBeanRegistrationCodeFragments(codeFragments, registeredBean,
							targetBeanName, targetBeanDefinition));
		}
		return null;
	}

	@Nullable
	private String getTargetBeanName(BeanDefinition beanDefinition) {
		Object value = beanDefinition.getPropertyValues().get("targetBeanName");
		return (value instanceof String targetBeanName ? targetBeanName : null);
	}

	@Nullable
	private BeanDefinition getTargetBeanDefinition(
			ConfigurableBeanFactory beanFactory, @Nullable String targetBeanName) {

		if (targetBeanName != null && beanFactory.containsBean(targetBeanName)) {
			return beanFactory.getMergedBeanDefinition(targetBeanName);
		}
		return null;
	}


	private static class ScopedProxyBeanRegistrationCodeFragments extends BeanRegistrationCodeFragmentsDecorator {

		private static final String REGISTERED_BEAN_PARAMETER_NAME = "registeredBean";

		private final RegisteredBean registeredBean;

		private final String targetBeanName;

		private final BeanDefinition targetBeanDefinition;

		ScopedProxyBeanRegistrationCodeFragments(BeanRegistrationCodeFragments delegate,
				RegisteredBean registeredBean, String targetBeanName, BeanDefinition targetBeanDefinition) {

			super(delegate);
			this.registeredBean = registeredBean;
			this.targetBeanName = targetBeanName;
			this.targetBeanDefinition = targetBeanDefinition;
		}

		@Override
		public ClassName getTarget(RegisteredBean registeredBean) {
			return ClassName.get(this.targetBeanDefinition.getResolvableType().toClass());
		}

		@Override
		public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
				ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {

			return super.generateNewBeanDefinitionCode(generationContext,
					this.targetBeanDefinition.getResolvableType(), beanRegistrationCode);
		}

		@Override
		public CodeBlock generateSetBeanDefinitionPropertiesCode(
				GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
				RootBeanDefinition beanDefinition, Predicate<String> attributeFilter) {

			RootBeanDefinition processedBeanDefinition = new RootBeanDefinition(beanDefinition);
			processedBeanDefinition.setTargetType(this.targetBeanDefinition.getResolvableType());
			processedBeanDefinition.getPropertyValues().removePropertyValue("targetBeanName");
			return super.generateSetBeanDefinitionPropertiesCode(generationContext,
					beanRegistrationCode, processedBeanDefinition, attributeFilter);
		}

		@Override
		public CodeBlock generateInstanceSupplierCode(
				GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode,
				boolean allowDirectSupplierShortcut) {

			GeneratedMethod generatedMethod = beanRegistrationCode.getMethods()
					.add("getScopedProxyInstance", method -> {
						method.addJavadoc("Create the scoped proxy bean instance for '$L'.",
								this.registeredBean.getBeanName());
						method.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
						method.returns(ScopedProxyFactoryBean.class);
						method.addParameter(RegisteredBean.class, REGISTERED_BEAN_PARAMETER_NAME);
						method.addStatement("$T factory = new $T()",
								ScopedProxyFactoryBean.class, ScopedProxyFactoryBean.class);
						method.addStatement("factory.setTargetBeanName($S)", this.targetBeanName);
						method.addStatement("factory.setBeanFactory($L.getBeanFactory())",
								REGISTERED_BEAN_PARAMETER_NAME);
						method.addStatement("return factory");
					});
			return CodeBlock.of("$T.of($L)", InstanceSupplier.class,
					generatedMethod.toMethodReference().toCodeBlock());
		}

	}

}
