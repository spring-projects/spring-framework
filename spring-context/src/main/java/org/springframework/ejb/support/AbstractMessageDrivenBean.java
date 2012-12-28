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

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenient base class for EJB 2.x MDBs.
 * Doesn't require JMS, as EJB 2.1 MDBs are no longer JMS-specific;
 * see the {@link AbstractJmsMessageDrivenBean} subclass.
 *
 * <p>This class ensures that subclasses have access to the
 * MessageDrivenContext provided by the EJB container, and implement
 * a no-arg {@code ejbCreate()} method as required by the EJB
 * specification. This {@code ejbCreate()} method loads a BeanFactory,
 * before invoking the {@code onEjbCreate()} method, which is
 * supposed to contain subclass-specific initialization.
 *
 * <p>NB: We cannot use final methods to implement EJB API methods,
 * as this violates the EJB specification. However, there should be
 * no need to override the {@code setMessageDrivenContext} or
 * {@code ejbCreate()} methods.
 *
 * @author Rod Johnson
 * @deprecated as of Spring 3.2, in favor of implementing EJBs in EJB 3 style
 */
@Deprecated
public abstract class AbstractMessageDrivenBean extends AbstractEnterpriseBean
	implements MessageDrivenBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private MessageDrivenContext messageDrivenContext;


	/**
	 * Required lifecycle method. Sets the MessageDriven context.
	 * @param messageDrivenContext MessageDrivenContext
	 */
	public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
		this.messageDrivenContext = messageDrivenContext;
	}

	/**
	 * Convenience method for subclasses to use.
	 * @return the MessageDrivenContext passed to this EJB by the EJB container
	 */
	protected final MessageDrivenContext getMessageDrivenContext() {
		return this.messageDrivenContext;
	}

	/**
	 * Lifecycle method required by the EJB specification but not the
	 * MessageDrivenBean interface. This implementation loads the BeanFactory.
	 * <p>Don't override it (although it can't be made final): code initialization
	 * in onEjbCreate(), which is called when the BeanFactory is available.
	 * <p>Unfortunately we can't load the BeanFactory in setSessionContext(),
	 * as resource manager access isn't permitted and the BeanFactory may require it.
	 */
	public void ejbCreate() {
		loadBeanFactory();
		onEjbCreate();
	}

	/**
	 * Subclasses must implement this method to do any initialization they would
	 * otherwise have done in an {@code ejbCreate()} method. In contrast
	 * to {@code ejbCreate()}, the BeanFactory will have been loaded here.
	 * <p>The same restrictions apply to the work of this method as
	 * to an {@code ejbCreate()} method.
	 */
	protected abstract void onEjbCreate();

}
