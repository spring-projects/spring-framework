/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation.beanvalidation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

/**
 * This is the central class for {@code javax.validation} (JSR-303) setup
 * in a Spring application context: It bootstraps a {@code javax.validation.ValidationFactory}
 * and exposes it through the Spring {@link org.springframework.validation.Validator} interface
 * as well as through the JSR-303 {@link javax.validation.Validator} interface and the
 * {@link javax.validation.ValidatorFactory} interface itself.
 *
 * <p>When talking to an instance of this bean through the Spring or JSR-303 Validator interfaces,
 * you'll be talking to the default Validator of the underlying ValidatorFactory. This is very
 * convenient in that you don't have to perform yet another call on the factory, assuming that
 * you will almost always use the default Validator anyway. This can also be injected directly
 * into any target dependency of type {@link org.springframework.validation.Validator}!
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see javax.validation.ValidatorFactory
 * @see javax.validation.Validator
 * @see javax.validation.Validation#buildDefaultValidatorFactory()
 * @see javax.validation.ValidatorFactory#getValidator()
 */
public class LocalValidatorFactoryBean extends SpringValidatorAdapter
		implements ValidatorFactory, ApplicationContextAware, InitializingBean {

	@SuppressWarnings("rawtypes")
	private Class providerClass;

	private MessageInterpolator messageInterpolator;

	private TraversableResolver traversableResolver;

	private ConstraintValidatorFactory constraintValidatorFactory;

	private Resource[] mappingLocations;

	private final Map<String, String> validationPropertyMap = new HashMap<String, String>();

	private ApplicationContext applicationContext;

	private ValidatorFactory validatorFactory;


	/**
	 * Specify the desired provider class, if any.
	 * <p>If not specified, JSR-303's default search mechanism will be used.
	 * @see javax.validation.Validation#byProvider(Class)
	 * @see javax.validation.Validation#byDefaultProvider()
	 */
	@SuppressWarnings("rawtypes")
	public void setProviderClass(Class providerClass) {
		this.providerClass = providerClass;
	}

	/**
	 * Specify a custom MessageInterpolator to use for this ValidatorFactory
	 * and its exposed default Validator.
	 */
	public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
		this.messageInterpolator = messageInterpolator;
	}

	/**
	 * Specify a custom Spring MessageSource for resolving validation messages,
	 * instead of relying on JSR-303's default "ValidationMessages.properties" bundle
	 * in the classpath. This may refer to a Spring context's shared "messageSource" bean,
	 * or to some special MessageSource setup for validation purposes only.
	 * <p><b>NOTE:</b> This feature requires Hibernate Validator 4.1 or higher on the classpath.
	 * You may nevertheless use a different validation provider but Hibernate Validator's
	 * {@link ResourceBundleMessageInterpolator} class must be accessible during configuration.
	 * <p>Specify either this property or {@link #setMessageInterpolator "messageInterpolator"},
	 * not both. If you would like to build a custom MessageInterpolator, consider deriving from
	 * Hibernate Validator's {@link ResourceBundleMessageInterpolator} and passing in a
	 * Spring {@link MessageSourceResourceBundleLocator} when constructing your interpolator.
	 * @see ResourceBundleMessageInterpolator
	 * @see MessageSourceResourceBundleLocator
	 */
	public void setValidationMessageSource(MessageSource messageSource) {
		this.messageInterpolator = HibernateValidatorDelegate.buildMessageInterpolator(messageSource);
	}

	/**
	 * Specify a custom TraversableResolver to use for this ValidatorFactory
	 * and its exposed default Validator.
	 */
	public void setTraversableResolver(TraversableResolver traversableResolver) {
		this.traversableResolver = traversableResolver;
	}

	/**
	 * Specify a custom ConstraintValidatorFactory to use for this ValidatorFactory.
	 * <p>Default is a {@link SpringConstraintValidatorFactory}, delegating to the
	 * containing ApplicationContext for creating autowired ConstraintValidator instances.
	 */
	public void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
		this.constraintValidatorFactory = constraintValidatorFactory;
	}

	/**
	 * Specify resource locations to load XML constraint mapping files from, if any.
	 */
	public void setMappingLocations(Resource[] mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * Specify bean validation properties to be passed to the validation provider.
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
	 * or a "props" element in XML bean definitions.
	 * @see javax.validation.Configuration#addProperty(String, String)
	 */
	public void setValidationProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.validationPropertyMap);
	}

	/**
	 * Specify bean validation properties to be passed to the validation provider as a Map.
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @see javax.validation.Configuration#addProperty(String, String)
	 */
	public void setValidationPropertyMap(Map<String, String> validationProperties) {
		if (validationProperties != null) {
			this.validationPropertyMap.putAll(validationProperties);
		}
	}

	/**
	 * Allow Map access to the bean validation properties to be passed to the validation provider,
	 * with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via "validationPropertyMap[myKey]".
	 */
	public Map<String, String> getValidationPropertyMap() {
		return this.validationPropertyMap;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {
		@SuppressWarnings("rawtypes")
		Configuration configuration = (this.providerClass != null ?
				Validation.byProvider(this.providerClass).configure() :
				Validation.byDefaultProvider().configure());

		MessageInterpolator targetInterpolator = this.messageInterpolator;
		if (targetInterpolator == null) {
			targetInterpolator = configuration.getDefaultMessageInterpolator();
		}
		configuration.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));

		if (this.traversableResolver != null) {
			configuration.traversableResolver(this.traversableResolver);
		}

		ConstraintValidatorFactory targetConstraintValidatorFactory = this.constraintValidatorFactory;
		if (targetConstraintValidatorFactory == null && this.applicationContext != null) {
			targetConstraintValidatorFactory =
					new SpringConstraintValidatorFactory(this.applicationContext.getAutowireCapableBeanFactory());
		}
		if (targetConstraintValidatorFactory != null) {
			configuration.constraintValidatorFactory(targetConstraintValidatorFactory);
		}

		if (this.mappingLocations != null) {
			for (Resource location : this.mappingLocations) {
				try {
					configuration.addMapping(location.getInputStream());
				}
				catch (IOException ex) {
					throw new IllegalStateException("Cannot read mapping resource: " + location);
				}
			}
		}

		for (Map.Entry<String, String> entry : this.validationPropertyMap.entrySet()) {
			configuration.addProperty(entry.getKey(), entry.getValue());
		}

		this.validatorFactory = configuration.buildValidatorFactory();
		setTargetValidator(this.validatorFactory.getValidator());
	}


	public Validator getValidator() {
		return this.validatorFactory.getValidator();
	}

	public ValidatorContext usingContext() {
		return this.validatorFactory.usingContext();
	}

	public MessageInterpolator getMessageInterpolator() {
		return this.validatorFactory.getMessageInterpolator();
	}

	public TraversableResolver getTraversableResolver() {
		return this.validatorFactory.getTraversableResolver();
	}

	public ConstraintValidatorFactory getConstraintValidatorFactory() {
		return this.validatorFactory.getConstraintValidatorFactory();
	}


	/**
	 * Inner class to avoid a hard-coded Hibernate Validator 4.1 dependency.
	 */
	private static class HibernateValidatorDelegate {

		public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
			return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
		}
	}

}
