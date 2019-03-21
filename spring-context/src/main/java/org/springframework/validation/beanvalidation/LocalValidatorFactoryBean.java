/*
 * Copyright 2002-2018 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * This is the central class for {@code javax.validation} (JSR-303) setup in a Spring
 * application context: It bootstraps a {@code javax.validation.ValidationFactory} and
 * exposes it through the Spring {@link org.springframework.validation.Validator} interface
 * as well as through the JSR-303 {@link javax.validation.Validator} interface and the
 * {@link javax.validation.ValidatorFactory} interface itself.
 *
 * <p>When talking to an instance of this bean through the Spring or JSR-303 Validator interfaces,
 * you'll be talking to the default Validator of the underlying ValidatorFactory. This is very
 * convenient in that you don't have to perform yet another call on the factory, assuming that
 * you will almost always use the default Validator anyway. This can also be injected directly
 * into any target dependency of type {@link org.springframework.validation.Validator}!
 *
 * <p><b>As of Spring 4.0, this class supports Bean Validation 1.0 and 1.1, with special support
 * for Hibernate Validator 4.3 and 5.x</b> (see {@link #setValidationMessageSource}).
 *
 * <p>Note that Bean Validation 1.1's {@code #forExecutables} method isn't supported: We do not
 * expect that method to be called by application code; consider {@link MethodValidationInterceptor}
 * instead. If you really need programmatic {@code #forExecutables} access, inject this class as
 * a {@link ValidatorFactory} and call {@link #getValidator()} on it, then {@code #forExecutables}
 * on the returned native {@link Validator} reference instead of directly on this class.
 * Alternatively, call {@code #unwrap(Validator.class)} which will also provide the native object.
 *
 * <p>This class is also being used by Spring's MVC configuration namespace, in case of the
 * {@code javax.validation} API being present but no explicit Validator having been configured.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see javax.validation.ValidatorFactory
 * @see javax.validation.Validator
 * @see javax.validation.Validation#buildDefaultValidatorFactory()
 * @see javax.validation.ValidatorFactory#getValidator()
 */
public class LocalValidatorFactoryBean extends SpringValidatorAdapter
		implements ValidatorFactory, ApplicationContextAware, InitializingBean, DisposableBean {

	// Bean Validation 1.1 close() method available?
	private static final Method closeMethod = ClassUtils.getMethodIfAvailable(ValidatorFactory.class, "close");


	@SuppressWarnings("rawtypes")
	private Class providerClass;

	private ValidationProviderResolver validationProviderResolver;

	private MessageInterpolator messageInterpolator;

	private TraversableResolver traversableResolver;

	private ConstraintValidatorFactory constraintValidatorFactory;

	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

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
	 * Specify a JSR-303 {@link ValidationProviderResolver} for bootstrapping the
	 * provider of choice, as an alternative to {@code META-INF} driven resolution.
	 * @since 4.3
	 */
	public void setValidationProviderResolver(ValidationProviderResolver validationProviderResolver) {
		this.validationProviderResolver = validationProviderResolver;
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
	 * <p><b>NOTE:</b> This feature requires Hibernate Validator 4.3 or higher on the classpath.
	 * You may nevertheless use a different validation provider but Hibernate Validator's
	 * {@link ResourceBundleMessageInterpolator} class must be accessible during configuration.
	 * <p>Specify either this property or {@link #setMessageInterpolator "messageInterpolator"},
	 * not both. If you would like to build a custom MessageInterpolator, consider deriving from
	 * Hibernate Validator's {@link ResourceBundleMessageInterpolator} and passing in a
	 * Spring-based {@code ResourceBundleLocator} when constructing your interpolator.
	 * <p>In order for Hibernate's default validation messages to be resolved still, your
	 * {@link MessageSource} must be configured for optional resolution (usually the default).
	 * In particular, the {@code MessageSource} instance specified here should not apply
	 * {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage
	 * "useCodeAsDefaultMessage"} behavior. Please double-check your setup accordingly.
	 * @see ResourceBundleMessageInterpolator
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
	 * Set the ParameterNameDiscoverer to use for resolving method and constructor
	 * parameter names if needed for message interpolation.
	 * <p>Default is a {@link org.springframework.core.DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Specify resource locations to load XML constraint mapping files from, if any.
	 */
	public void setMappingLocations(Resource... mappingLocations) {
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

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void afterPropertiesSet() {
		Configuration<?> configuration;
		if (this.providerClass != null) {
			ProviderSpecificBootstrap bootstrap = Validation.byProvider(this.providerClass);
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}
		else {
			GenericBootstrap bootstrap = Validation.byDefaultProvider();
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}

		// Try Hibernate Validator 5.2's externalClassLoader(ClassLoader) method
		if (this.applicationContext != null) {
			try {
				Method eclMethod = configuration.getClass().getMethod("externalClassLoader", ClassLoader.class);
				ReflectionUtils.invokeMethod(eclMethod, configuration, this.applicationContext.getClassLoader());
			}
			catch (NoSuchMethodException ex) {
				// Ignore - no Hibernate Validator 5.2+ or similar provider
			}
		}

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

		if (this.parameterNameDiscoverer != null) {
			configureParameterNameProviderIfPossible(configuration);
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

		// Allow for custom post-processing before we actually build the ValidatorFactory.
		postProcessConfiguration(configuration);

		this.validatorFactory = configuration.buildValidatorFactory();
		setTargetValidator(this.validatorFactory.getValidator());
	}

	private void configureParameterNameProviderIfPossible(Configuration<?> configuration) {
		try {
			Class<?> parameterNameProviderClass =
					ClassUtils.forName("javax.validation.ParameterNameProvider", getClass().getClassLoader());
			Method parameterNameProviderMethod =
					Configuration.class.getMethod("parameterNameProvider", parameterNameProviderClass);
			final Object defaultProvider = ReflectionUtils.invokeMethod(
					Configuration.class.getMethod("getDefaultParameterNameProvider"), configuration);
			final ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
			Object parameterNameProvider = Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {parameterNameProviderClass}, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					if (method.getName().equals("getParameterNames")) {
						String[] result = null;
						if (args[0] instanceof Constructor) {
							result = discoverer.getParameterNames((Constructor<?>) args[0]);
						}
						else if (args[0] instanceof Method) {
							result = discoverer.getParameterNames((Method) args[0]);
						}
						if (result != null) {
							return Arrays.asList(result);
						}
						else {
							try {
								return method.invoke(defaultProvider, args);
							}
							catch (InvocationTargetException ex) {
								throw ex.getTargetException();
							}
						}
					}
					else {
						// toString, equals, hashCode
						try {
							return method.invoke(this, args);
						}
						catch (InvocationTargetException ex) {
							throw ex.getTargetException();
						}
					}
				}
			});
			ReflectionUtils.invokeMethod(parameterNameProviderMethod, configuration, parameterNameProvider);

		}
		catch (Throwable ex) {
			// Bean Validation 1.1 API not available - simply not applying the ParameterNameDiscoverer
		}
	}

	/**
	 * Post-process the given Bean Validation configuration,
	 * adding to or overriding any of its settings.
	 * <p>Invoked right before building the {@link ValidatorFactory}.
	 * @param configuration the Configuration object, pre-populated with
	 * settings driven by LocalValidatorFactoryBean's properties
	 */
	protected void postProcessConfiguration(Configuration<?> configuration) {
	}


	@Override
	public Validator getValidator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getValidator();
	}

	@Override
	public ValidatorContext usingContext() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.usingContext();
	}

	@Override
	public MessageInterpolator getMessageInterpolator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getMessageInterpolator();
	}

	@Override
	public TraversableResolver getTraversableResolver() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getTraversableResolver();
	}

	@Override
	public ConstraintValidatorFactory getConstraintValidatorFactory() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getConstraintValidatorFactory();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if (type == null || !ValidatorFactory.class.isAssignableFrom(type)) {
			try {
				return super.unwrap(type);
			}
			catch (ValidationException ex) {
				// ignore - we'll try ValidatorFactory unwrapping next
			}
		}
		try {
			return this.validatorFactory.unwrap(type);
		}
		catch (ValidationException ex) {
			// ignore if just being asked for ValidatorFactory
			if (ValidatorFactory.class == type) {
				return (T) this.validatorFactory;
			}
			throw ex;
		}
	}


	public void close() {
		if (closeMethod != null && this.validatorFactory != null) {
			ReflectionUtils.invokeMethod(closeMethod, this.validatorFactory);
		}
	}

	@Override
	public void destroy() {
		close();
	}


	/**
	 * Inner class to avoid a hard-coded Hibernate Validator dependency.
	 */
	private static class HibernateValidatorDelegate {

		public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
			return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
		}
	}

}
