/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;
import javax.naming.NamingException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiTemplate;

/**
 * Service exporter which binds RMI services to JNDI.
 * Typically used for RMI-IIOP (CORBA).
 *
 * <p>Exports services via the {@link javax.rmi.PortableRemoteObject} class.
 * You need to run "rmic" with the "-iiop" option to generate corresponding
 * stubs and skeletons for each exported service.
 *
 * <p>Also supports exposing any non-RMI service via RMI invokers, to be accessed
 * via {@link JndiRmiClientInterceptor} / {@link JndiRmiProxyFactoryBean}'s
 * automatic detection of such invokers.
 *
 * <p>With an RMI invoker, RMI communication works on the {@link RmiInvocationHandler}
 * level, needing only one stub for any service. Service interfaces do not have to
 * extend {@code java.rmi.Remote} or throw {@code java.rmi.RemoteException}
 * on all methods, but in and out parameters have to be serializable.
 *
 * <p>The JNDI environment can be specified as "jndiEnvironment" bean property,
 * or be configured in a {@code jndi.properties} file or as system properties.
 * For example:
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props>
 *		 &lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		 &lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setService
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setJndiName
 * @see JndiRmiClientInterceptor
 * @see JndiRmiProxyFactoryBean
 * @see javax.rmi.PortableRemoteObject#exportObject
 */
public class JndiRmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	private JndiTemplate jndiTemplate = new JndiTemplate();

	private String jndiName;

	private Remote exportedObject;


	/**
	 * Set the JNDI template to use for JNDI lookups.
	 * You can also specify JNDI environment settings via "jndiEnvironment".
	 * @see #setJndiEnvironment
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * Set the JNDI environment to use for JNDI lookups.
	 * Creates a JndiTemplate with the given environment settings.
	 * @see #setJndiTemplate
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * Set the JNDI name of the exported RMI service.
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}


	@Override
	public void afterPropertiesSet() throws NamingException, RemoteException {
		prepare();
	}

	/**
	 * Initialize this service exporter, binding the specified service to JNDI.
	 * @throws NamingException if service binding failed
	 * @throws RemoteException if service export failed
	 */
	public void prepare() throws NamingException, RemoteException {
		if (this.jndiName == null) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}

		// Initialize and cache exported object.
		this.exportedObject = getObjectToExport();

		rebind();
	}

	/**
	 * Rebind the specified service to JNDI, for recovering in case
	 * of the target registry having been restarted.
	 * @throws NamingException if service binding failed
	 */
	public void rebind() throws NamingException {
		if (logger.isInfoEnabled()) {
			logger.info("Binding RMI service to JNDI location [" + this.jndiName + "]");
		}
		this.jndiTemplate.rebind(this.jndiName, this.exportedObject);
	}

	/**
	 * Unbind the RMI service from JNDI on bean factory shutdown.
	 */
	@Override
	public void destroy() throws NamingException, NoSuchObjectException {
		if (logger.isInfoEnabled()) {
			logger.info("Unbinding RMI service from JNDI location [" + this.jndiName + "]");
		}
		this.jndiTemplate.unbind(this.jndiName);
	}

}
