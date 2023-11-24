/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.handler;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Instantiates a target handler through a Spring {@link BeanFactory} and also provides
 * an equivalent destroy method. Mainly for internal use to assist with initializing and
 * destroying handlers with per-connection lifecycle.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> the handler type
 */
public class BeanCreatingHandlerProvider<T> implements BeanFactoryAware {

	private final Class<? extends T> handlerType;

	@Nullable
	private AutowireCapableBeanFactory beanFactory;


	public BeanCreatingHandlerProvider(Class<? extends T> handlerType) {
		Assert.notNull(handlerType, "handlerType must not be null");
		this.handlerType = handlerType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof AutowireCapableBeanFactory autowireCapableBeanFactory) {
			this.beanFactory = autowireCapableBeanFactory;
		}
	}

	public void destroy(T handler) {
		if (this.beanFactory != null) {
			this.beanFactory.destroyBean(handler);
		}
	}


	public Class<? extends T> getHandlerType() {
		return this.handlerType;
	}

	public T getHandler() {
		if (this.beanFactory != null) {
			return this.beanFactory.createBean(this.handlerType);
		}
		else {
			return BeanUtils.instantiateClass(this.handlerType);
		}
	}

	@Override
	public String toString() {
		return "BeanCreatingHandlerProvider[handlerType=" + this.handlerType + "]";
	}

}
