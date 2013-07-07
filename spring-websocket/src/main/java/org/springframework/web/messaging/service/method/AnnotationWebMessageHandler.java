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

package org.springframework.web.messaging.service.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.annotation.SubscribeEvent;
import org.springframework.web.messaging.annotation.UnsubscribeEvent;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.service.AbstractWebMessageHandler;
import org.springframework.web.messaging.support.MessageHolder;
import org.springframework.web.messaging.support.WebMessageHeaderAccesssor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotationWebMessageHandler extends AbstractWebMessageHandler
		implements ApplicationContextAware, InitializingBean {

	private final MessageChannel inboundChannel;

	private final MessageChannel outboundChannel;

	private List<MessageConverter> messageConverters;

	private ApplicationContext applicationContext;

	private Map<MappingInfo, HandlerMethod> messageMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> subscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> unsubscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private final Map<Class<?>, MessageExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<Class<?>, MessageExceptionHandlerMethodResolver>(64);

	private ArgumentResolverComposite argumentResolvers = new ArgumentResolverComposite();

	private ReturnValueHandlerComposite returnValueHandlers = new ReturnValueHandlerComposite();


	/**
	 * @param inboundChannel a channel for processing incoming messages from clients
	 * @param outboundChannel a channel for messages going out to clients
	 */
	public AnnotationWebMessageHandler(MessageChannel inboundChannel, MessageChannel outboundChannel) {
		Assert.notNull(inboundChannel, "inboundChannel is required");
		Assert.notNull(outboundChannel, "outboundChannel is required");
		this.inboundChannel = inboundChannel;
		this.outboundChannel = outboundChannel;
	}

	public void setMessageConverters(List<MessageConverter> converters) {
		this.messageConverters = converters;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	protected Collection<MessageType> getSupportedMessageTypes() {
		return Arrays.asList(MessageType.MESSAGE, MessageType.SUBSCRIBE, MessageType.UNSUBSCRIBE);
	}

	@Override
	public void afterPropertiesSet() {

		initHandlerMethods();

		this.argumentResolvers.addResolver(new MessageChannelArgumentResolver(this.inboundChannel));
		this.argumentResolvers.addResolver(new MessageBodyArgumentResolver(this.messageConverters));

		this.returnValueHandlers.addHandler(new MessageReturnValueHandler(this.outboundChannel));
		this.returnValueHandlers.addHandler(new PayloadReturnValueHandler(this.outboundChannel));
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
		return ((AnnotationUtils.findAnnotation(beanType, Controller.class) != null) ||
				(AnnotationUtils.findAnnotation(beanType, MessageMapping.class) != null));
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
	public void handlePublish(Message<?> message) {
		handleMessageInternal(message, this.messageMethods);
	}

	@Override
	public void handleSubscribe(Message<?> message) {
		handleMessageInternal(message, this.subscribeMethods);
	}

	@Override
	public void handleUnsubscribe(Message<?> message) {
		handleMessageInternal(message, this.unsubscribeMethods);
	}

	private void handleMessageInternal(final Message<?> message, Map<MappingInfo, HandlerMethod> handlerMethods) {

		WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.wrap(message);
		String destination = headers.getDestination();

		HandlerMethod match = getHandlerMethod(destination, handlerMethods);
		if (match == null) {
			return;
		}

		HandlerMethod handlerMethod = match.createWithResolvedBean();

		// TODO: avoid re-creating invocableHandlerMethod
		InvocableMessageHandlerMethod invocableHandlerMethod = new InvocableMessageHandlerMethod(handlerMethod);
		invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
			MessageHolder.setMessage(message);

			Object value = invocableHandlerMethod.invoke(message);

			MethodParameter returnType = handlerMethod.getReturnType();
			if (void.class.equals(returnType.getParameterType())) {
				return;
			}

			this.returnValueHandlers.handleReturnValue(value, returnType, message);
		}
		catch (Exception ex) {
			invokeExceptionHandler(message, handlerMethod, ex);
		}
		catch (Throwable ex) {
			// TODO
			ex.printStackTrace();
		}
		finally {
			MessageHolder.reset();
		}
	}

	private void invokeExceptionHandler(Message<?> message, HandlerMethod handlerMethod, Exception ex) {

		InvocableMessageHandlerMethod invocableHandlerMethod;
		Class<?> beanType = handlerMethod.getBeanType();
		MessageExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(beanType);
		if (resolver == null) {
			resolver = new MessageExceptionHandlerMethodResolver(beanType);
			this.exceptionHandlerCache.put(beanType, resolver);
		}

		Method method = resolver.resolveMethod(ex);
		if (method == null) {
			logger.error("Unhandled exception", ex);
			return;
		}

		invocableHandlerMethod = new InvocableMessageHandlerMethod(handlerMethod.getBean(), method);
		invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
			invocableHandlerMethod.invoke(message, ex);
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
