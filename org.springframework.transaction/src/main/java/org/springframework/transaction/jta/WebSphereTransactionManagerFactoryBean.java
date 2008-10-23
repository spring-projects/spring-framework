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

package org.springframework.transaction.jta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.TransactionSystemException;

/**
 * {@link org.springframework.beans.factory.FactoryBean} that retrieves
 * the JTA TransactionManager for IBM's WebSphere application servers
 * (versions 5.1, 6.0 and 6.1).
 *
 * <p>Uses WebSphere's static accessor methods to obtain the internal JTA
 * TransactionManager. This is known to work reliably on all tested WebSphere
 * versions; however, access to the internal TransactionManager facility
 * is not officially supported by IBM.
 *
 * <p>In combination with Spring's JtaTransactionManager, this FactoryBean
 * can be used to enable transaction suspension (PROPAGATION_REQUIRES_NEW,
 * PROPAGATION_NOT_SUPPORTED) on WebSphere:
 *
 * <pre>
 * &lt;bean id="wsJtaTm" class="org.springframework.transaction.jta.WebSphereTransactionManagerFactoryBean"/&gt;
 *
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="transactionManager ref="wsJtaTm"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note that Spring's JtaTransactionManager will continue to use the JTA
 * UserTransaction for standard transaction demarcation, as defined by
 * standard J2EE. It will only use the provided WebSphere TransactionManager
 * in case of actual transaction suspension needs. <i>If you do not require
 * transaction suspension in the first place, do not bother with this FactoryBean.</i>
 *
 * <p><b>NOTE: On recent WebSphere 6.0.x and 6.1.x versions, this class has
 * been superseded by the {@link WebSphereUowTransactionManager} class, which
 * uses IBM's official UOWManager API facility for transaction suspension.</b>
 * The WebSphereUowTransactionManager class is a direct replacement for a
 * standard JtaTransactionManager definition, without further configuration.
 *
 * @author Juergen Hoeller
 * @since 21.01.2004
 * @see JtaTransactionManager#setTransactionManager
 * @see com.ibm.ws.Transaction.TransactionManagerFactory#getTransactionManager
 * @see WebSphereUowTransactionManager
 */
public class WebSphereTransactionManagerFactoryBean implements FactoryBean {

	private static final String FACTORY_CLASS_5_1 = "com.ibm.ws.Transaction.TransactionManagerFactory";


	protected final Log logger = LogFactory.getLog(getClass());

	private final TransactionManager transactionManager;


	/**
	 * This constructor retrieves the WebSphere TransactionManager factory class,
	 * so we can get access to the JTA TransactionManager.
	 */
	public WebSphereTransactionManagerFactoryBean() throws TransactionSystemException {
		try {
			// Using the thread context class loader for compatibility with the WSAD test server.
			Class clazz = Thread.currentThread().getContextClassLoader().loadClass(FACTORY_CLASS_5_1);
			Method method = clazz.getMethod("getTransactionManager", (Class[]) null);
			this.transactionManager = (TransactionManager) method.invoke(null, (Object[]) null);
		}
		catch (ClassNotFoundException ex) {
			throw new TransactionSystemException(
					"Could not find WebSphere 5.1/6.0/6.1 TransactionManager factory class", ex);
		}
		catch (InvocationTargetException ex) {
			throw new TransactionSystemException(
					"WebSphere's TransactionManagerFactory.getTransactionManager method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new TransactionSystemException(
					"Could not access WebSphere's TransactionManagerFactory.getTransactionManager method", ex);
		}
	}
	

	public Object getObject() {
		return this.transactionManager;
	}

	public Class getObjectType() {
		return this.transactionManager.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

}
