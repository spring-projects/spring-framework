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

import javax.ejb.CreateException;
import javax.ejb.EJBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenient base class for EJB 2.x stateless session beans (SLSBs),
 * minimizing the work involved in implementing an SLSB and preventing
 * common errors. <b>Note that SLSBs are the most useful kind of EJB.</b>
 *
 * <p>As the ejbActivate() and ejbPassivate() methods cannot be invoked
 * on SLSBs, these methods are implemented to throw an exception and should
 * not be overriden by subclasses. (Unfortunately the EJB specification
 * forbids enforcing this by making EJB lifecycle methods final.)
 *
 * <p>There should be no need to override the {@code setSessionContext()}
 * or {@code ejbCreate()} lifecycle methods.
 *
 * <p>Subclasses are left to implement the {@code onEjbCreate()} method
 * to do whatever initialization they wish to do after their BeanFactory has
 * already been loaded, and is available from the {@code getBeanFactory()}
 * method.
 *
 * <p>This class provides the no-arg {@code ejbCreate()} method required
 * by the EJB specification, but not the SessionBean interface, eliminating
 * a common cause of EJB deployment failure.
 *
 * @author Rod Johnson
 * @deprecated as of Spring 3.2, in favor of implementing EJBs in EJB 3 style
 */
@Deprecated
@SuppressWarnings("serial")
public abstract class AbstractStatelessSessionBean extends AbstractSessionBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * This implementation loads the BeanFactory. A BeansException thrown by
	 * loadBeanFactory will simply get propagated, as it is a runtime exception.
	 * <p>Don't override it (although it can't be made final): code your own
	 * initialization in onEjbCreate(), which is called when the BeanFactory
	 * is available.
	 * <p>Unfortunately we can't load the BeanFactory in setSessionContext(),
	 * as resource manager access isn't permitted there - but the BeanFactory
	 * may require it.
	 */
	public void ejbCreate() throws CreateException {
		loadBeanFactory();
		onEjbCreate();
	}

	/**
	 * Subclasses must implement this method to do any initialization
	 * they would otherwise have done in an {@code ejbCreate()} method.
	 * In contrast to {@code ejbCreate}, the BeanFactory will have been loaded here.
	 * <p>The same restrictions apply to the work of this method as
	 * to an {@code ejbCreate()} method.
	 * @throws CreateException
	 */
	protected abstract void onEjbCreate() throws CreateException;


	/**
	 * This method always throws an exception, as it should not be invoked by the EJB container.
	 * @see javax.ejb.SessionBean#ejbActivate()
	 */
	@Override
	public void ejbActivate() throws EJBException {
		throw new IllegalStateException("ejbActivate must not be invoked on a stateless session bean");
	}

	/**
	 * This method always throws an exception, as it should not be invoked by the EJB container.
	 * @see javax.ejb.SessionBean#ejbPassivate()
	 */
	@Override
	public void ejbPassivate() throws EJBException {
		throw new IllegalStateException("ejbPassivate must not be invoked on a stateless session bean");
	}

}
