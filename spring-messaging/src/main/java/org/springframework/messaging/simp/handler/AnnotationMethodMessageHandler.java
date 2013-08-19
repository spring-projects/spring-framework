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
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
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
import org.springframework.messaging.handler.annotation.ReplyTo;
import org.springframework.messaging.handler.annotation.support.ExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.annotation.support.MessageBodyMethodArgumentResolver;
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver;
import org.springframework.messaging.handler.method.HandlerMethod;
import org.springframework.messaging.handler.method.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.method.HandlerMethodArgumentResolverComposite;
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

	private final SimpMessageSendingOperations dispatchMessagingTemplate;

	private final SimpMessageSendingOperations webSocketSessionMessagingTemplate;

	private List<String> destinationPrefixes;

	private MessageConverter<?> messageConverter;

	private ApplicationContext applicationContext;

	private Map<MappingInfo, HandlerMethod> messageMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> subscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> unsubscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private final Map<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver>(64);

	private List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();

	private HandlerMethodArgumentResolverComposite argumentResolvers = new HandlerMethodArgumentResolverComposite();

	private HandlerMethodReturnValueHandlerComposite returnValueHandlers = new HandlerMethodReturnValueHandlerComposite();


	/**
	 * @param dispatchMessagingTemplate a messaging template to dispatch messages to for
	 *        further processing, e.g. the use of an {@link ReplyTo} annotation on a
	 *        message handling method, causes a new (broadcast) message to be sent.
	 * @param webSocketSessionChannel the channel to send messages to WebSocket sessions
	 *        on this application server. This is used primarily for processing the return
	 *        values from {@link SubscribeEvent}-annotated methods.
	 */
	public AnnotationMethodMessageHandler(SimpMessageSendingOperations dispatchMessagingTemplate,
			MessageChannel webSocketSessionChannel) {

		Assert.notNull(dispatchMessagingTemplate, "dispatchMessagingTemplate is required");
		Assert.notNull(webSocketSessionChannel, "webSocketSessionChannel is required");
		this.dispatchMessagingTemplate = dispatchMessagingTemplate;
		this.webSocketSessionMessagingTemplate = new SimpMessagingTemplate(webSocketSessionChannel);
	}


	public void setDestinationPrefixes(List<String> destinationPrefixes) {
		this.destinationPrefixes = destinationPrefixes;
	}

	public List<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	public void setMessageConverter(MessageConverter<?> converter) {
		this.messageConverter = converter;
		if (converter != null) {
			((AbstractMessageSendingTemplate<?>) this.webSocketSessionMessagingTemplate).setMessageConverter(converter);
		}
	}

	/**
	 * Sets the custom list of {@code HandlerMethodArgumentResolver}s that will be used as the <em>first</em>
	 * argument resolvers when resolving the values of the mapped methods.
	 */
	public void setCustomArgumentResolvers(List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		Assert.notNull(customArgumentResolvers, "The 'customArgumentResolvers' cannot be null.");

		this.customArgumentResolvers = customArgumentResolvers;
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

		this.argumentResolvers.addResolvers(this.customArgumentResolvers);
		this.argumentResolvers.addResolver(new PrincipalMethodArgumentResolver());
		this.argumentResolvers.addResolver(new MessageMethodArgumentResolver());
		this.argumentResolvers.addResolver(new MessageBodyMethodArgumentResolver(this.messageConverter));

		this.returnValueHandlers.addHandler(new ReplyToMethodReturnValueHandler(this.dispatchMessagingTemplate));
		this.returnValueHandlers.addHandler(new SubscriptionMethodReturnValueHandler(this.webSocketSessionMessagingTemplate));
	}

	protected void initHandlerMethods() {
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

	protected void detectHandlerMethods(Object handler) {

		Class<?> handlerType = (handler instanceof String) ?
				this.applicationContext.getType((String) handler) : handler.getClass();

		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		initHandlerMethods(handler, userType, MessageMapping.class,
				new MessageMappingInfoCreator(), this.messageMethods);

		initHandlerMethods(handler, userType, SubscribeEvent.class,
				new SubscribeMappingInfoCreator(), this.subscribeMethods);

		initHandlerMethods(handler, userType, UnsubscribeEvent.class,
				new UnsubscribeMappingInfoCreator(), this.unsubscribeMethods);
	}

	private <A extends Annotation> void initHandlerMethods(Object handler, Class<?> handlerType,
			final Class<A> annotationType, MappingInfoCreator<A> mappingInfoCreator,
			Map<MappingInfo, HandlerMethod> handlerMethods) {

		Set<Method> messageMethods = HandlerMethodSelector.selectMethods(handlerType, new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				return AnnotationUtils.findAnnotation(method, annotationType) != null;
			}
		});

		for (Method method : messageMethods) {
			A annotation = AnnotationUtils.findAnnotation(method, annotationType);
			HandlerMethod hm = createHandlerMethod(handler, method);
			handlerMethods.put(mappingInfoCreator.create(annotation), hm);
		}
	}

	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
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

	private void handleMessageInternal(final Message<?> message, Map<MappingInfo, HandlerMethod> handlerMethods) {

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
		String destination = headers.getDestination();

		if (!checkDestinationPrefix(destination)) {
			return;
		}

		HandlerMethod match = getHandlerMethod(destination, handlerMethods);
		if (match == null) {
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Processing message: " + message);
		}

		HandlerMethod handlerMethod = match.createWithResolvedBean();

		InvocableHandlerMethod invocableHandlerMethod = new InvocableHandlerMethod(handlerMethod);
		invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
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

	private boolean checkDestinationPrefix(String destination) {
		if ((destination == null) || CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return true;
		}
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return true;
			}
		}
		return false;
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

		private final List<String> destinations;


		public MappingInfo(List<String> destinations) {
			this.destinations = destinations;
		}

		public List<String> getDestinations() {
			return this.destinations;
		}

		@Override
		public String toString() {
			return "MappingInfo [destinations=" + this.destinations + "]";
		}
	}

	private interface MappingInfoCreator<A extends Annotation> {

		MappingInfo create(A annotation);
	}

	private static class MessageMappingInfoCreator implements MappingInfoCreator<MessageMapping> {

		@Override
		public MappingInfo create(MessageMapping annotation) {
			return new MappingInfo(Arrays.asList(annotation.value()));
		}
	}

	private static class SubscribeMappingInfoCreator implements MappingInfoCreator<SubscribeEvent> {

		@Override
		public MappingInfo create(SubscribeEvent annotation) {
			return new MappingInfo(Arrays.asList(annotation.value()));
		}
	}

	private static class UnsubscribeMappingInfoCreator implements MappingInfoCreator<UnsubscribeEvent> {

		@Override
		public MappingInfo create(UnsubscribeEvent annotation) {
			return new MappingInfo(Arrays.asList(annotation.value()));
		}
	}

}
