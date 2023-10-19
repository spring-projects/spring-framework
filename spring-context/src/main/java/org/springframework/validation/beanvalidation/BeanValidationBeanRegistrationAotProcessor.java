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

package org.springframework.validation.beanvalidation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.NoProviderFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ConstructorDescriptor;
import jakarta.validation.metadata.MethodDescriptor;
import jakarta.validation.metadata.MethodType;
import jakarta.validation.metadata.ParameterDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.KotlinDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * AOT {@code BeanRegistrationAotProcessor} that adds additional hints
 * required for {@link ConstraintValidator}s.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 6.0.5
 */
class BeanValidationBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private static final boolean beanValidationPresent = ClassUtils.isPresent(
			"jakarta.validation.Validation", BeanValidationBeanRegistrationAotProcessor.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(BeanValidationBeanRegistrationAotProcessor.class);


	@Override
	@Nullable
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (beanValidationPresent) {
			return BeanValidationDelegate.processAheadOfTime(registeredBean);
		}
		return null;
	}


	/**
	 * Inner class to avoid a hard dependency on the Bean Validation API at runtime.
	 */
	private static class BeanValidationDelegate {

		@Nullable
		private static final Validator validator = getValidatorIfAvailable();

		@Nullable
		private static Validator getValidatorIfAvailable() {
			try {
				return Validation.buildDefaultValidatorFactory().getValidator();
			}
			catch (NoProviderFoundException ex) {
				logger.info("No Bean Validation provider available - skipping validation constraint hint inference");
				return null;
			}
		}

		@Nullable
		public static BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
			if (validator == null) {
				return null;
			}

			BeanDescriptor descriptor;
			try {
				descriptor = validator.getConstraintsForClass(registeredBean.getBeanClass());
			}
			catch (RuntimeException ex) {
				if (KotlinDetector.isKotlinType(registeredBean.getBeanClass()) && ex instanceof ArrayIndexOutOfBoundsException) {
					// See https://hibernate.atlassian.net/browse/HV-1796 and https://youtrack.jetbrains.com/issue/KT-40857
					logger.warn("Skipping validation constraint hint inference for bean " + registeredBean.getBeanName() +
							" due to an ArrayIndexOutOfBoundsException at validator level");
				}
				else if (ex instanceof TypeNotPresentException) {
					logger.debug("Skipping validation constraint hint inference for bean " +
							registeredBean.getBeanName() + " due to a TypeNotPresentException at validator level: " + ex.getMessage());
				}
				else {
					logger.warn("Skipping validation constraint hint inference for bean " +
							registeredBean.getBeanName(), ex);
				}
				return null;
			}

			Set<ConstraintDescriptor<?>> constraintDescriptors = new HashSet<>();
			for (MethodDescriptor methodDescriptor : descriptor.getConstrainedMethods(MethodType.NON_GETTER, MethodType.GETTER)) {
				for (ParameterDescriptor parameterDescriptor : methodDescriptor.getParameterDescriptors()) {
					constraintDescriptors.addAll(parameterDescriptor.getConstraintDescriptors());
				}
			}
			for (ConstructorDescriptor constructorDescriptor : descriptor.getConstrainedConstructors()) {
				for (ParameterDescriptor parameterDescriptor : constructorDescriptor.getParameterDescriptors()) {
					constraintDescriptors.addAll(parameterDescriptor.getConstraintDescriptors());
				}
			}
			for (PropertyDescriptor propertyDescriptor : descriptor.getConstrainedProperties()) {
				constraintDescriptors.addAll(propertyDescriptor.getConstraintDescriptors());
			}
			if (!constraintDescriptors.isEmpty()) {
				return new AotContribution(constraintDescriptors);
			}
			return null;
		}
	}


	private static class AotContribution implements BeanRegistrationAotContribution {

		private final Collection<ConstraintDescriptor<?>> constraintDescriptors;

		public AotContribution(Collection<ConstraintDescriptor<?>> constraintDescriptors) {
			this.constraintDescriptors = constraintDescriptors;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			for (ConstraintDescriptor<?> constraintDescriptor : this.constraintDescriptors) {
				for (Class<?> constraintValidatorClass : constraintDescriptor.getConstraintValidatorClasses()) {
					generationContext.getRuntimeHints().reflection().registerType(constraintValidatorClass,
							MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
				}
			}
		}
	}

}
