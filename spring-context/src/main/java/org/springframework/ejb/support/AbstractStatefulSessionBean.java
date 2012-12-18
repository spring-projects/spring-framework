/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.ejb.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;

/**
 * Convenient base class for EJB 2.x stateful session beans (SFSBs).
 * SFSBs should extend this class, leaving them to implement the
 * {@code ejbActivate()} and {@code ejbPassivate()} lifecycle
 * methods to comply with the requirements of the EJB specification.
 *
 * <p><b>Note: Subclasses should invoke the {@code loadBeanFactory()}
 * method in their custom {@code ejbCreate()} and {@code ejbActivate()}
 * methods, and should invoke the {@code unloadBeanFactory()} method in
 * their {@code ejbPassivate} method.</b>
 *
 * <p><b>Note: The default BeanFactoryLocator used by this class's superclass
 * (ContextJndiBeanFactoryLocator) is <b>not</b> serializable. Therefore,
 * when using the default BeanFactoryLocator, or another variant which is
 * not serializable, subclasses must call {@code setBeanFactoryLocator(null)}
 * in {@code ejbPassivate()}, with a corresponding call to
 * {@code setBeanFactoryLocator(xxx)} in {@code ejbActivate()}
 * unless relying on the default locator.
 *
 * @author Rod Johnson
 * @author Colin Sampaleanu
 * @see org.springframework.context.access.ContextJndiBeanFactoryLocator
 * @deprecated as of Spring 3.2, in favor of implementing EJBs in EJB 3 style
 */
@Deprecated
public abstract class AbstractStatefulSessionBean extends AbstractSessionBean {

	/**
	 * Load a Spring BeanFactory namespace. Exposed for subclasses
	 * to load a BeanFactory in their {@code ejbCreate()} methods.
	 * Those callers would normally want to catch BeansException and
	 * rethrow it as {@link javax.ejb.CreateException}. Unless the
	 * BeanFactory is known to be serializable, this method must also
	 * be called from {@code ejbActivate()}, to reload a context
	 * removed via a call to {@code unloadBeanFactory()} from
	 * the {@code ejbPassivate()} implementation.
	 */
	@Override
	protected void loadBeanFactory() throws BeansException {
		super.loadBeanFactory();
	}

	/**
	 * Unload the Spring BeanFactory instance. The default {@code ejbRemove()}
	 * method invokes this method, but subclasses which override
	 * {@code ejbRemove()} must invoke this method themselves.
	 * <p>Unless the BeanFactory is known to be serializable, this method
	 * must also be called from {@code ejbPassivate()}, with a corresponding
	 * call to {@code loadBeanFactory()} from {@code ejbActivate()}.
	 */
	@Override
	protected void unloadBeanFactory() throws FatalBeanException {
		super.unloadBeanFactory();
	}

}
