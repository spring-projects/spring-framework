/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.event;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.OrderComparator;

/**
 * Abstract implementation of the {@link ApplicationEventMulticaster} interface,
 * providing the basic listener registration facility.
 *
 * <p>Doesn't permit multiple instances of the same listener by default,
 * as it keeps listeners in a linked Set. The collection class used to hold
 * ApplicationListener objects can be overridden through the "collectionClass"
 * bean property.
 *
 * <p>Implementing ApplicationEventMulticaster's actual {@link #multicastEvent} method
 * is left to subclasses. {@link SimpleApplicationEventMulticaster} simply multicasts
 * all events to all registered listeners, invoking them in the calling thread.
 * Alternative implementations could be more sophisticated in those respects.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #setCollectionClass
 * @see #getApplicationListeners()
 * @see SimpleApplicationEventMulticaster
 */
public abstract class AbstractApplicationEventMulticaster
		implements ApplicationEventMulticaster, BeanFactoryAware {

	private final Set<ApplicationListener> applicationListeners = new LinkedHashSet<ApplicationListener>();

	private final Set<String> applicationListenerBeans = new LinkedHashSet<String>();

	private BeanFactory beanFactory;


	public void addApplicationListener(ApplicationListener listener) {
		this.applicationListeners.add(listener);
	}

	public void addApplicationListenerBean(String listenerBeanName) {
		this.applicationListenerBeans.add(listenerBeanName);
	}

	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Return the current Collection of ApplicationListeners.
	 * @return a Collection of ApplicationListeners
	 * @see org.springframework.context.ApplicationListener
	 */
	protected Collection<ApplicationListener> getApplicationListeners() {
		LinkedList<ApplicationListener> allListeners =
				new LinkedList<ApplicationListener>(this.applicationListeners);
		if (!this.applicationListenerBeans.isEmpty()) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
						"because it is not associated with a BeanFactory: " + this.applicationListenerBeans);
			}
			for (String listenerBeanName : applicationListenerBeans) {
				allListeners.add(this.beanFactory.getBean(listenerBeanName, ApplicationListener.class));
			}
		}
		Collections.sort(allListeners, new OrderComparator());
		return allListeners;
	}

}
