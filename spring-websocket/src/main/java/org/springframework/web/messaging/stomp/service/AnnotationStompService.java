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

package org.springframework.web.messaging.stomp.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.messaging.MessageMapping;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.converter.StringMessageConverter;
import org.springframework.web.messaging.stomp.StompMessage;
import org.springframework.web.messaging.stomp.service.support.MessageBodyArgumentResolver;
import org.springframework.web.messaging.stomp.service.support.MessageBrokerArgumentResolver;
import org.springframework.web.messaging.stomp.service.support.SubscriptionArgumentResolver;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;

import reactor.core.Reactor;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotationStompService extends AbstractStompService
		implements ApplicationContextAware, InitializingBean {

	private List<MessageConverter<?>> messageConverters;

	private ApplicationContext applicationContext;

	private Map<MessageMapping, HandlerMethod> handlerMethods = new HashMap<MessageMapping, HandlerMethod>();

	private MessageMethodArgumentResolverComposite argumentResolvers = new MessageMethodArgumentResolverComposite();


	public AnnotationStompService(Reactor reactor) {
		super(reactor);
	}

	public void setMessageConverters(List<MessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {

		initHandlerMethods();

		if (this.messageConverters == null) {
			this.messageConverters = new ArrayList<MessageConverter<?>>();
			this.messageConverters.add(new StringMessageConverter());
			this.messageConverters.add(new MappingJackson2MessageConverter());
		}
		this.argumentResolvers.addResolver(new SubscriptionArgumentResolver(getReactor(), this.messageConverters));
		this.argumentResolvers.addResolver(new MessageBrokerArgumentResolver(getReactor(), this.messageConverters));
		this.argumentResolvers.addResolver(new MessageBodyArgumentResolver(this.messageConverters));
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

	protected void detectHandlerMethods(final Object handler) {

		Class<?> handlerType = (handler instanceof String) ?
				this.applicationContext.getType((String) handler) : handler.getClass();

		final Class<?> userType = ClassUtils.getUserClass(handlerType);

		Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				return AnnotationUtils.findAnnotation(method, MessageMapping.class) != null;
			}
		});

		for (Method method : methods) {
			MessageMapping mapping = AnnotationUtils.findAnnotation(method, MessageMapping.class);
			HandlerMethod handlerMethod = createHandlerMethod(handler, method);
			this.handlerMethods.put(mapping, handlerMethod);
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

	protected HandlerMethod getHandlerMethod(String destination, MessageType messageType) {
		for (MessageMapping mapping : this.handlerMethods.keySet()) {
			boolean match = false;
			for (String mappingDestination : mapping.value()) {
				if (destination.equals(mappingDestination)) {
					match = true;
					break;
				}
			}
			if (match && messageType.equals(mapping.messageType())) {
				return this.handlerMethods.get(mapping);
			}
		}
		return null;
	}

	@Override
	protected void processSubscribe(StompMessage message, Object replyTo) {
		handleMessage(message, replyTo, MessageType.SUBSCRIBE);
	}

	@Override
	protected void processSend(StompMessage message) {
		handleMessage(message, null, MessageType.SEND);
	}

	private void handleMessage(final StompMessage message, final Object replyTo, MessageType messageType) {

		String destination = message.getHeaders().getDestination();

		HandlerMethod match = getHandlerMethod(destination, messageType);
		if (match == null) {
			return;
		}

		HandlerMethod handlerMethod = match.createWithResolvedBean();
		InvocableMessageHandlerMethod messageHandlerMethod = new InvocableMessageHandlerMethod(handlerMethod);
		messageHandlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);

		try {
			messageHandlerMethod.invoke(message, replyTo);
		}
		catch (Throwable e) {
			// TODO: send error message, or add @ExceptionHandler-like capability
			e.printStackTrace();
		}
	}

}
