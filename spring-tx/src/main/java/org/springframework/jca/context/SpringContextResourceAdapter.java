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

package org.springframework.jca.context;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

/**
 * JCA 1.5 {@link javax.resource.spi.ResourceAdapter} implementation
 * that loads a Spring {@link org.springframework.context.ApplicationContext},
 * starting and stopping Spring-managed beans as part of the ResourceAdapter's
 * lifecycle.
 *
 * <p>Ideal for application contexts that do not need any HTTP entry points
 * but rather just consist of message endpoints and scheduled jobs etc.
 * Beans in such a context may use application server resources such as the
 * JTA transaction manager and JNDI-bound JDBC DataSources and JMS
 * ConnectionFactory instances, and may also register with the platform's
 * JMX server - all through Spring's standard transaction management and
 * JNDI and JMX support facilities.
 *
 * <p>If the need for scheduling asynchronous work arises, consider using
 * Spring's {@link org.springframework.jca.work.WorkManagerTaskExecutor}
 * as a standard bean definition, to be injected into application beans
 * through dependency injection. This WorkManagerTaskExecutor will automatically
 * use the JCA WorkManager from the BootstrapContext that has been provided
 * to this ResourceAdapter.
 *
 * <p>The JCA {@link javax.resource.spi.BootstrapContext} may also be
 * accessed directly, through application components that implement the
 * {@link BootstrapContextAware} interface. When deployed using this
 * ResourceAdapter, the BootstrapContext is guaranteed to be passed on
 * to such components.
 *
 * <p>This ResourceAdapter is to be defined in a "META-INF/ra.xml" file
 * within a J2EE ".rar" deployment unit like as follows:
 *
 * <pre class="code">
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;connector xmlns="http://java.sun.com/xml/ns/j2ee"
 *		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *		 xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/connector_1_5.xsd"
 *		 version="1.5"&gt;
 *	 &lt;vendor-name&gt;Spring Framework&lt;/vendor-name&gt;
 *	 &lt;eis-type&gt;Spring Connector&lt;/eis-type&gt;
 *	 &lt;resourceadapter-version&gt;1.0&lt;/resourceadapter-version&gt;
 *	 &lt;resourceadapter&gt;
 *		 &lt;resourceadapter-class&gt;org.springframework.jca.context.SpringContextResourceAdapter&lt;/resourceadapter-class&gt;
 *		 &lt;config-property&gt;
 *			 &lt;config-property-name&gt;ContextConfigLocation&lt;/config-property-name&gt;
 *			 &lt;config-property-type&gt;java.lang.String&lt;/config-property-type&gt;
 *			 &lt;config-property-value&gt;META-INF/applicationContext.xml&lt;/config-property-value&gt;
 *		 &lt;/config-property&gt;
 *	 &lt;/resourceadapter&gt;
 * &lt;/connector&gt;</pre>
 *
 * Note that "META-INF/applicationContext.xml" is the default context config
 * location, so it doesn't have to specified unless you intend to specify
 * different/additional config files. So in the default case, you may remove
 * the entire {@code config-property} section above.
 *
 * <p><b>For simple deployment needs, all you need to do is the following:</b>
 * Package all application classes into a RAR file (which is just a standard
 * JAR file with a different file extension), add all required library jars
 * into the root of the RAR archive, add a "META-INF/ra.xml" deployment
 * descriptor as shown above as well as the corresponding Spring XML bean
 * definition file(s) (typically "META-INF/applicationContext.xml"),
 * and drop the resulting RAR file into your application server's
 * deployment directory!
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setContextConfigLocation
 * @see #loadBeanDefinitions
 * @see ResourceAdapterApplicationContext
 */
public class SpringContextResourceAdapter implements ResourceAdapter {

	/**
	 * Any number of these characters are considered delimiters between
	 * multiple context config paths in a single String value.
	 * @see #setContextConfigLocation
	 */
	public static final String CONFIG_LOCATION_DELIMITERS = ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS;

	public static final String DEFAULT_CONTEXT_CONFIG_LOCATION = "META-INF/applicationContext.xml";


	protected final Log logger = LogFactory.getLog(getClass());

	private String contextConfigLocation = DEFAULT_CONTEXT_CONFIG_LOCATION;

	private ConfigurableApplicationContext applicationContext;


	/**
	 * Set the location of the context configuration files, within the
	 * resource adapter's deployment unit. This can be a delimited
	 * String that consists of multiple resource location, separated
	 * by commas, semicolons, whitespace, or line breaks.
	 * <p>This can be specified as "ContextConfigLocation" config
	 * property in the {@code ra.xml} deployment descriptor.
	 * <p>The default is "classpath:META-INF/applicationContext.xml".
	 */
	public void setContextConfigLocation(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * Return the specified context configuration files.
	 */
	protected String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * Return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * This implementation loads a Spring ApplicationContext through the
	 * {@link #createApplicationContext} template method.
	 */
	@Override
	public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
		if (logger.isInfoEnabled()) {
			logger.info("Starting SpringContextResourceAdapter with BootstrapContext: " + bootstrapContext);
		}
		this.applicationContext = createApplicationContext(bootstrapContext);
	}

	/**
	 * Build a Spring ApplicationContext for the given JCA BootstrapContext.
	 * <p>The default implementation builds a {@link ResourceAdapterApplicationContext}
	 * and delegates to {@link #loadBeanDefinitions} for actually parsing the
	 * specified configuration files.
	 * @param bootstrapContext this ResourceAdapter's BootstrapContext
	 * @return the Spring ApplicationContext instance
	 */
	protected ConfigurableApplicationContext createApplicationContext(BootstrapContext bootstrapContext) {
		ResourceAdapterApplicationContext applicationContext =
				new ResourceAdapterApplicationContext(bootstrapContext);
		// Set ResourceAdapter's ClassLoader as bean class loader.
		applicationContext.setClassLoader(getClass().getClassLoader());
		// Extract individual config locations.
		String[] configLocations =
				StringUtils.tokenizeToStringArray(getContextConfigLocation(), CONFIG_LOCATION_DELIMITERS);
		if (configLocations != null) {
			loadBeanDefinitions(applicationContext, configLocations);
		}
		applicationContext.refresh();
		return applicationContext;
	}

	/**
	 * Load the bean definitions into the given registry,
	 * based on the specified configuration files.
	 * @param registry the registry to load into
	 * @param configLocations the parsed config locations
	 * @see #setContextConfigLocation
	 */
	protected void loadBeanDefinitions(BeanDefinitionRegistry registry, String[] configLocations) {
		new XmlBeanDefinitionReader(registry).loadBeanDefinitions(configLocations);
	}

	/**
	 * This implementation closes the Spring ApplicationContext.
	 */
	@Override
	public void stop() {
		logger.info("Stopping SpringContextResourceAdapter");
		this.applicationContext.close();
	}


	/**
	 * This implementation always throws a NotSupportedException.
	 */
	@Override
	public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec)
			throws ResourceException {

		throw new NotSupportedException("SpringContextResourceAdapter does not support message endpoints");
	}

	/**
	 * This implementation does nothing.
	 */
	@Override
	public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
	}

	/**
	 * This implementation always returns {@code null}.
	 */
	@Override
	public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException {
		return null;
	}

}
