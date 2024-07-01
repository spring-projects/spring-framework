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

package org.springframework.validation.beanvalidation;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;

/**
 * A convenient {@link BeanPostProcessor} implementation that delegates to a
 * JSR-303 provider for performing method-level validation on annotated methods.
 *
 * <p>Applicable methods have JSR-303 constraint annotations on their parameters
 * and/or on their return value (in the latter case specified at the method level,
 * typically as inline annotation), e.g.:
 *
 * <pre class="code">
 * public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)
 * </pre>
 *
 * <p>In case of validation errors, the interceptor can raise
 * {@link ConstraintViolationException}, or adapt the violations to
 * {@link MethodValidationResult} and raise {@link MethodValidationException}.
 *
 * <p>Target classes with such annotated methods need to be annotated with Spring's
 * {@link Validated} annotation at the type level, for their methods to be searched for
 * inline constraint annotations. Validation groups can be specified through {@code @Validated}
 * as well. By default, JSR-303 will validate against its default group only.
 *
 * <p>This functionality requires a Bean Validation 1.1+ provider.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationInterceptor
 * @see jakarta.validation.executable.ExecutableValidator
 */
@SuppressWarnings("serial")
public class MethodValidationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements InitializingBean {

	private Class<? extends Annotation> validatedAnnotationType = Validated.class;

	private Supplier<Validator> validator = SingletonSupplier.of(() ->
			Validation.buildDefaultValidatorFactory().getValidator());

	private boolean adaptConstraintViolations;


	/**
	 * Set the 'validated' annotation type.
	 * The default validated annotation type is the {@link Validated} annotation.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a class is supposed
	 * to be validated in the sense of applying method validation.
	 * @param validatedAnnotationType the desired annotation type
	 */
	public void setValidatedAnnotationType(Class<? extends Annotation> validatedAnnotationType) {
		Assert.notNull(validatedAnnotationType, "'validatedAnnotationType' must not be null");
		this.validatedAnnotationType = validatedAnnotationType;
	}

	/**
	 * Set the JSR-303 ValidatorFactory to delegate to for validating methods,
	 * using its default Validator.
	 * <p>Default is the default ValidatorFactory's default Validator.
	 * @see jakarta.validation.ValidatorFactory#getValidator()
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validator = SingletonSupplier.of(validatorFactory::getValidator);
	}

	/**
	 * Set the JSR-303 Validator to delegate to for validating methods.
	 * <p>Default is the default ValidatorFactory's default Validator.
	 */
	public void setValidator(Validator validator) {
		this.validator = () -> validator;
	}

	/**
	 * Set a lazily initialized Validator to delegate to for validating methods.
	 * @since 6.0
	 * @see #setValidator
	 */
	public void setValidatorProvider(ObjectProvider<Validator> validatorProvider) {
		this.validator = validatorProvider::getObject;
	}

	/**
	 * Whether to adapt {@link ConstraintViolation}s to {@link MethodValidationResult}.
	 * <p>By default {@code false} in which case
	 * {@link jakarta.validation.ConstraintViolationException} is raised in case of
	 * violations. When set to {@code true}, {@link MethodValidationException}
	 * is raised instead with the method validation results.
	 * @since 6.1
	 */
	public void setAdaptConstraintViolations(boolean adaptViolations) {
		this.adaptConstraintViolations = adaptViolations;
	}


	@Override
	public void afterPropertiesSet() {
		Pointcut pointcut = new AnnotationMatchingPointcut(this.validatedAnnotationType, true);
		this.advisor = new DefaultPointcutAdvisor(pointcut, createMethodValidationAdvice(this.validator));
	}

	/**
	 * Create AOP advice for method validation purposes, to be applied
	 * with a pointcut for the specified 'validated' annotation.
	 * @param validator a Supplier for the Validator to use
	 * @return the interceptor to use (typically, but not necessarily,
	 * a {@link MethodValidationInterceptor} or subclass thereof)
	 * @since 6.0
	 */
	protected Advice createMethodValidationAdvice(Supplier<Validator> validator) {
		return new MethodValidationInterceptor(validator, this.adaptConstraintViolations);
	}

}
