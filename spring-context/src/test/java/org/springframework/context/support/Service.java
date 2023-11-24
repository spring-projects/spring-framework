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

package org.springframework.context.support;

import java.util.Set;

import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 */
public class Service implements ApplicationContextAware, MessageSourceAware, DisposableBean {

	private ApplicationContext applicationContext;

	private MessageSource messageSource;

	private Resource[] resources;

	private Set<Resource> resourceSet;

	private boolean properlyDestroyed = false;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		if (this.messageSource != null) {
			throw new IllegalArgumentException("MessageSource should not be set twice");
		}
		this.messageSource = messageSource;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	public Resource[] getResources() {
		return resources;
	}

	public void setResourceSet(Set<Resource> resourceSet) {
		this.resourceSet = resourceSet;
	}

	public Set<Resource> getResourceSet() {
		return resourceSet;
	}


	@Override
	public void destroy() {
		this.properlyDestroyed = true;
		Thread thread = new Thread() {
			@Override
			public void run() {
				Assert.state(applicationContext.getBean("messageSource") instanceof StaticMessageSource,
						"Invalid MessageSource bean");
				try {
					applicationContext.getBean("service2");
					// Should have thrown BeanCreationNotAllowedException
					properlyDestroyed = false;
				}
				catch (BeanCreationNotAllowedException ex) {
					// expected
				}
			}
		};
		thread.start();
		try {
			thread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public boolean isProperlyDestroyed() {
		return properlyDestroyed;
	}

}
