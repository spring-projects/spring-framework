/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.ejb.EnterpriseBean;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.context.access.ContextJndiBeanFactoryLocator;
import org.springframework.util.WeakReferenceMonitor;

/**
 * Base class for Spring-based EJB 2.x beans. Not intended for direct subclassing:
 * Extend {@link AbstractStatelessSessionBean}, {@link AbstractStatefulSessionBean}
 * or {@link AbstractMessageDrivenBean} instead.
 *
 * <p>Provides a standard way of loading a Spring BeanFactory. Subclasses act as a
 * facade, with the business logic deferred to beans in the BeanFactory. Default
 * is to use a {@link org.springframework.context.access.ContextJndiBeanFactoryLocator},
 * which will initialize an XML ApplicationContext from the class path (based on a JNDI
 * name specified). For a different locator strategy, {@code setBeanFactoryLocator}
 * may be called (<i>before</i> your EJB's {@code ejbCreate} method is invoked,
 * e.g. in {@code setSessionContext}). For use of a shared ApplicationContext between
 * multiple EJBs, where the container class loader setup supports this visibility, you may
 * instead use a {@link org.springframework.context.access.ContextSingletonBeanFactoryLocator}.
 * Alternatively, {@link #setBeanFactoryLocator} may be called with a custom implementation
 * of the {@link org.springframework.beans.factory.access.BeanFactoryLocator} interface.
 *
 * <p>Note that we cannot use {@code final} for our implementation of EJB lifecycle
 * methods, as this would violate the EJB specification.
 *
 * @author Rod Johnson
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @see org.springframework.context.access.ContextJndiBeanFactoryLocator
 * @see org.springframework.context.access.ContextSingletonBeanFactoryLocator
 * @deprecated as of Spring 3.2, in favor of implementing EJBs in EJB 3 style
 */
@Deprecated
@SuppressWarnings("serial")
public abstract class AbstractEnterpriseBean implements EnterpriseBean {

	public static final String BEAN_FACTORY_PATH_ENVIRONMENT_KEY = "java:comp/env/ejb/BeanFactoryPath";


	/**
	 * Helper strategy that knows how to locate a Spring BeanFactory (or
	 * ApplicationContext).
	 */
	private BeanFactoryLocator beanFactoryLocator;

	/** factoryKey to be used with BeanFactoryLocator */
	private String beanFactoryLocatorKey;

	/** Spring BeanFactory that provides the namespace for this EJB */
	private BeanFactoryReference beanFactoryReference;


	/**
	 * Set the BeanFactoryLocator to use for this EJB. Default is a
	 * ContextJndiBeanFactoryLocator.
	 * <p>Can be invoked before loadBeanFactory, for example in constructor or
	 * setSessionContext if you want to override the default locator.
	 * <p>Note that the BeanFactory is automatically loaded by the {@code ejbCreate}
	 * implementations of AbstractStatelessSessionBean and
	 * AbstractMessageDriverBean but needs to be explicitly loaded in custom
	 * AbstractStatefulSessionBean {@code ejbCreate} methods.
	 * @see AbstractStatelessSessionBean#ejbCreate
	 * @see AbstractMessageDrivenBean#ejbCreate
	 * @see AbstractStatefulSessionBean#loadBeanFactory
	 * @see org.springframework.context.access.ContextJndiBeanFactoryLocator
	 */
	public void setBeanFactoryLocator(BeanFactoryLocator beanFactoryLocator) {
		this.beanFactoryLocator = beanFactoryLocator;
	}

	/**
	 * Set the bean factory locator key.
	 * <p>In case of the default BeanFactoryLocator implementation,
	 * ContextJndiBeanFactoryLocator, this is the JNDI path. The default value
	 * of this property is "java:comp/env/ejb/BeanFactoryPath".
	 * <p>Can be invoked before {@link #loadBeanFactory}, for example in the constructor
	 * or {@code setSessionContext} if you want to override the default locator key.
	 * @see #BEAN_FACTORY_PATH_ENVIRONMENT_KEY
	 */
	public void setBeanFactoryLocatorKey(String factoryKey) {
		this.beanFactoryLocatorKey = factoryKey;
	}

	/**
	 * Load a Spring BeanFactory namespace. Subclasses must invoke this method.
	 * <p>Package-visible as it shouldn't be called directly by user-created
	 * subclasses.
	 * @see org.springframework.ejb.support.AbstractStatelessSessionBean#ejbCreate()
	 */
	void loadBeanFactory() throws BeansException {
		if (this.beanFactoryLocator == null) {
			this.beanFactoryLocator = new ContextJndiBeanFactoryLocator();
		}
		if (this.beanFactoryLocatorKey == null) {
			this.beanFactoryLocatorKey = BEAN_FACTORY_PATH_ENVIRONMENT_KEY;
		}

		this.beanFactoryReference = this.beanFactoryLocator.useBeanFactory(this.beanFactoryLocatorKey);

		// We cannot rely on the container to call ejbRemove() (it's skipped in
		// the case of system exceptions), so ensure the the bean factory
		// reference is eventually released.
		WeakReferenceMonitor.monitor(this, new BeanFactoryReferenceReleaseListener(this.beanFactoryReference));
	}

	/**
	 * Unload the Spring BeanFactory instance. The default {@link #ejbRemove()}
	 * method invokes this method, but subclasses which override {@code ejbRemove()}
	 * must invoke this method themselves.
	 * <p>Package-visible as it shouldn't be called directly by user-created
	 * subclasses.
	 */
	void unloadBeanFactory() throws FatalBeanException {
		// We will not ever get here if the container skips calling ejbRemove(),
		// but the WeakReferenceMonitor will still clean up (later) in that case.
		if (this.beanFactoryReference != null) {
			this.beanFactoryReference.release();
			this.beanFactoryReference = null;
		}
	}

	/**
	 * May be called after {@code ejbCreate()}.
	 * @return the bean factory
	 */
	protected BeanFactory getBeanFactory() {
		return this.beanFactoryReference.getFactory();
	}

	/**
	 * EJB lifecycle method, implemented to invoke {@code onEjbRemove()}
	 * and unload the BeanFactory afterwards.
	 * <p>Don't override it (although it can't be made final): code your shutdown
	 * in {@link #onEjbRemove()}.
	 */
	public void ejbRemove() {
		onEjbRemove();
		unloadBeanFactory();
	}

	/**
	 * Subclasses must implement this method to do any initialization they would
	 * otherwise have done in an {@code ejbRemove()} method.
	 * The BeanFactory will be unloaded afterwards.
	 * <p>This implementation is empty, to be overridden in subclasses.
	 * The same restrictions apply to the work of this method as to an
	 * {@code ejbRemove()} method.
	 */
	protected void onEjbRemove() {
		// empty
	}


	/**
	 * Implementation of WeakReferenceMonitor's ReleaseListener callback interface.
	 * Release the given BeanFactoryReference if the monitor detects that there
	 * are no strong references to the handle anymore.
	 */
	private static class BeanFactoryReferenceReleaseListener implements WeakReferenceMonitor.ReleaseListener {

		private final BeanFactoryReference beanFactoryReference;

		public BeanFactoryReferenceReleaseListener(BeanFactoryReference beanFactoryReference) {
			this.beanFactoryReference = beanFactoryReference;
		}

		public void released() {
			this.beanFactoryReference.release();
		}
	}

}
