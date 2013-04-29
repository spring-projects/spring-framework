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

package org.springframework.websocket.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class BeanCreatingHandlerProvider<T> implements BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(BeanCreatingHandlerProvider.class);

	private final Class<? extends T> handlerType;

	private AutowireCapableBeanFactory beanFactory;


	public BeanCreatingHandlerProvider(Class<? extends T> handlerType) {
		Assert.notNull(handlerType, "handlerType is required");
		this.handlerType = handlerType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	public Class<? extends T> getHandlerType() {
		return this.handlerType;
	}

	public T getHandler() {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance for handler type " + this.handlerType);
		}
		if (this.beanFactory == null) {
			logger.warn("No BeanFactory available, attempting to use default constructor");
			return BeanUtils.instantiate(this.handlerType);
		}
		else {
			return this.beanFactory.createBean(this.handlerType);
		}
	}

	public void destroy(T handler) {
		if (this.beanFactory != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Destroying handler instance " + handler);
			}
			this.beanFactory.destroyBean(handler);
		}
	}

	@Override
	public String toString() {
		return "BeanCreatingHandlerProvider [handlerClass=" + handlerType + "]";
	}

}
