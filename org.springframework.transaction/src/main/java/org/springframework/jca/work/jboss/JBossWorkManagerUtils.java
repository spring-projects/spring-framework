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

package org.springframework.jca.work.jboss;

import java.lang.reflect.Method;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.resource.spi.work.WorkManager;

/**
 * Utility class for obtaining the JBoss JCA WorkManager,
 * typically for use in web applications.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public abstract class JBossWorkManagerUtils {

	private static final String JBOSS_WORK_MANAGER_MBEAN_CLASS_NAME = "org.jboss.resource.work.JBossWorkManagerMBean";

	private static final String MBEAN_SERVER_CONNECTION_JNDI_NAME = "jmx/invoker/RMIAdaptor";

	private static final String WORK_MANAGER_OBJECT_NAME = "jboss.jca:service=WorkManager";


	/**
	 * Obtain the default JBoss JCA WorkManager through a JMX lookup
	 * for the JBossWorkManagerMBean.
	 * @see org.jboss.resource.work.JBossWorkManagerMBean
	 */
	public static WorkManager getWorkManager() {
		try {
			Class mbeanClass = JBossWorkManagerUtils.class.getClassLoader().loadClass(JBOSS_WORK_MANAGER_MBEAN_CLASS_NAME);
			InitialContext jndiContext = new InitialContext();
			MBeanServerConnection mconn = (MBeanServerConnection) jndiContext.lookup(MBEAN_SERVER_CONNECTION_JNDI_NAME);
			ObjectName objectName = ObjectName.getInstance(WORK_MANAGER_OBJECT_NAME);
			Object workManagerMBean = MBeanServerInvocationHandler.newProxyInstance(mconn, objectName, mbeanClass, false);
			Method getInstanceMethod = workManagerMBean.getClass().getMethod("getInstance", new Class[0]);
			return (WorkManager) getInstanceMethod.invoke(workManagerMBean, new Object[0]);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize JBossWorkManagerTaskExecutor because JBoss API is not available: " + ex);
		}
	}

}
