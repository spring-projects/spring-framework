/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jms.config;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * A {@link JmsListenerEndpoint} providing the method to invoke to process
 * an incoming message for this endpoint.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class MethodJmsListenerEndpoint extends AbstractJmsListenerEndpoint implements BeanFactoryAware {

	@Nullable
	private Object bean;

	@Nullable
	private Method method;

	@Nullable
	private Method mostSpecificMethod;

	@Nullable
	private MessageHandlerMethodFactory messageHandlerMethodFactory;

	@Nullable
	private StringValueResolver embeddedValueResolver;


	/**
	 * Set the actual bean instance to invoke this endpoint method on.
	 */
	public void setBean(@Nullable Object bean) {
		this.bean = bean;
	}

	@Nullable
	public Object getBean() {
		return this.bean;
	}

	/**
	 * Set the method to invoke for processing a message managed by this endpoint.
	 */
	public void setMethod(@Nullable Method method) {
		this.method = method;
	}

	@Nullable
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Set the most specific method known for this endpoint's declaration.
	 * <p>In case of a proxy, this will be the method on the target class
	 * (if annotated itself, that is, if not just annotated in an interface).
	 * @since 4.2.3
	 */
	public void setMostSpecificMethod(@Nullable Method mostSpecificMethod) {
		this.mostSpecificMethod = mostSpecificMethod;
	}

	@Nullable
	public Method getMostSpecificMethod() {
		if (this.mostSpecificMethod != null) {
			return this.mostSpecificMethod;
		}
		Method method = getMethod();
		if (method != null) {
			Object bean = getBean();
			if (AopUtils.isAopProxy(bean)) {
				Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
				method = AopUtils.getMostSpecificMethod(method, targetClass);
			}
		}
		return method;
	}

	/**
	 * Set the {@link MessageHandlerMethodFactory} to use to build the
	 * {@link InvocableHandlerMethod} responsible to manage the invocation
	 * of this endpoint.
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	/**
	 * Set a value resolver for embedded placeholders and expressions.
	 */
	public void setEmbeddedValueResolver(@Nullable StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}

	/**
	 * Set the {@link BeanFactory} to use to resolve expressions (may be {@code null}).
	 */
	@Override
	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
		if (this.embeddedValueResolver == null && beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	protected MessagingMessageListenerAdapter createMessageListener(MessageListenerContainer container) {
		Assert.state(this.messageHandlerMethodFactory != null,
				"Could not create message listener - MessageHandlerMethodFactory not set");
		MessagingMessageListenerAdapter messageListener = createMessageListenerInstance();
		Object bean = getBean();
		Method method = getMethod();
		Assert.state(bean != null && method != null, "No bean+method set on endpoint");
		InvocableHandlerMethod invocableHandlerMethod =
				this.messageHandlerMethodFactory.createInvocableHandlerMethod(bean, method);
		messageListener.setHandlerMethod(invocableHandlerMethod);
		String responseDestination = getDefaultResponseDestination();
		if (StringUtils.hasText(responseDestination)) {
			if (container.isReplyPubSubDomain()) {
				messageListener.setDefaultResponseTopicName(responseDestination);
			}
			else {
				messageListener.setDefaultResponseQueueName(responseDestination);
			}
		}
		QosSettings responseQosSettings = container.getReplyQosSettings();
		if (responseQosSettings != null) {
			messageListener.setResponseQosSettings(responseQosSettings);
		}
		MessageConverter messageConverter = container.getMessageConverter();
		if (messageConverter != null) {
			messageListener.setMessageConverter(messageConverter);
		}
		DestinationResolver destinationResolver = container.getDestinationResolver();
		if (destinationResolver != null) {
			messageListener.setDestinationResolver(destinationResolver);
		}
		return messageListener;
	}

	/**
	 * Create an empty {@link MessagingMessageListenerAdapter} instance.
	 * @return a new {@code MessagingMessageListenerAdapter} or subclass thereof
	 */
	protected MessagingMessageListenerAdapter createMessageListenerInstance() {
		return new MessagingMessageListenerAdapter();
	}

	/**
	 * Return the default response destination, if any.
	 */
	@Nullable
	protected String getDefaultResponseDestination() {
		Method specificMethod = getMostSpecificMethod();
		if (specificMethod == null) {
			return null;
		}
		SendTo ann = getSendTo(specificMethod);
		if (ann != null) {
			Object[] destinations = ann.value();
			if (destinations.length != 1) {
				throw new IllegalStateException("Invalid @" + SendTo.class.getSimpleName() + " annotation on '" +
						specificMethod + "' one destination must be set (got " + Arrays.toString(destinations) + ")");
			}
			return resolve((String) destinations[0]);
		}
		return null;
	}

	@Nullable
	private SendTo getSendTo(Method specificMethod) {
		SendTo ann = AnnotatedElementUtils.findMergedAnnotation(specificMethod, SendTo.class);
		if (ann == null) {
			ann = AnnotatedElementUtils.findMergedAnnotation(specificMethod.getDeclaringClass(), SendTo.class);
		}
		return ann;
	}

	@Nullable
	private String resolve(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


	@Override
	protected StringBuilder getEndpointDescription() {
		return super.getEndpointDescription()
				.append(" | bean='").append(this.bean).append("'")
				.append(" | method='").append(this.method).append("'");
	}

}
