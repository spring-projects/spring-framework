/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.simp.annotation.support;

import java.lang.reflect.Method;
import java.util.*;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.*;
import org.springframework.messaging.handler.annotation.support.DestinationVariableMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.SimpMessageMappingInfo;
import org.springframework.messaging.simp.SimpMessageTypeMessageCondition;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * A handler for messages delegating to
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping @SubscribeMapping} and
 * {@link MessageMapping @MessageMapping} annotated methods.
 * <p>
 * Supports Ant-style path patterns with template variables.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.0
 */
public class SimpAnnotationMethodMessageHandler extends AbstractMethodMessageHandler<SimpMessageMappingInfo>
		implements SmartLifecycle {

	private final SubscribableChannel clientInboundChannel;

	private final SimpMessageSendingOperations clientMessagingTemplate;

	private final SimpMessageSendingOperations brokerTemplate;

	private MessageConverter messageConverter;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	private PathMatcher pathMatcher = new AntPathMatcher();

	private Validator validator;

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


	/**
	 * Create an instance of SimpAnnotationMethodMessageHandler with the given
	 * message channels and broker messaging template.
	 *
	 * @param clientInboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param clientOutboundChannel the channel for messages to clients (e.g. WebSocket clients)
	 * @param brokerTemplate a messaging template to send application messages to the broker
	 */
	public SimpAnnotationMethodMessageHandler(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, SimpMessageSendingOperations brokerTemplate) {

		Assert.notNull(clientInboundChannel, "clientInboundChannel must not be null");
		Assert.notNull(clientOutboundChannel, "clientOutboundChannel must not be null");
		Assert.notNull(brokerTemplate, "brokerTemplate must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.clientMessagingTemplate = new SimpMessagingTemplate(clientOutboundChannel);
		this.brokerTemplate = brokerTemplate;

		Collection<MessageConverter> converters = new ArrayList<MessageConverter>();
		converters.add(new StringMessageConverter());
		converters.add(new ByteArrayMessageConverter());
		this.messageConverter = new CompositeMessageConverter(converters);
	}

	/**
	 * Configure a {@link MessageConverter} to use to convert the payload of a message
	 * from serialize form with a specific MIME type to an Object matching the target
	 * method parameter. The converter is also used when sending message to the message
	 * broker.
	 * @see CompositeMessageConverter
	 */
	public void setMessageConverter(MessageConverter converter) {
		this.messageConverter = converter;
		if (converter != null) {
			((AbstractMessageSendingTemplate<?>) this.clientMessagingTemplate).setMessageConverter(converter);
		}
	}

	/**
	 * Return the configured {@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * Configure a {@link ConversionService} to use when resolving method arguments, for
	 * example message header values.
	 * <p>By default an instance of {@link DefaultFormattingConversionService} is used.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * The configured {@link ConversionService}.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Set the PathMatcher implementation to use for matching destinations
	 * against configured destination patterns.
	 * <p>By default AntPathMatcher is used
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the PathMatcher implementation to use for matching destinations
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * The configured Validator instance
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the Validator instance used for validating @Payload arguments
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadArgumentResolver
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.clientInboundChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientInboundChannel.unsubscribe(this);
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {

		ConfigurableBeanFactory beanFactory =
				(ClassUtils.isAssignableValue(ConfigurableApplicationContext.class, getApplicationContext())) ?
						((ConfigurableApplicationContext) getApplicationContext()).getBeanFactory() : null;

		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<HandlerMethodArgumentResolver>();

		// Annotation-based argument resolution
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());
		resolvers.add(new DestinationVariableMethodArgumentResolver(this.conversionService));

		// Type-based argument resolution
		resolvers.add(new PrincipalMethodArgumentResolver());
		resolvers.add(new MessageMethodArgumentResolver());

		resolvers.addAll(getCustomArgumentResolvers());
		resolvers.add(new PayloadArgumentResolver(this.messageConverter,
				this.validator != null ? this.validator : new NoopValidator()));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {

		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<HandlerMethodReturnValueHandler>();

		// Annotation-based return value types
		handlers.add(new SendToMethodReturnValueHandler(this.brokerTemplate, true));
		handlers.add(new SubscriptionMethodReturnValueHandler(this.clientMessagingTemplate));

		// custom return value types
		handlers.addAll(getCustomReturnValueHandlers());

		// catch-all
		handlers.add(new SendToMethodReturnValueHandler(this.brokerTemplate, false));

		return handlers;
	}


	@Override
	protected boolean isHandler(Class<?> beanType) {
		return (AnnotationUtils.findAnnotation(beanType, Controller.class) != null);
	}

	@Override
	protected SimpMessageMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {

		MessageMapping typeAnnot = AnnotationUtils.findAnnotation(handlerType, MessageMapping.class);
		MessageMapping messageAnnot = AnnotationUtils.findAnnotation(method, MessageMapping.class);
		if (messageAnnot != null) {
			SimpMessageMappingInfo result = createMessageMappingCondition(messageAnnot);
			if (typeAnnot != null) {
				result = createMessageMappingCondition(typeAnnot).combine(result);
			}
			return result;
		}

		SubscribeMapping subsribeAnnot = AnnotationUtils.findAnnotation(method, SubscribeMapping.class);
		if (subsribeAnnot != null) {
			SimpMessageMappingInfo result = createSubscribeCondition(subsribeAnnot);
			if (typeAnnot != null) {
				result = createMessageMappingCondition(typeAnnot).combine(result);
			}
			return result;
		}

		return null;
	}

	private SimpMessageMappingInfo createMessageMappingCondition(MessageMapping annotation) {
		return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.MESSAGE,
				new DestinationPatternsMessageCondition(annotation.value()));
	}

	private SimpMessageMappingInfo createSubscribeCondition(SubscribeMapping annotation) {
		return new SimpMessageMappingInfo(SimpMessageTypeMessageCondition.SUBSCRIBE,
				new DestinationPatternsMessageCondition(annotation.value()));
	}

	@Override
	protected Set<String> getDirectLookupDestinations(SimpMessageMappingInfo mapping) {
		Set<String> result = new LinkedHashSet<String>();
		for (String s : mapping.getDestinationConditions().getPatterns()) {
			if (!this.pathMatcher.isPattern(s)) {
				result.add(s);
			}
		}
		return result;
	}

	@Override
	protected String getDestination(Message<?> message) {
		return (String) message.getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER);
	}

	@Override
	protected SimpMessageMappingInfo getMatchingMapping(SimpMessageMappingInfo mapping, Message<?> message) {
		return mapping.getMatchingCondition(message);

	}

	@Override
	protected Comparator<SimpMessageMappingInfo> getMappingComparator(final Message<?> message) {
		return new Comparator<SimpMessageMappingInfo>() {
			@Override
			public int compare(SimpMessageMappingInfo info1, SimpMessageMappingInfo info2) {
				return info1.compareTo(info2, message);
			}
		};
	}

	@Override
	protected void handleMatch(SimpMessageMappingInfo mapping, HandlerMethod handlerMethod,
			String lookupDestination, Message<?> message) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);

		String matchedPattern = mapping.getDestinationConditions().getPatterns().iterator().next();
		Map<String, String> vars = getPathMatcher().extractUriTemplateVariables(matchedPattern, lookupDestination);

		headers.setDestination(lookupDestination);
		headers.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
		message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();

		super.handleMatch(mapping, handlerMethod, lookupDestination, message);
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

	private static final class NoopValidator implements Validator {
		@Override
		public boolean supports(Class<?> clazz) {
			return false;
		}
		@Override
		public void validate(Object target, Errors errors) {
		}
	};

}
