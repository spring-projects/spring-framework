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

package org.springframework.web.servlet.view.tiles2;

import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.context.ChainedTilesContextFactory;
import org.apache.tiles.definition.UrlDefinitionsFactory;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.factory.TilesContainerFactory;
import org.apache.tiles.impl.BasicTilesContainer;
import org.apache.tiles.jsp.context.JspTilesContextFactory;
import org.apache.tiles.preparer.BasicPreparerFactory;
import org.apache.tiles.servlet.context.ServletTilesContextFactory;
import org.apache.tiles.web.util.ServletContextAdapter;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * Helper class to configure Tiles2 for the Spring Framework. See
 * <a href="http://tiles.apache.org">http://tiles.apache.org</a>
 * for more information about Tiles, which basically is a templating
 * mechanism for JSP-based web applications.
 *
 * <p>The TilesConfigurer simply configures a TilesContainer using a set
 * of files containing definitions, to be accessed by {@link TilesView}
 * instances.
 *
 * <p>TilesViews can be managed by any {@link org.springframework.web.servlet.ViewResolver}.
 * For simple convention-based view resolution, consider using
 * {@link org.springframework.web.servlet.view.UrlBasedViewResolver} with the
 * "viewClass" property set to "org.springframework.web.servlet.view.tiles2.TilesView".
 *
 * <p>A typical TilesConfigurer bean definition looks as follows:
 *
 * <pre>
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles2.TilesConfigurer">
 *   &lt;property name="definitions">
 *     &lt;list>
 *       &lt;value>/WEB-INF/defs/general.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/widgets.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/administrator.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/customer.xml&lt;/value>
 *       &lt;value>/WEB-INF/defs/templates.xml&lt;/value>
 *     &lt;/list>
 *   &lt;/property>
 * &lt;/bean></pre>
 *
 * The values in the list are the actual files containing the definitions.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see TilesView
 * @see org.springframework.web.servlet.view.UrlBasedViewResolver
 */
public class TilesConfigurer implements ServletContextAware, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Properties tilesPropertyMap = new Properties();

	private ServletContext servletContext;


	public TilesConfigurer() {
		this.tilesPropertyMap.put(
				TilesContainerFactory.CONTAINER_FACTORY_INIT_PARAM,
				TilesContainerFactory.class.getName());
		this.tilesPropertyMap.put(
				TilesContainerFactory.CONTEXT_FACTORY_INIT_PARAM,
				ChainedTilesContextFactory.class.getName());
		this.tilesPropertyMap.put(
				TilesContainerFactory.DEFINITIONS_FACTORY_INIT_PARAM,
				UrlDefinitionsFactory.class.getName());
		this.tilesPropertyMap.put(
				TilesContainerFactory.PREPARER_FACTORY_INIT_PARAM,
				BasicPreparerFactory.class.getName());
		this.tilesPropertyMap.put(
				ChainedTilesContextFactory.FACTORY_CLASS_NAMES,
				ServletTilesContextFactory.class.getName() + "," + JspTilesContextFactory.class.getName());
		this.tilesPropertyMap.put(
				UrlDefinitionsFactory.LOCALE_RESOLVER_IMPL_PROPERTY,
				SpringLocaleResolver.class.getName());
	}


	/**
	 * Set the Tiles definitions, i.e. the list of files containing the definitions.
	 * Default is "/WEB-INF/tiles.xml".
	 */
	public void setDefinitions(String[] definitions) {
		if (definitions != null) {
			String defs = StringUtils.arrayToCommaDelimitedString(definitions);
			if (logger.isInfoEnabled()) {
				logger.info("TilesConfigurer: adding definitions [" + defs + "]");
			}
			this.tilesPropertyMap.put(BasicTilesContainer.DEFINITIONS_CONFIG, defs);
		}
	}

	/**
	 * Set whether to validate the Tiles XML definitions. Default is "true".
	 */
	public void setValidateDefinitions(boolean validateDefinitions) {
		this.tilesPropertyMap.put(DigesterDefinitionsReader.PARSER_VALIDATE_PARAMETER_NAME,
				Boolean.toString(validateDefinitions));
	}

	/**
	 * Set the {@link org.apache.tiles.definition.DefinitionsFactory} implementation to use.
	 * Default is {@link org.apache.tiles.definition.UrlDefinitionsFactory},
	 * operating on definition resource URLs.
	 * <p>Specify a custom DefinitionsFactory, e.g. a UrlDefinitionsFactory subclass,
	 * to customize the creation of Tiles Definition objects. Note that such a
	 * DefinitionsFactory has to be able to handle {@link java.net.URL} source objects,
	 * unless you configure a different TilesContainerFactory.
	 */
	public void setDefinitionsFactoryClass(Class definitionsFactoryClass) {
		this.tilesPropertyMap.put(TilesContainerFactory.DEFINITIONS_FACTORY_INIT_PARAM,
				definitionsFactoryClass.getName());
	}

	/**
	 * Set the {@link org.apache.tiles.preparer.PreparerFactory} implementation to use.
	 * Default is {@link org.apache.tiles.preparer.BasicPreparerFactory}, creating
	 * shared instances for specified preparer classes.
	 * <p>Specify {@link SimpleSpringPreparerFactory} to autowire
	 * {@link org.apache.tiles.preparer.ViewPreparer} instances based on specified
	 * preparer classes, applying Spring's container callbacks as well as applying
	 * configured Spring BeanPostProcessors. If Spring's context-wide annotation-config
	 * has been activated, annotations in ViewPreparer classes will be automatically
	 * detected and applied.
	 * <p>Specify {@link SpringBeanPreparerFactory} to operate on specified preparer
	 * <i>names</i> instead of classes, obtaining the corresponding Spring bean from
	 * the DispatcherServlet's application context. The full bean creation process
	 * will be in the control of the Spring application context in this case,
	 * allowing for the use of scoped beans etc. Note that you need to define one
	 * Spring bean definition per preparer name (as used in your Tiles definitions).
	 * @see SimpleSpringPreparerFactory
	 * @see SpringBeanPreparerFactory
	 */
	public void setPreparerFactoryClass(Class preparerFactoryClass) {
		this.tilesPropertyMap.put(TilesContainerFactory.PREPARER_FACTORY_INIT_PARAM,
				preparerFactoryClass.getName());
	}

	/**
	 * Set whether to use a MutableTilesContainer for this application.
	 * Default is "false".
	 */
	public void setUseMutableTilesContainer(boolean useMutableTilesContainer) {
		this.tilesPropertyMap.put(TilesContainerFactory.CONTAINER_FACTORY_MUTABLE_INIT_PARAM,
				Boolean.toString(useMutableTilesContainer));
	}

	/**
	 * Set Tiles properties (equivalent to the ServletContext init-params in
	 * the Tiles documentation), overriding the default settings.
	 */
	public void setTilesProperties(Properties tilesProperties) {
		CollectionUtils.mergePropertiesIntoMap(tilesProperties, this.tilesPropertyMap);
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	/**
	 * Creates and exposes a TilesContainer for this web application.
	 * @throws TilesException in case of setup failure
	 */
	public void afterPropertiesSet() throws TilesException {
		TilesContainer container = createTilesContainer(this.servletContext);
		TilesAccess.setContainer(this.servletContext, container);
	}

	/**
	 * Create a TilesContainer for this web application.
	 * @param context this web application's ServletContext
	 * @return the TilesContainer to expose
	 * @throws TilesException in case of setup failure
	 */
	protected TilesContainer createTilesContainer(ServletContext context) throws TilesException {
		ServletContextAdapter adaptedContext = new ServletContextAdapter(new DelegatingServletConfig());
		TilesContainerFactory factory = TilesContainerFactory.getFactory(adaptedContext);
		return factory.createContainer(adaptedContext);
	}

	/**
	 * Removes the TilesContainer from this web application.
	 * @throws TilesException in case of cleanup failure
	 */
	public void destroy() throws TilesException {
		TilesAccess.setContainer(this.servletContext, null);
	}


	/**
	 * Internal implementation of the ServletConfig interface, to be passed
	 * to the wrapped servlet. Delegates to ServletWrappingController fields
	 * and methods to provide init parameters and other environment info.
	 */
	private class DelegatingServletConfig implements ServletConfig {

		public String getServletName() {
			return "TilesConfigurer";
		}

		public ServletContext getServletContext() {
			return servletContext;
		}

		public String getInitParameter(String paramName) {
			return tilesPropertyMap.getProperty(paramName);
		}

		public Enumeration getInitParameterNames() {
			return tilesPropertyMap.keys();
		}
	}

}
