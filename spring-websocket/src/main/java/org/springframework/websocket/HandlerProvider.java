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

package org.springframework.websocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HandlerProvider<T> implements BeanFactoryAware {

	private Log logger = LogFactory.getLog(this.getClass());

	private final T handlerBean;

	private final Class<? extends T> handlerClass;

	private AutowireCapableBeanFactory beanFactory;


	public HandlerProvider(T handlerBean) {
		Assert.notNull(handlerBean, "handlerBean is required");
		this.handlerBean = handlerBean;
		this.handlerClass = null;
	}

	public HandlerProvider(Class<? extends T> handlerClass) {
		Assert.notNull(handlerClass, "handlerClass is required");
		this.handlerBean = null;
		this.handlerClass = handlerClass;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	public void setLogger(Log logger) {
		this.logger = logger;
	}

	public boolean isSingleton() {
		return (this.handlerBean != null);
	}

	@SuppressWarnings("unchecked")
	public Class<? extends T> getHandlerType() {
		if (this.handlerClass != null) {
			return this.handlerClass;
		}
		return (Class<? extends T>) ClassUtils.getUserClass(this.handlerBean.getClass());
	}

	public T getHandler() {
		if (this.handlerBean != null) {
			if (logger != null && logger.isTraceEnabled()) {
				logger.trace("Returning handler singleton " + this.handlerBean);
			}
			return this.handlerBean;
		}
		Assert.isTrue(this.beanFactory != null, "BeanFactory is required to initialize handler instances.");
		if (logger != null && logger.isTraceEnabled()) {
			logger.trace("Creating handler of type " + this.handlerClass);
		}
		return this.beanFactory.createBean(this.handlerClass);
	}

}
