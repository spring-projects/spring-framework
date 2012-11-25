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
 * <code>ejbActivate()</code> and <code>ejbPassivate()</code> lifecycle
 * methods to comply with the requirements of the EJB specification.
 *
 * <p><b>Note: Subclasses should invoke the <code>loadBeanFactory()</code>
 * method in their custom <code>ejbCreate()</code> and <code>ejbActivate()</code>
 * methods, and should invoke the <code>unloadBeanFactory()</code> method in
 * their <code>ejbPassivate</code> method.</b>
 * 
 * <p><b>Note: The default BeanFactoryLocator used by this class's superclass
 * (ContextJndiBeanFactoryLocator) is <b>not</b> serializable. Therefore,
 * when using the default BeanFactoryLocator, or another variant which is
 * not serializable, subclasses must call <code>setBeanFactoryLocator(null)</code>
 * in <code>ejbPassivate()</code>, with a corresponding call to
 * <code>setBeanFactoryLocator(xxx)</code> in <code>ejbActivate()</code>
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
	 * to load a BeanFactory in their <code>ejbCreate()</code> methods.
	 * Those callers would normally want to catch BeansException and
	 * rethrow it as {@link javax.ejb.CreateException}. Unless the
	 * BeanFactory is known to be serializable, this method must also
	 * be called from <code>ejbActivate()</code>, to reload a context
	 * removed via a call to <code>unloadBeanFactory()</code> from
	 * the <code>ejbPassivate()</code> implementation.
	 */
	@Override
	protected void loadBeanFactory() throws BeansException {
		super.loadBeanFactory();
	}

	/**
	 * Unload the Spring BeanFactory instance. The default <code>ejbRemove()</code>
	 * method invokes this method, but subclasses which override
	 * <code>ejbRemove()</code> must invoke this method themselves.
	 * <p>Unless the BeanFactory is known to be serializable, this method
	 * must also be called from <code>ejbPassivate()</code>, with a corresponding
	 * call to <code>loadBeanFactory()</code> from <code>ejbActivate()</code>.
	 */
	@Override
	protected void unloadBeanFactory() throws FatalBeanException {
		super.unloadBeanFactory();
	}

}
