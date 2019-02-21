/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.transaction.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * A {@link FactoryBean} equivalent to the &lt;tx:jta-transaction-manager/&gt; XML element,
 * autodetecting WebLogic and WebSphere servers and exposing the corresponding
 * {@link org.springframework.transaction.jta.JtaTransactionManager} subclass.
 *
 * @author Juergen Hoeller
 * @since 4.1.1
 * @see org.springframework.transaction.jta.WebLogicJtaTransactionManager
 * @see org.springframework.transaction.jta.WebSphereUowTransactionManager
 */
public class JtaTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager> {

	private static final String WEBLOGIC_JTA_TRANSACTION_MANAGER_CLASS_NAME =
			"org.springframework.transaction.jta.WebLogicJtaTransactionManager";

	private static final String WEBSPHERE_TRANSACTION_MANAGER_CLASS_NAME =
			"org.springframework.transaction.jta.WebSphereUowTransactionManager";

	private static final String JTA_TRANSACTION_MANAGER_CLASS_NAME =
			"org.springframework.transaction.jta.JtaTransactionManager";


	private static final boolean weblogicPresent;

	private static final boolean webspherePresent;

	static {
		ClassLoader classLoader = JtaTransactionManagerFactoryBean.class.getClassLoader();
		weblogicPresent = ClassUtils.isPresent("weblogic.transaction.UserTransaction", classLoader);
		webspherePresent = ClassUtils.isPresent("com.ibm.wsspi.uow.UOWManager", classLoader);
	}


	@Nullable
	private final JtaTransactionManager transactionManager;


	@SuppressWarnings("unchecked")
	public JtaTransactionManagerFactoryBean() {
		String className = resolveJtaTransactionManagerClassName();
		try {
			Class<? extends JtaTransactionManager> clazz = (Class<? extends JtaTransactionManager>)
					ClassUtils.forName(className, JtaTransactionManagerFactoryBean.class.getClassLoader());
			this.transactionManager = BeanUtils.instantiateClass(clazz);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load JtaTransactionManager class: " + className, ex);
		}
	}


	@Override
	@Nullable
	public JtaTransactionManager getObject() {
		return this.transactionManager;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.transactionManager != null ? this.transactionManager.getClass() : JtaTransactionManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	static String resolveJtaTransactionManagerClassName() {
		if (weblogicPresent) {
			return WEBLOGIC_JTA_TRANSACTION_MANAGER_CLASS_NAME;
		}
		else if (webspherePresent) {
			return WEBSPHERE_TRANSACTION_MANAGER_CLASS_NAME;
		}
		else {
			return JTA_TRANSACTION_MANAGER_CLASS_NAME;
		}
	}

}
