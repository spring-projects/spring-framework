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

package org.springframework.jmx.support;

import java.lang.reflect.InvocationTargetException;

import javax.management.MBeanServer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;

/**
 * FactoryBean that obtains a specified WebLogic {@link javax.management.MBeanServer}
 * reference through WebLogic's proprietary <code>Helper</code> /
 * <code>MBeanHome</code> API, which is available on WebLogic 6.1 and higher.
 *
 * <p>Exposes the <code>MBeanServer</code> for bean references.
 * This FactoryBean is a direct alternative to {@link MBeanServerFactoryBean},
 * which uses standard JMX 1.2 API to access the platform's MBeanServer.
 *
 * <p>Note: There is also a {@link WebLogicJndiMBeanServerFactoryBean} for
 * accessing the WebLogic <code>MBeanServer</code> instance through a WebLogic
 * <code>MBeanHome</code> obtained via a JNDI lookup, typical a local one.
 *
 * <p><b>NOTE: This class is only intended for use with WebLogic up to 8.1.</b>
 * On WebLogic 9.x, simply obtain the MBeanServer directly from the JNDI location
 * "java:comp/env/jmx/runtime", for example through the following configuration:
 *
 * <pre>
 * &lt;bean class="org.springframework.jndi.JndiObjectFactoryBean"&gt;
 *   &lt;property name="jndiName" value="java:comp/env/jmx/runtime"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see weblogic.management.Helper#getMBeanHome(String, String, String, String)
 * @see weblogic.management.MBeanHome#getMBeanServer()
 * @see javax.management.MBeanServer
 * @see MBeanServerFactoryBean
 * @see WebLogicJndiMBeanServerFactoryBean
 */
public class WebLogicMBeanServerFactoryBean implements FactoryBean, InitializingBean {

	private static final String WEBLOGIC_JMX_HELPER_CLASS = "weblogic.management.Helper";

	private static final String GET_MBEAN_HOME_METHOD = "getMBeanHome";

	private static final String GET_MBEAN_SERVER_METHOD = "getMBeanServer";


	private String username = "weblogic";

	private String password = "weblogic";

	private String serverUrl = "t3://localhost:7001";

	private String serverName = "server";

	private MBeanServer mbeanServer;


	/**
	 * Set the username to use for retrieving the WebLogic MBeanServer.
	 * Default is "weblogic".
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the password to use for retrieving the WebLogic MBeanServer.
	 * Default is "weblogic".
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Set the server URL to use for retrieving the WebLogic MBeanServer.
	 * Default is "t3://localhost:7001".
	 */
	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	/**
	 * Set the server name to use for retrieving the WebLogic MBeanServer.
	 * Default is "server".
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}


	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		try {
			/*
			 * MBeanHome mbeanHome = Helper.getMBeanHome(this.username, this.password, this.serverUrl, this.serverName);
			 */
			Class helperClass = getClass().getClassLoader().loadClass(WEBLOGIC_JMX_HELPER_CLASS);
			Class[] argTypes = new Class[] {String.class, String.class, String.class, String.class};
			Object[] args = new Object[] {this.username, this.password, this.serverUrl, this.serverName};
			Object mbeanHome = helperClass.getMethod(GET_MBEAN_HOME_METHOD, argTypes).invoke(null, args);

			/*
			 * this.mbeanServer = mbeanHome.getMBeanServer();
			 */
			this.mbeanServer = (MBeanServer)
					mbeanHome.getClass().getMethod(GET_MBEAN_SERVER_METHOD, null).invoke(mbeanHome, null);
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanServerNotFoundException("Could not find WebLogic's JMX Helper class", ex);
		}
		catch (InvocationTargetException ex) {
			throw new MBeanServerNotFoundException(
					"WebLogic's JMX Helper.getMBeanHome/getMBeanServer method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new MBeanServerNotFoundException(
					"Could not access WebLogic's JMX Helper.getMBeanHome/getMBeanServer method", ex);
		}
	}


	public Object getObject() {
		return this.mbeanServer;
	}

	public Class getObjectType() {
		return (this.mbeanServer != null ? this.mbeanServer.getClass() : MBeanServer.class);
	}

	public boolean isSingleton() {
		return true;
	}

}
