/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Invoker for a local Stateless Session Bean.
 * Designed for EJB 2.x, but works for EJB 3 Session Beans as well.
 *
 * <p>Caches the home object, since a local EJB home can never go stale.
 * See {@link org.springframework.jndi.JndiObjectLocator} for info on
 * how to specify the JNDI location of the target EJB.
 *
 * <p>In a bean container, this class is normally best used as a singleton. However,
 * if that bean container pre-instantiates singletons (as do the XML ApplicationContext
 * variants) you may have a problem if the bean container is loaded before the EJB
 * container loads the target EJB. That is because by default the JNDI lookup will be
 * performed in the init method of this class and cached, but the EJB will not have been
 * bound at the target location yet. The best solution is to set the lookupHomeOnStartup
 * property to false, in which case the home will be fetched on first access to the EJB.
 * (This flag is only true by default for backwards compatibility reasons).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AbstractSlsbInvokerInterceptor#setLookupHomeOnStartup
 * @see AbstractSlsbInvokerInterceptor#setCacheHome
 */
public class LocalSlsbInvokerInterceptor extends AbstractSlsbInvokerInterceptor {

	private volatile boolean homeAsComponent = false;


	/**
	 * This implementation "creates" a new EJB instance for each invocation.
	 * Can be overridden for custom invocation strategies.
	 * <p>Alternatively, override {@link #getSessionBeanInstance} and
	 * {@link #releaseSessionBeanInstance} to change EJB instance creation,
	 * for example to hold a single shared EJB instance.
	 */
	@Override
	public Object invokeInContext(MethodInvocation invocation) throws Throwable {
		Object ejb = null;
		try {
			ejb = getSessionBeanInstance();
			Method method = invocation.getMethod();
			if (method.getDeclaringClass().isInstance(ejb)) {
				// directly implemented
				return method.invoke(ejb, invocation.getArguments());
			}
			else {
				// not directly implemented
				Method ejbMethod = ejb.getClass().getMethod(method.getName(), method.getParameterTypes());
				return ejbMethod.invoke(ejb, invocation.getArguments());
			}
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (logger.isDebugEnabled()) {
				logger.debug("Method of local EJB [" + getJndiName() + "] threw exception", targetEx);
			}
			if (targetEx instanceof CreateException) {
				throw new EjbAccessException("Could not create local EJB [" + getJndiName() + "]", targetEx);
			}
			else {
				throw targetEx;
			}
		}
		catch (NamingException ex) {
			throw new EjbAccessException("Failed to locate local EJB [" + getJndiName() + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new EjbAccessException("Could not access method [" + invocation.getMethod().getName() +
			    "] of local EJB [" + getJndiName() + "]", ex);
		}
		finally {
			if (ejb instanceof EJBLocalObject) {
				releaseSessionBeanInstance((EJBLocalObject) ejb);
			}
		}
	}

	/**
	 * Check for EJB3-style home object that serves as EJB component directly.
	 */
	@Override
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		if (this.homeAsComponent) {
			return null;
		}
		if (!(home instanceof EJBLocalHome)) {
			// An EJB3 Session Bean...
			this.homeAsComponent = true;
			return null;
		}
		return super.getCreateMethod(home);
	}

	/**
	 * Return an EJB instance to delegate the call to.
	 * Default implementation delegates to newSessionBeanInstance.
	 * @throws NamingException if thrown by JNDI
	 * @throws InvocationTargetException if thrown by the create method
	 * @see #newSessionBeanInstance
	 */
	protected Object getSessionBeanInstance() throws NamingException, InvocationTargetException {
		return newSessionBeanInstance();
	}

	/**
	 * Release the given EJB instance.
	 * Default implementation delegates to removeSessionBeanInstance.
	 * @param ejb the EJB instance to release
	 * @see #removeSessionBeanInstance
	 */
	protected void releaseSessionBeanInstance(EJBLocalObject ejb) {
		removeSessionBeanInstance(ejb);
	}

	/**
	 * Return a new instance of the stateless session bean.
	 * Can be overridden to change the algorithm.
	 * @throws NamingException if thrown by JNDI
	 * @throws InvocationTargetException if thrown by the create method
	 * @see #create
	 */
	protected Object newSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to create reference to local EJB");
		}
		Object ejbInstance = create();
		if (logger.isDebugEnabled()) {
			logger.debug("Obtained reference to local EJB: " + ejbInstance);
		}
		return ejbInstance;
	}

	/**
	 * Remove the given EJB instance.
	 * @param ejb the EJB instance to remove
	 * @see javax.ejb.EJBLocalObject#remove()
	 */
	protected void removeSessionBeanInstance(EJBLocalObject ejb) {
		if (ejb != null && !this.homeAsComponent) {
			try {
				ejb.remove();
			}
			catch (Throwable ex) {
				logger.warn("Could not invoke 'remove' on local EJB proxy", ex);
			}
		}
	}

}
