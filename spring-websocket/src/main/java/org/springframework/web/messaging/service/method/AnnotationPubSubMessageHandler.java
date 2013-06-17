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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubChannelRegistry;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.annotation.SubscribeEvent;
import org.springframework.web.messaging.annotation.UnsubscribeEvent;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.service.AbstractPubSubMessageHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotationPubSubMessageHandler extends AbstractPubSubMessageHandler
		implements ApplicationContextAware, InitializingBean {

	private PubSubChannelRegistry registry;

	private List<MessageConverter> messageConverters;

	private ApplicationContext applicationContext;

	private Map<MappingInfo, HandlerMethod> messageMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> subscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private Map<MappingInfo, HandlerMethod> unsubscribeMethods = new HashMap<MappingInfo, HandlerMethod>();

	private ArgumentResolverComposite argumentResolvers = new ArgumentResolverComposite();

	private ReturnValueHandlerComposite returnValueHandlers = new ReturnValueHandlerComposite();


	public AnnotationPubSubMessageHandler(PubSubChannelRegistry registry) {
		Assert.notNull(registry, "registry is required");
		this.registry = registry;
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

		this.argumentResolvers.addResolver(new MessageChannelArgumentResolver(this.registry.getMessageBrokerChannel()));
		this.argumentResolvers.addResolver(new MessageBodyArgumentResolver(this.messageConverters));

		this.returnValueHandlers.addHandler(new MessageReturnValueHandler(this.registry.getClientOutputChannel()));
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

	@SuppressWarnings("unchecked")
	private <A extends Annotation> void initHandlerMethods(Object handler, Class<?> handlerType,
			final Class<A> annotationType, MappingInfoCreator mappingInfoCreator,
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

		PubSubHeaders headers = PubSubHeaders.fromMessageHeaders(message.getHeaders());
		String destination = headers.getDestination();

		HandlerMethod match = getHandlerMethod(destination, handlerMethods);
		if (match == null) {
			return;
		}

		HandlerMethod handlerMethod = match.createWithResolvedBean();

		// TODO:
		InvocableMessageHandlerMethod invocableHandlerMethod = new InvocableMessageHandlerMethod(handlerMethod);
		invocableHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
			Object value = invocableHandlerMethod.invoke(message);

			MethodParameter returnType = handlerMethod.getReturnType();
			if (void.class.equals(returnType.getParameterType())) {
				return;
			}

			this.returnValueHandlers.handleReturnValue(value, returnType, message);

		}
		catch (Throwable e) {
			// TODO: send error message, or add @ExceptionHandler-like capability
			e.printStackTrace();
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
