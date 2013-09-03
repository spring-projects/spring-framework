/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.simp.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.ExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.MessageBodyMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.method.HandlerMethod;
import org.springframework.messaging.handler.method.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.method.HandlerMethodArgumentResolverComposite;
import org.springframework.messaging.handler.method.HandlerMethodReturnValueHandler;
import org.springframework.messaging.handler.method.HandlerMethodReturnValueHandlerComposite;
import org.springframework.messaging.handler.method.HandlerMethodSelector;
import org.springframework.messaging.handler.method.InvocableHandlerMethod;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeEvent;
import org.springframework.messaging.simp.annotation.UnsubscribeEvent;
import org.springframework.messaging.simp.annotation.support.PrincipalMethodArgumentResolver;
import org.springframework.messaging.simp.annotation.support.ReplyToMethodReturnValueHandler;
import org.springframework.messaging.simp.annotation.support.SubscriptionMethodReturnValueHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotationMethodMessageHandler implements MessageHandler, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(AnnotationMethodMessageHandler.class);

	private final SimpMessageSendingOperations brokerTemplate;

	private final SimpMessageSendingOperations webSocketResponseTemplate;

	private Collection<String> destinationPrefixes = new ArrayList<String>();

	private MessageConverter<?> messageConverter;

	private ApplicationContext applicationContext;

	private Map<MappingInfo, HandlerMethod> messageMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> subscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> unsubscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver>(64);

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();

	private List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();

	private HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();


	/**
	 * @param brokerTemplate a messaging template to sending messages to the broker
	 * @param webSocketResponseChannel the channel for messages to WebSocket clients
	 */
	public AnnotationMethodMessageHandler(SimpMessageSendingOperations brokerTemplate,
			MessageChannel webSocketResponseChannel) {

		Assert.notNull(brokerTemplate, "brokerTemplate is required");
		Assert.notNull(webSocketResponseChannel, "webSocketReplyChannel is required");
		this.brokerTemplate = brokerTemplate;
		this.webSocketResponseTemplate = new SimpMessagingTemplate(webSocketResponseChannel);
	}

	/**
	 * Configure one or more prefixes to filter destinations targeting annotated
	 * application methods. For example destinations prefixed with "/app" may be processed
	 * by annotated application methods while other destinations may target the message
	 * broker (e.g. "/topic", "/queue").
	 * <p>
	 * When messages are processed, the matching prefix is removed from the destination in
	 * order to form the lookup path. This means annotations should not contain the
	 * destination prefix.
	 * <p>
	 * Prefixes that do not have a trailing slash will have one automatically appended.
	 */
	public void setDestinationPrefixes(Collection<String> destinationPrefixes) {
		this.destinationPrefixes.clear();
		if (destinationPrefixes != null) {
			for (String prefix : destinationPrefixes) {
				prefix = prefix.trim();
				if (!prefix.endsWith("/")) {
					prefix += "/";
				}
				this.destinationPrefixes.add(prefix);
			}
		}
	}

	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	public void setMessageConverter(MessageConverter<?> converter) {
		this.messageConverter = converter;
		if (converter != null) {
			((AbstractMessageSendingTemplate<?>) this.webSocketResponseTemplate).setMessageConverter(converter);
		}
	}

	/**
	 * Sets the list of custom {@code HandlerMethodArgumentResolver}s that will be used
	 * after resolvers for supported argument type.
	 *
	 * @param customArgumentResolvers the list of resolvers; never {@code null}.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		Assert.notNull(customArgumentResolvers, "The 'customArgumentResolvers' cannot be null.");
		this.customArgumentResolvers = customArgumentResolvers;
	}

	/**
	 * Set the list of custom {@code HandlerMethodReturnValueHandler}s that will be used
	 * after return value handlers for known types.
	 *
	 * @param customReturnValueHandlers the list of custom return value handlers, never {@code null}.
	 */
	public void setCustomReturnValueHandlers(List<HandlerMethodReturnValueHandler> customReturnValueHandlers) {
		Assert.notNull(customReturnValueHandlers, "The 'customReturnValueHandlers' cannot be null.");
		this.customReturnValueHandlers = customReturnValueHandlers;
	}

	public MessageConverter<?> getMessageConverter() {
		return this.messageConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {

		initHandlerMethods();

		// Type-based argument resolution
		this.argumentResolvers.addResolver(new PrincipalMethodArgumentResolver());
		this.argumentResolvers.addResolver(new MessageMethodArgumentResolver());

		// custom arguments
		this.argumentResolvers.addResolvers(this.customArgumentResolvers);

		// catch-all argument resolver
		this.argumentResolvers.addResolver(new MessageBodyMethodArgumentResolver(this.messageConverter));

		// Annotation-based return value types
		this.returnValueHandlers.addHandler(new ReplyToMethodReturnValueHandler(this.brokerTemplate, true));
		this.returnValueHandlers.addHandler(new SubscriptionMethodReturnValueHandler(this.webSocketResponseTemplate));

		// custom return value types
		this.returnValueHandlers.addHandlers(this.customReturnValueHandlers);

		// catch-all
		this.returnValueHandlers.addHandler(new ReplyToMethodReturnValueHandler(this.brokerTemplate, false));
	}

	protected final void initHandlerMethods() {
		String[] beanNames = this.applicationContext.getBeanNamesForType(Object.class);
		for (String beanName : beanNames) {
			if (isHandler(this.applicationContext.getType(beanName))){
				detectHandlerMethods(beanName);
			}
		}
	}

	protected boolean isHandler(Class<?> beanType) {
		return (AnnotationUtils.findAnnotation(beanType, Controller.class) != null);
	}

	protected final void detectHandlerMethods(Object handler) {

		Class<?> handlerType = (handler instanceof String) ?
				this.applicationContext.getType((String) handler) : handler.getClass();

		handlerType = ClassUtils.getUserClass(handlerType);

		initHandlerMethods(handler, handlerType, MessageMapping.class, this.messageMethods);
		initHandlerMethods(handler, handlerType, SubscribeEvent.class, this.subscribeMethods);
		initHandlerMethods(handler, handlerType, UnsubscribeEvent.class, this.unsubscribeMethods);
	}

	private <A extends Annotation> void initHandlerMethods(Object handler, Class<?> handlerType,
			final Class<A> annotationType, Map<MappingInfo, HandlerMethod> handlerMethods) {

		Set<Method> methods = HandlerMethodSelector.selectMethods(handlerType, new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				return AnnotationUtils.findAnnotation(method, annotationType) != null;
			}
		});

		for (Method method : methods) {
			A annotation = AnnotationUtils.findAnnotation(method, annotationType);
			String[] destinations = (String[]) AnnotationUtils.getValue(annotation);
			MappingInfo mapping = new MappingInfo(destinations);

			HandlerMethod newHandlerMethod = createHandlerMethod(handler, method);
			HandlerMethod oldHandlerMethod = handlerMethods.get(mapping);
			if (oldHandlerMethod != null && !oldHandlerMethod.equals(newHandlerMethod)) {
				throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + newHandlerMethod.getBean()
						+ "' bean method \n" + newHandlerMethod + "\nto " + mapping + ": There is already '"
						+ oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
			}
			handlerMethods.put(mapping, newHandlerMethod);
			if (logger.isInfoEnabled()) {
				logger.info("Mapped \"@" + annotationType.getSimpleName()
						+ " " + mapping + "\" onto " + newHandlerMethod);
			}
		}
	}

	private HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName, this.applicationContext, method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		SimpMessageType messageType = headers.getMessageType();

		if (SimpMessageType.MESSAGE.equals(messageType)) {
			handleMessageInternal(message, this.messageMethods);
		}
		else if (SimpMessageType.SUBSCRIBE.equals(messageType)) {
			handleMessageInternal(message, this.subscribeMethods);
		}
		else if (SimpMessageType.UNSUBSCRIBE.equals(messageType)) {
			handleMessageInternal(message, this.unsubscribeMethods);
		}
	}

	private void handleMessageInternal(Message<?> message, Map<MappingInfo, HandlerMethod> handlerMethods) {

		if (logger.isTraceEnabled()) {
			logger.trace("Message " + message);
		}

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		String lookupPath = getLookupPath(headers.getDestination());
		if (lookupPath == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Ignoring message with destination " + headers.getDestination());
			}
			return;
		}

		HandlerMethod match = getHandlerMethod(lookupPath, handlerMethods);
		if (match == null) {
			if (logger.isTraceEnabled()) {
				logger.trace("No matching method, lookup path " + lookupPath);
			}
			return;
		}

		HandlerMethod handlerMethod = match.createWithResolvedBean();

		InvocableHandlerMethod invocableHandlerMethod = new InvocableHandlerMethod(handlerMethod);
		invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
			headers.setDestination(lookupPath);
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();

			Object returnValue = invocableHandlerMethod.invoke(message);

			MethodParameter returnType = handlerMethod.getReturnType();
			if (void.class.equals(returnType.getParameterType())) {
				return;
			}
			this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
		}
		catch (Exception ex) {
			invokeExceptionHandler(message, handlerMethod, ex);
		}
		catch (Throwable ex) {
			// TODO
			ex.printStackTrace();
		}
	}

	private String getLookupPath(String destination) {
		if (destination != null) {
			if (CollectionUtils.isEmpty(this.destinationPrefixes)) {
				return destination;
			}
			for (String prefix : this.destinationPrefixes) {
				if (destination.startsWith(prefix)) {
					return destination.substring(prefix.length() - 1);
				}
			}
		}
		return null;
	}

	private void invokeExceptionHandler(Message<?> message, HandlerMethod handlerMethod, Exception ex) {

		InvocableHandlerMethod exceptionHandlerMethod;
		Class<?> beanType = handlerMethod.getBeanType();
		ExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(beanType);
		if (resolver == null) {
			resolver = new ExceptionHandlerMethodResolver(beanType);
			this.exceptionHandlerCache.put(beanType, resolver);
		}

		Method method = resolver.resolveMethod(ex);
		if (method == null) {
			logger.error("Unhandled exception", ex);
			return;
		}

		exceptionHandlerMethod = new InvocableHandlerMethod(handlerMethod.getBean(), method);
		exceptionHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
			Object returnValue = exceptionHandlerMethod.invoke(message, ex);

			MethodParameter returnType = exceptionHandlerMethod.getReturnType();
			if (void.class.equals(returnType.getParameterType())) {
				return;
			}
			this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
		}
		catch (Throwable t) {
			logger.error("Error while handling exception", t);
			return;
		}
	}

	protected HandlerMethod getHandlerMethod(String destination, Map<MappingInfo, HandlerMethod> handlerMethods) {
		for (MappingInfo key : handlerMethods.keySet()) {
			for (String mappingDestination : key.getDestinations()) {
				if (destination.equals(mappingDestination)) {
					return handlerMethods.get(key);
				}
			}
		}
		return null;
	}


	private static class MappingInfo {

		private final String[] destinations;


		public MappingInfo(String[] destinations) {
			Assert.notNull(destinations, "No destinations");
			this.destinations = destinations;
		}

		public String[] getDestinations() {
			return this.destinations;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.destinations);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o != null && getClass().equals(o.getClass())) {
				MappingInfo other = (MappingInfo) o;
				return Arrays.equals(destinations, other.getDestinations());
			}
			return false;
		}

		@Override
		public String toString() {
			return "[destinations=" + Arrays.toString(this.destinations) + "]";
		}
	}

}
