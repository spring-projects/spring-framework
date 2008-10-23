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

package org.springframework.transaction.jta;

import javax.naming.NamingException;
import javax.transaction.SystemException;

import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * {@link FactoryBean} that retrieves the JTA UserTransaction/TransactionManager
 * for ObjectWeb's <a href="http://jotm.objectweb.org">JOTM</a>. Will retrieve
 * an already active JOTM instance if found (e.g. if running in JOnAS),
 * else create a new local JOTM instance.
 *
 * <p>With JOTM, the same object implements both the
 * {@link javax.transaction.UserTransaction} and the
 * {@link javax.transaction.TransactionManager} interface,
 * as returned by this FactoryBean.
 *
 * <p>A local JOTM instance is well-suited for working in conjunction with
 * ObjectWeb's <a href="http://xapool.experlog.com">XAPool</a>, e.g. with bean
 * definitions like the following:
 *
 * <pre class="code">
 * &lt;bean id="jotm" class="org.springframework.transaction.jta.JotmFactoryBean"/&gt;
 *
 * &lt;bean id="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager"&gt;
 *   &lt;property name="userTransaction" ref="jotm"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="innerDataSource" class="org.enhydra.jdbc.standard.StandardXADataSource" destroy-method="shutdown"&gt;
 *   &lt;property name="transactionManager" ref="jotm"/&gt;
 *   &lt;property name="driverName" value="..."/&gt;
 *   &lt;property name="url" value="..."/&gt;
 *   &lt;property name="user" value="..."/&gt;
 *   &lt;property name="password" value="..."/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="dataSource" class="org.enhydra.jdbc.pool.StandardXAPoolDataSource" destroy-method="shutdown"&gt;
 *   &lt;property name="dataSource" ref="innerDataSource"/&gt;
 *   &lt;property name="user" value="..."/&gt;
 *   &lt;property name="password" value="..."/&gt;
 *   &lt;property name="maxSize" value="..."/&gt;
 * &lt;/bean&gt;</pre>
 *
 * Note that Spring's {@link JtaTransactionManager} will automatically detect
 * that the passed-in UserTransaction reference also implements the
 * TransactionManager interface. Hence, it is not necessary to specify a
 * separate reference for JtaTransactionManager's "transactionManager" property.
 *
 * <p>Implementation note: This FactoryBean uses JOTM's static access method
 * to obtain the JOTM {@link org.objectweb.jotm.Current} object, which
 * implements both the UserTransaction and the TransactionManager interface,
 * as mentioned above.
 *
 * @author Juergen Hoeller
 * @since 21.01.2004
 * @see JtaTransactionManager#setUserTransaction
 * @see JtaTransactionManager#setTransactionManager
 * @see org.objectweb.jotm.Current
 */
public class JotmFactoryBean implements FactoryBean, DisposableBean {

	private Current jotmCurrent;

	private Jotm jotm;


	public JotmFactoryBean() throws NamingException {
		// Check for already active JOTM instance.
		this.jotmCurrent = Current.getCurrent();

		// If none found, create new local JOTM instance.
		if (this.jotmCurrent == null) {
			// Only for use within the current Spring context:
			// local, not bound to registry.
			this.jotm = new Jotm(true, false);
			this.jotmCurrent = Current.getCurrent();
		}
	}

	/**
	 * Set the default transaction timeout for the JOTM instance.
	 * <p>Should only be called for a local JOTM instance,
	 * not when accessing an existing (shared) JOTM instance.
	 */
	public void setDefaultTimeout(int defaultTimeout) {
		this.jotmCurrent.setDefaultTimeout(defaultTimeout);
		// The following is a JOTM oddity: should be used for demarcation transaction only,
		// but is required here in order to actually get rid of JOTM's default (60 seconds).
		try {
			this.jotmCurrent.setTransactionTimeout(defaultTimeout);
		}
		catch (SystemException ex) {
			// should never happen
		}
	}


	/**
	 * Return the JOTM instance created by this factory bean, if any.
	 * Will be <code>null</code> if an already active JOTM instance is used.
	 * <p>Application code should never need to access this.
	 */
	public Jotm getJotm() {
		return this.jotm;
	}

	public Object getObject() {
		return this.jotmCurrent;
	}

	public Class getObjectType() {
		return this.jotmCurrent.getClass();
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Stop the local JOTM instance, if created by this FactoryBean.
	 */
	public void destroy() {
		if (this.jotm != null) {
			this.jotm.stop();
		}
	}

}
