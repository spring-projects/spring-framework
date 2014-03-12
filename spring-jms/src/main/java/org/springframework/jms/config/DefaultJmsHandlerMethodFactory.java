/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * The default {@link JmsHandlerMethodFactory} implementation creating an
 * {@link InvocableHandlerMethod} with the necessary
 * {@link HandlerMethodArgumentResolver} instances to detect and process
 * all the use cases defined by {@link org.springframework.jms.annotation.JmsListener
 * JmsListener}.
 *
 * <p>Extra method argument resolvers can be added to customize the method
 * signature that can be handled.
 *
 * <p>By default, the validation process redirects to a no-op implementation, see
 * {@link #setValidator(Validator)} to customize it. The {@link ConversionService}
 * can be customized in a similar manner to tune how the message payload
 * can be converted
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see #setCustomArgumentResolvers(java.util.List)
 * @see #setValidator(Validator)
 * @see #setConversionService(ConversionService)
 */
public class DefaultJmsHandlerMethodFactory
		implements JmsHandlerMethodFactory, InitializingBean, ApplicationContextAware {

	private ApplicationContext applicationContext;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private MessageConverter messageConverter;

	private Validator validator = new NoOpValidator();

	private List<HandlerMethodArgumentResolver> customArgumentResolvers
			= new ArrayList<HandlerMethodArgumentResolver>();

	private HandlerMethodArgumentResolverComposite argumentResolvers
			= new HandlerMethodArgumentResolverComposite();


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Set the {@link ConversionService} to use to convert the original
	 * message payload or headers.
	 *
	 * @see HeaderMethodArgumentResolver
	 * @see GenericMessageConverter
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	protected ConversionService getConversionService() {
		return conversionService;
	}

	/**
	 * Set the {@link MessageConverter} to use. By default a {@link GenericMessageConverter}
	 * is used.
	 *
	 * @see GenericMessageConverter
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	protected MessageConverter getMessageConverter() {
		return messageConverter;
	}

	/**
	 * Set the Validator instance used for validating @Payload arguments
	 * @see org.springframework.validation.annotation.Validated
	 * @see org.springframework.messaging.handler.annotation.support.PayloadArgumentResolver
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * The configured Validator instance
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the list of custom {@code HandlerMethodArgumentResolver}s that will be used
	 * after resolvers for supported argument type.
	 * @param customArgumentResolvers the list of resolvers; never {@code null}.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		Assert.notNull(customArgumentResolvers, "The 'customArgumentResolvers' cannot be null.");
		this.customArgumentResolvers = customArgumentResolvers;
	}

	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return customArgumentResolvers;
	}

	/**
	 * Configure the complete list of supported argument types effectively overriding
	 * the ones configured by default. This is an advanced option. For most use cases
	 * it should be sufficient to use {@link #setCustomArgumentResolvers(java.util.List)}.
	 */
	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers.clear();
			return;
		}
		this.argumentResolvers.addResolvers(argumentResolvers);
	}

	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers.getResolvers();
	}

	@Override
	public void afterPropertiesSet() {
		if (messageConverter == null) {
			messageConverter = new GenericMessageConverter(getConversionService());
		}
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}
	}

	@Override
	public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(bean, method);
		handlerMethod.setMessageMethodArgumentResolvers(argumentResolvers);
		return handlerMethod;
	}

	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		ConfigurableBeanFactory beanFactory =
				(ClassUtils.isAssignableValue(ConfigurableApplicationContext.class, applicationContext)) ?
						((ConfigurableApplicationContext) applicationContext).getBeanFactory() : null;

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(getConversionService(), beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());

		// Type-based argument resolution
		resolvers.add(new MessageMethodArgumentResolver());

		resolvers.addAll(getCustomArgumentResolvers());
		resolvers.add(new PayloadArgumentResolver(getMessageConverter(), getValidator()));

		return resolvers;
	}


	private static final class NoOpValidator implements Validator {
		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		public void validate(Object target, Errors errors) {
		}
	}

}
