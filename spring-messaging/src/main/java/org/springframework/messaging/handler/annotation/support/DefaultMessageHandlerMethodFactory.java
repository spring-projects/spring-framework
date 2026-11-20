/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

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
import org.springframework.util.Assert;
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

	private @Nullable MessageConverter messageConverter;

	private @Nullable Validator validator;

	private @Nullable List<HandlerMethodArgumentResolver> customArgumentResolvers;

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private @Nullable BeanFactory beanFactory;


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
	 * Set the Validator instance used for validating {@code @Payload} arguments.
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadMethodArgumentResolver
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
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
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
		ConfigurableBeanFactory beanFactory = (this.beanFactory instanceof ConfigurableBeanFactory cbf ?
				cbf : null);

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());

		// Type-based argument resolution
		resolvers.add(new MessageMethodArgumentResolver(this.messageConverter));

		if (this.customArgumentResolvers != null) {
			resolvers.addAll(this.customArgumentResolvers);
		}

		Assert.notNull(this.messageConverter, "MessageConverter not configured");
		resolvers.add(new PayloadMethodArgumentResolver(this.messageConverter, this.validator));

		return resolvers;
	}

}
