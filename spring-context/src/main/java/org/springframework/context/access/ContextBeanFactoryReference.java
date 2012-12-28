/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.context.access;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * ApplicationContext-specific implementation of BeanFactoryReference,
 * wrapping a newly created ApplicationContext, closing it on release.
 *
 * <p>As per BeanFactoryReference contract, {@code release} may be called
 * more than once, with subsequent calls not doing anything. However, calling
 * {@code getFactory} after a {@code release} call will cause an exception.
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @since 13.02.2004
 * @see org.springframework.context.ConfigurableApplicationContext#close
 */
public class ContextBeanFactoryReference implements BeanFactoryReference {

	private ApplicationContext applicationContext;


	/**
	 * Create a new ContextBeanFactoryReference for the given context.
	 * @param applicationContext the ApplicationContext to wrap
	 */
	public ContextBeanFactoryReference(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	public BeanFactory getFactory() {
		if (this.applicationContext == null) {
			throw new IllegalStateException(
					"ApplicationContext owned by this BeanFactoryReference has been released");
		}
		return this.applicationContext;
	}

	public void release() {
		if (this.applicationContext != null) {
			ApplicationContext savedCtx;

			// We don't actually guarantee thread-safety, but it's not a lot of extra work.
			synchronized (this) {
				savedCtx = this.applicationContext;
				this.applicationContext = null;
			}

			if (savedCtx != null && savedCtx instanceof ConfigurableApplicationContext) {
				((ConfigurableApplicationContext) savedCtx).close();
			}
		}
	}

}
