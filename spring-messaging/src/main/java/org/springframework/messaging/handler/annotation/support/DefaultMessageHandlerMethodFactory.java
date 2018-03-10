/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.validation.Validator;

/**
 * The default {@link MessageHandlerMethodFactory} implementation creating an
 * {@link InvocableHandlerMethod} with the necessary
 * {@link HandlerMethodArgumentResolver} instances to detect and process
 * most of the use cases defined by
 * {@link org.springframework.messaging.handler.annotation.MessageMapping MessageMapping}.
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
 * @author Juergen Hoeller
 * @since 4.1
 * @see #setConversionService
 * @see #setValidator
 * @see #setCustomArgumentResolvers
 */
public class DefaultMessageHandlerMethodFactory
		implements MessageHandlerMethodFactory, BeanFactoryAware, InitializingBean {

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private MessageConverter messageConverter;

	private Validator validator;

	private List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private BeanFactory beanFactory;


	/**
	 * Set the {@link ConversionService} to use to convert the original
	 * message payload or headers.
	 * @see HeaderMethodArgumentResolver
	 * @see GenericMessageConverter
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Set the {@link MessageConverter} to use. By default a {@link GenericMessageConverter}
	 * is used.
	 * @see GenericMessageConverter
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
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
	 * Set the list of custom {@code HandlerMethodArgumentResolver}s that will be used
	 * after resolvers for supported argument type.
	 * @param customArgumentResolvers the list of resolvers (never {@code null})
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		this.customArgumentResolvers = customArgumentResolvers;
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

	/**
	 * A {@link BeanFactory} only needs to be available for placeholder resolution
	 * in handler method arguments; it's optional otherwise.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.messageConverter == null) {
			this.messageConverter = new GenericMessageConverter(this.conversionService);
		}
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}
	}


	@Override
	public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(bean, method);
		handlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);
		return handlerMethod;
	}

	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		ConfigurableBeanFactory cbf = (this.beanFactory instanceof ConfigurableBeanFactory ?
				(ConfigurableBeanFactory) this.beanFactory : null);

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, cbf));
		resolvers.add(new HeadersMethodArgumentResolver());

		// Type-based argument resolution
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

		if (this.customArgumentResolvers != null) {
			resolvers.addAll(this.customArgumentResolvers);
		}
		resolvers.add(new PayloadArgumentResolver(this.messageConverter, this.validator));

		return resolvers;
	}

}
