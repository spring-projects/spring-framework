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

package org.springframework.websocket.client.endpoint;

import javax.websocket.Session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.support.BeanCreatingHandlerProvider;
import org.springframework.websocket.support.SimpleHandlerProvider;

/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotatedEndpointConnectionManager extends EndpointConnectionManagerSupport
		implements BeanFactoryAware {

	private final HandlerProvider<Object> handlerProvider;


	public AnnotatedEndpointConnectionManager(Object endpointBean, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		this.handlerProvider = new SimpleHandlerProvider<Object>(endpointBean);
	}

	public AnnotatedEndpointConnectionManager(Class<?> endpointClass, String uriTemplate, Object... uriVariables) {
		super(uriTemplate, uriVariables);
		this.handlerProvider = new BeanCreatingHandlerProvider<Object>(endpointClass);
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.handlerProvider instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.handlerProvider).setBeanFactory(beanFactory);
		}
	}

	@Override
	protected void openConnection() throws Exception {
		Object endpoint = this.handlerProvider.getHandler();
		Session session = getWebSocketContainer().connectToServer(endpoint, getUri());
		updateSession(session);
	}

}
