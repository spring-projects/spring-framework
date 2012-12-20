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

package org.springframework.web.servlet.view.tiles;

import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryConfig;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.TilesUtil;
import org.apache.struts.tiles.xmlDefinition.I18nFactorySet;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * Helper class to configure Tiles 1.x for the Spring Framework. See
 * <a href="http://struts.apache.org">http://struts.apache.org</a>
 * for more information about Struts Tiles, which basically is a templating
 * mechanism for JSP-based web applications.
 *
 * <p><b>NOTE:</b> This TilesConfigurer class supports Tiles 1.x,
 * a.k.a. "Struts Tiles", which comes as part of Struts 1.x.
 * For Tiles 2.x support, check out
 * {@link org.springframework.web.servlet.view.tiles2.TilesConfigurer}.
 *
 * <p>The TilesConfigurer simply configures a Tiles DefinitionsFactory using
 * a set of files containing definitions, to be accessed by {@link TilesView}
 * instances.
 *
 * <p>TilesViews can be managed by any {@link org.springframework.web.servlet.ViewResolver}.
 * For simple convention-based view resolution, consider using
 * {@link org.springframework.web.servlet.view.UrlBasedViewResolver} with the
 * "viewClass" property set to "org.springframework.web.servlet.view.tiles.TilesView".
 *
 * <p>A typical TilesConfigurer bean definition looks as follows:
 *
 * <pre>
 * &lt;bean id="tilesConfigurer" class="org.springframework.web.servlet.view.tiles.TilesConfigurer">
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
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @see TilesView
 * @see org.springframework.web.servlet.view.UrlBasedViewResolver
 * @deprecated as of Spring 3.0
 */
@Deprecated
public class TilesConfigurer extends WebApplicationObjectSupport implements InitializingBean {

	/** Definition URLs mapped to descriptions */
	private String[] definitions;

	/** Validate the Tiles definitions? */
	private boolean validateDefinitions = true;

	/** Factory class for Tiles */
	private Class<?> factoryClass = I18nFactorySet.class;


	/**
	 * Set the Tiles definitions, i.e. the list of files containing the definitions.
	 */
	public void setDefinitions(String[] definitions) {
		this.definitions = definitions;
	}

	/**
	 * Set whether to validate the Tiles XML definitions. Default is "true".
	 */
	public void setValidateDefinitions(boolean validateDefinitions) {
		this.validateDefinitions = validateDefinitions;
	}

	/**
	 * Set the factory class for Tiles. Default is I18nFactorySet.
	 * @see org.apache.struts.tiles.xmlDefinition.I18nFactorySet
	 */
	public void setFactoryClass(Class<?> factoryClass) {
		this.factoryClass = factoryClass;
	}


	/**
	 * Initialize the Tiles definition factory.
	 * Delegates to createDefinitionsFactory for the actual creation.
	 * @throws DefinitionsFactoryException if an error occurs
	 * @see #createDefinitionsFactory
	 */
	public void afterPropertiesSet() throws DefinitionsFactoryException {
		logger.debug("TilesConfigurer: initializion started");

		// initialize the configuration for the definitions factory
		DefinitionsFactoryConfig factoryConfig = new DefinitionsFactoryConfig();
		factoryConfig.setFactoryName("");
		factoryConfig.setFactoryClassname(this.factoryClass.getName());
		factoryConfig.setParserValidate(this.validateDefinitions);

		if (this.definitions != null) {
			String defs = StringUtils.arrayToCommaDelimitedString(this.definitions);
			if (logger.isInfoEnabled()) {
				logger.info("TilesConfigurer: adding definitions [" + defs + "]");
			}
			factoryConfig.setDefinitionConfigFiles(defs);
		}

		// initialize the definitions factory
		createDefinitionsFactory(factoryConfig);

		logger.debug("TilesConfigurer: initialization completed");
	}

	/**
	 * Create the Tiles DefinitionsFactory and expose it to the ServletContext.
	 * @param factoryConfig the configuration for the DefinitionsFactory
	 * @return the DefinitionsFactory
	 * @throws DefinitionsFactoryException if an error occurs
	 */
	protected DefinitionsFactory createDefinitionsFactory(DefinitionsFactoryConfig factoryConfig)
			throws DefinitionsFactoryException {

		return TilesUtil.createDefinitionsFactory(getServletContext(), factoryConfig);
	}

}
