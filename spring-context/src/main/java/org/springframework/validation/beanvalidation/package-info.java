/**
 * Support classes for integrating a JSR-303 Bean Validation provider
 * (such as Hibernate Validator) into a Spring ApplicationContext
 * and in particular with Spring's data binding and validation APIs.
 *
 * <p>The central class is {@link
 * org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}
 * which defines a shared ValidatorFactory/Validator setup for availability
 * to other Spring components.
 */
@NonNullApi
@NonNullFields
package org.springframework.validation.beanvalidation;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;