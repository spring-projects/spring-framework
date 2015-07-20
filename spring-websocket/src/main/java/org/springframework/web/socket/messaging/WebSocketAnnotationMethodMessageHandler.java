/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.web.method.ControllerAdviceBean;

/**
 * A sub-class of {@link SimpAnnotationMethodMessageHandler} to provide support
 * for {@link org.springframework.web.bind.annotation.ControllerAdvice
 * ControllerAdvice} with global {@code @MessageExceptionHandler} methods.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketAnnotationMethodMessageHandler extends SimpAnnotationMethodMessageHandler {

	public WebSocketAnnotationMethodMessageHandler(SubscribableChannel clientInChannel, MessageChannel clientOutChannel,
			SimpMessageSendingOperations brokerTemplate) {

		super(clientInChannel, clientOutChannel, brokerTemplate);
	}


	@Override
	public void afterPropertiesSet() {
		initControllerAdviceCache();
		super.afterPropertiesSet();
	}

	private void initControllerAdviceCache() {
		if (getApplicationContext() == null) {
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for @MessageExceptionHandler mappings: " + getApplicationContext());
		}
		List<ControllerAdviceBean> controllerAdvice = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
		AnnotationAwareOrderComparator.sort(controllerAdvice);
		initMessagingAdviceCache(MessagingControllerAdviceBean.createFromList(controllerAdvice));
	}

	private void initMessagingAdviceCache(List<MessagingAdviceBean> beans) {
		if (beans == null) {
			return;
		}
		for (MessagingAdviceBean bean : beans) {
			Class<?> beanType = bean.getBeanType();
			AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(beanType);
			if (resolver.hasExceptionMappings()) {
				registerExceptionHandlerAdvice(bean, resolver);
				logger.info("Detected @MessageExceptionHandler methods in " + bean);
			}
		}
	}


	/**
	 * Adapt ControllerAdviceBean to MessagingAdviceBean.
	 */
	private static class MessagingControllerAdviceBean implements MessagingAdviceBean {

		private final ControllerAdviceBean adviceBean;

		private MessagingControllerAdviceBean(ControllerAdviceBean adviceBean) {
			this.adviceBean = adviceBean;
		}

		public static List<MessagingAdviceBean> createFromList(List<ControllerAdviceBean> controllerAdvice) {
			List<MessagingAdviceBean> messagingAdvice = new ArrayList<MessagingAdviceBean>(controllerAdvice.size());
			for (ControllerAdviceBean bean : controllerAdvice) {
				messagingAdvice.add(new MessagingControllerAdviceBean(bean));
			}
			return messagingAdvice;
		}

		@Override
		public Class<?> getBeanType() {
			return this.adviceBean.getBeanType();
		}

		@Override
		public Object resolveBean() {
			return this.adviceBean.resolveBean();
		}

		@Override
		public boolean isApplicableToBeanType(Class<?> beanType) {
			return this.adviceBean.isApplicableToBeanType(beanType);
		}

		@Override
		public int getOrder() {
			return this.adviceBean.getOrder();
		}
	}

}
