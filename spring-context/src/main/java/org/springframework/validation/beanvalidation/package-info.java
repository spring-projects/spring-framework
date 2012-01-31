/**
 * Support classes for integrating a JSR-303 Bean Validation provider
 * (such as Hibernate Validator 4.0) into a Spring ApplicationContext
 * and in particular with Spring's data binding and validation APIs.
 *
 * <p>The central class is {@link
 * org.springframework.validation.beanvalidation.LocalValidatorFactoryBean}
 * which defines a shared ValidatorFactory/Validator setup for availability
 * to other Spring components.
 */
package org.springframework.validation.beanvalidation;
