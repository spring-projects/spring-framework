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

package org.springframework.web.servlet.view.tiles2;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.TilesApplicationContext;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.awareness.TilesApplicationContextAware;
import org.apache.tiles.context.AbstractTilesApplicationContextFactory;
import org.apache.tiles.context.ChainedTilesRequestContextFactory;
import org.apache.tiles.context.TilesRequestContextFactory;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.DefinitionsFactoryException;
import org.apache.tiles.definition.DefinitionsReader;
import org.apache.tiles.definition.Refreshable;
import org.apache.tiles.definition.dao.BaseLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.dao.CachingLocaleUrlDefinitionDAO;
import org.apache.tiles.definition.digester.DigesterDefinitionsReader;
import org.apache.tiles.evaluator.AttributeEvaluator;
import org.apache.tiles.evaluator.el.ELAttributeEvaluator;
import org.apache.tiles.evaluator.impl.DirectAttributeEvaluator;
import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.apache.tiles.factory.BasicTilesContainerFactory;
import org.apache.tiles.factory.TilesContainerFactory;
import org.apache.tiles.impl.BasicTilesContainer;
import org.apache.tiles.impl.mgmt.CachingTilesContainer;
import org.apache.tiles.locale.LocaleResolver;
import org.apache.tiles.preparer.BasicPreparerFactory;
import org.apache.tiles.preparer.PreparerFactory;
import org.apache.tiles.renderer.RendererFactory;
import org.apache.tiles.servlet.context.ServletUtil;
import org.apache.tiles.startup.BasicTilesInitializer;
import org.apache.tiles.startup.TilesInitializer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * Helper class to configure Tiles 2.x for the Spring Framework. See
 * <a href="http://tiles.apache.org">http://tiles.apache.org</a>
 * for more information about Tiles, which basically is a templating
 * mechanism for JSP-based web applications.
 *
 * <b>Note: Spring 3.0 requires Tiles 2.1.2 or above, with explicit support for Tiles 2.2.</b>
 * Tiles 2.1's EL support will be activated by default when running on JSP 2.1 or above
 * and when the Tiles EL module is present in the classpath.
 *
 * <p>The TilesConfigurer simply configures a TilesContainer using a set of files
 * containing definitions, to be accessed by {@link TilesView} instances. This is a
 * Spring-based alternative (for usage in Spring configuration) to the Tiles-provided
 * {@link org.apache.tiles.web.startup.TilesListener} (for usage in {@code web.xml}).
 *
 * <p>TilesViews can be managed by any {@link org.springframework.web.servlet.ViewResolver}.
 * For simple convention-based view resolution, consider using {@link TilesViewResolver}.
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
 * The values in the list are the actual Tiles XML files containing the definitions.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see TilesView
 * @see TilesViewResolver
 */
public class TilesConfigurer implements ServletContextAware, InitializingBean, DisposableBean {

	private static final boolean tilesElPresent =  // requires JSP 2.1 as well as Tiles EL module
			ClassUtils.isPresent(
			"javax.servlet.jsp.JspApplicationContext", TilesConfigurer.class.getClassLoader()) &&
			ClassUtils.isPresent(
			"org.apache.tiles.evaluator.el.ELAttributeEvaluator", TilesConfigurer.class.getClassLoader());

	private static final boolean tiles22Present = ClassUtils.isPresent(
			"org.apache.tiles.evaluator.AttributeEvaluatorFactory", TilesConfigurer.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	private TilesInitializer tilesInitializer;

	private boolean overrideLocaleResolver = false;

	private String[] definitions;

	private boolean checkRefresh = false;

	private boolean validateDefinitions = true;

	private Class<? extends DefinitionsFactory> definitionsFactoryClass;

	private Class<? extends PreparerFactory> preparerFactoryClass;

	private boolean useMutableTilesContainer = false;

	private final Map<String, String> tilesPropertyMap = new HashMap<String, String>();

	private ServletContext servletContext;


	public TilesConfigurer() {
		// Avoid Tiles 2.1 warn logging when default RequestContextFactory impl class not found
		StringBuilder sb = new StringBuilder("org.apache.tiles.servlet.context.ServletTilesRequestContextFactory");
		addClassNameIfPresent(sb, "org.apache.tiles.portlet.context.PortletTilesRequestContextFactory");
		addClassNameIfPresent(sb, "org.apache.tiles.jsp.context.JspTilesRequestContextFactory");
		this.tilesPropertyMap.put(ChainedTilesRequestContextFactory.FACTORY_CLASS_NAMES, sb.toString());

		// Register further Spring-specific settings which differ from the Tiles defaults
		this.tilesPropertyMap.put(AbstractTilesApplicationContextFactory.APPLICATION_CONTEXT_FACTORY_INIT_PARAM,
				SpringTilesApplicationContextFactory.class.getName());
		this.tilesPropertyMap.put(DefinitionsFactory.LOCALE_RESOLVER_IMPL_PROPERTY,
				SpringLocaleResolver.class.getName());
		this.tilesPropertyMap.put(TilesContainerFactory.PREPARER_FACTORY_INIT_PARAM,
				BasicPreparerFactory.class.getName());
		this.tilesPropertyMap.put(TilesContainerFactory.CONTAINER_FACTORY_MUTABLE_INIT_PARAM,
				Boolean.toString(false));
	}

	private static void addClassNameIfPresent(StringBuilder sb, String className) {
		if (ClassUtils.isPresent(className, TilesConfigurer.class.getClassLoader())) {
			sb.append(',').append(className);
		}
	}


	/**
	 * Configure Tiles using a custom TilesInitializer, typically specified as an inner bean.
	 * <p>Default is a variant of {@link org.apache.tiles.startup.DefaultTilesInitializer},
	 * respecting the "definitions", "preparerFactoryClass" etc properties on this configurer.
	 * <p><b>NOTE: Specifying a custom TilesInitializer effectively disables all other bean
	 * properties on this configurer.</b> The entire initialization procedure is then left
	 * to the TilesInitializer as specified.
	 */
	public void setTilesInitializer(TilesInitializer tilesInitializer) {
		this.tilesInitializer = tilesInitializer;
	}

	/**
	 * Specify whether to apply Tiles 2.2's "complete-autoload" configuration.
	 * <p>See {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory}
	 * for details on the complete-autoload mode.
	 * <p><b>NOTE: Specifying the complete-autoload mode effectively disables all other bean
	 * properties on this configurer.</b> The entire initialization procedure is then left
	 * to {@link org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer}.
	 * @see org.apache.tiles.extras.complete.CompleteAutoloadTilesContainerFactory
	 * @see org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer
	 */
	public void setCompleteAutoload(boolean completeAutoload) {
		if (completeAutoload) {
			try {
				Class clazz = getClass().getClassLoader().loadClass(
						"org.apache.tiles.extras.complete.CompleteAutoloadTilesInitializer");
				this.tilesInitializer = (TilesInitializer) clazz.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Tiles-Extras 2.2 not available", ex);
			}
		}
		else {
			this.tilesInitializer = null;
		}
		this.overrideLocaleResolver = completeAutoload;
	}

	/**
	 * Set the Tiles definitions, i.e. the list of files containing the definitions.
	 * Default is "/WEB-INF/tiles.xml".
	 */
	public void setDefinitions(String[] definitions) {
		this.definitions = definitions;
		if (definitions != null) {
			String defs = StringUtils.arrayToCommaDelimitedString(definitions);
			if (logger.isInfoEnabled()) {
				logger.info("TilesConfigurer: adding definitions [" + defs + "]");
			}
			this.tilesPropertyMap.put(DefinitionsFactory.DEFINITIONS_CONFIG, defs);
		}
		else {
			this.tilesPropertyMap.remove(DefinitionsFactory.DEFINITIONS_CONFIG);
		}
	}

	/**
	 * Set whether to check Tiles definition files for a refresh at runtime.
	 * Default is "false".
	 */
	public void setCheckRefresh(boolean checkRefresh) {
		this.checkRefresh = checkRefresh;
		this.tilesPropertyMap.put(CachingLocaleUrlDefinitionDAO.CHECK_REFRESH_INIT_PARAMETER,
				Boolean.toString(checkRefresh));
	}

	/**
	 * Set whether to validate the Tiles XML definitions. Default is "true".
	 */
	public void setValidateDefinitions(boolean validateDefinitions) {
		this.validateDefinitions = validateDefinitions;
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
	public void setDefinitionsFactoryClass(Class<? extends DefinitionsFactory> definitionsFactoryClass) {
		this.definitionsFactoryClass = definitionsFactoryClass;
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
	public void setPreparerFactoryClass(Class<? extends PreparerFactory> preparerFactoryClass) {
		this.preparerFactoryClass = preparerFactoryClass;
		this.tilesPropertyMap.put(TilesContainerFactory.PREPARER_FACTORY_INIT_PARAM,
				preparerFactoryClass.getName());
	}

	/**
	 * Set whether to use a MutableTilesContainer (typically the CachingTilesContainer
	 * implementation) for this application. Default is "false".
	 * @see org.apache.tiles.mgmt.MutableTilesContainer
	 * @see org.apache.tiles.mgmt.CachingTilesContainer
	 */
	public void setUseMutableTilesContainer(boolean useMutableTilesContainer) {
		this.useMutableTilesContainer = useMutableTilesContainer;
		this.tilesPropertyMap.put(TilesContainerFactory.CONTAINER_FACTORY_MUTABLE_INIT_PARAM,
				Boolean.toString(useMutableTilesContainer));
	}

	/**
	 * Set Tiles properties (equivalent to the ServletContext init-params in
	 * the Tiles documentation), overriding the default settings.
	 * <p><b>NOTE: This property is only effective with Tiles 2.1.</b>
	 * Tiles 2.2 doesn't support property-based configuration anymore.
	 */
	public void setTilesProperties(Properties tilesProperties) {
		CollectionUtils.mergePropertiesIntoMap(tilesProperties, this.tilesPropertyMap);
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	/**
	 * Creates and exposes a TilesContainer for this web application,
	 * delegating to the TilesInitializer.
	 * @throws TilesException in case of setup failure
	 * @see #createTilesInitializer()
	 */
	public void afterPropertiesSet() throws TilesException {
		boolean activateEl = false;
		if (tilesElPresent) {
			activateEl = new JspExpressionChecker().isExpressionFactoryAvailable();
			if (!this.tilesPropertyMap.containsKey(TilesContainerFactory.ATTRIBUTE_EVALUATOR_INIT_PARAM)) {
				this.tilesPropertyMap.put(TilesContainerFactory.ATTRIBUTE_EVALUATOR_INIT_PARAM, activateEl ?
						"org.apache.tiles.evaluator.el.ELAttributeEvaluator" : DirectAttributeEvaluator.class.getName());
			}
		}

		SpringTilesApplicationContextFactory factory = new SpringTilesApplicationContextFactory();
		factory.init(this.tilesPropertyMap);
		TilesApplicationContext preliminaryContext = factory.createApplicationContext(this.servletContext);
		if (this.tilesInitializer == null) {
			this.tilesInitializer = createTilesInitializer();
		}
		this.tilesInitializer.initialize(preliminaryContext);

		if (this.overrideLocaleResolver) {
			// We need to do this after initialization simply because we're reusing the
			// original CompleteAutoloadTilesInitializer above. We cannot subclass
			// CompleteAutoloadTilesInitializer when compiling against Tiles 2.1...
			logger.debug("Registering Tiles 2.2 LocaleResolver for complete-autoload setup");
			try {
				BasicTilesContainer container = (BasicTilesContainer) ServletUtil.getContainer(this.servletContext);
				DefinitionsFactory definitionsFactory = container.getDefinitionsFactory();
				Method setter = definitionsFactory.getClass().getMethod("setLocaleResolver", LocaleResolver.class);
				setter.invoke(definitionsFactory, new SpringLocaleResolver());
			}
			catch (Exception ex) {
				throw new IllegalStateException("Cannot override LocaleResolver with SpringLocaleResolver", ex);
			}
		}

		if (activateEl && this.tilesInitializer instanceof SpringTilesInitializer) {
			// Again, we need to do this after initialization since SpringTilesContainerFactory
			// cannot override template methods that refer to Tiles 2.2 classes: in this case,
			// AttributeEvaluatorFactory as createAttributeEvaluatorFactory return type.
			BasicTilesContainer container = (BasicTilesContainer) ServletUtil.getContainer(this.servletContext);
			new TilesElActivator().registerEvaluator(container);
		}
	}

	/**
	 * Creates a new instance of {@link org.apache.tiles.startup.BasicTilesInitializer}.
	 * <p>Override it to use a different initializer.
	 * @see org.apache.tiles.web.startup.TilesListener#createTilesInitializer()
	 */
	protected TilesInitializer createTilesInitializer() {
		return (tiles22Present ? new SpringTilesInitializer() : new BasicTilesInitializer());
	}

	/**
	 * Removes the TilesContainer from this web application.
	 * @throws TilesException in case of cleanup failure
	 */
	public void destroy() throws TilesException {
		try {
			// Tiles 2.2?
			ReflectionUtils.invokeMethod(TilesInitializer.class.getMethod("destroy"), this.tilesInitializer);
		}
		catch (NoSuchMethodException ex) {
			// Tiles 2.1...
			ServletUtil.setContainer(this.servletContext, null);
		}
	}


	private class SpringTilesInitializer extends BasicTilesInitializer {

		@Override
		protected AbstractTilesContainerFactory createContainerFactory(TilesApplicationContext context) {
			return new SpringTilesContainerFactory();
		}
	}


	private class SpringTilesContainerFactory extends BasicTilesContainerFactory {

		@Override
		protected BasicTilesContainer instantiateContainer(TilesApplicationContext context) {
			return (useMutableTilesContainer ? new CachingTilesContainer() : new BasicTilesContainer());
		}

		@Override
		protected void registerRequestContextFactory(String className,
				List<TilesRequestContextFactory> factories, TilesRequestContextFactory parent) {
			// Avoid Tiles 2.2 warn logging when default RequestContextFactory impl class not found
			if (ClassUtils.isPresent(className, TilesConfigurer.class.getClassLoader())) {
				super.registerRequestContextFactory(className, factories, parent);
			}
		}

		@Override
		protected List<URL> getSourceURLs(TilesApplicationContext applicationContext,
				TilesRequestContextFactory contextFactory) {
			if (definitions != null) {
				try {
					List<URL> result = new LinkedList<URL>();
					for (String definition : definitions) {
						result.addAll(applicationContext.getResources(definition));
					}
					return result;
				}
				catch (IOException ex) {
					throw new DefinitionsFactoryException("Cannot load definition URLs", ex);
				}
			}
			else {
				return super.getSourceURLs(applicationContext, contextFactory);
			}
		}

		@Override
		protected BaseLocaleUrlDefinitionDAO instantiateLocaleDefinitionDao(TilesApplicationContext applicationContext,
				TilesRequestContextFactory contextFactory, LocaleResolver resolver) {
			BaseLocaleUrlDefinitionDAO dao = super.instantiateLocaleDefinitionDao(
					applicationContext, contextFactory, resolver);
			if (checkRefresh && dao instanceof CachingLocaleUrlDefinitionDAO) {
				((CachingLocaleUrlDefinitionDAO) dao).setCheckRefresh(checkRefresh);
			}
			return dao;
		}

		@Override
		protected DefinitionsReader createDefinitionsReader(TilesApplicationContext applicationContext,
				TilesRequestContextFactory contextFactory) {
			DigesterDefinitionsReader reader = new DigesterDefinitionsReader();
			if (!validateDefinitions){
				Map<String,String> map = new HashMap<String,String>();
				map.put(DigesterDefinitionsReader.PARSER_VALIDATE_PARAMETER_NAME, Boolean.FALSE.toString());
				reader.init(map);
			}
			return reader;
		}

		@Override
		protected DefinitionsFactory createDefinitionsFactory(TilesApplicationContext applicationContext,
				TilesRequestContextFactory contextFactory, LocaleResolver resolver) {
			if (definitionsFactoryClass != null) {
				DefinitionsFactory factory = BeanUtils.instantiate(definitionsFactoryClass);
				if (factory instanceof TilesApplicationContextAware) {
					((TilesApplicationContextAware) factory).setApplicationContext(applicationContext);
				}
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(factory);
				if (bw.isWritableProperty("localeResolver")) {
					bw.setPropertyValue("localeResolver", resolver);
				}
				if (bw.isWritableProperty("definitionDAO")) {
					bw.setPropertyValue("definitionDAO",
							createLocaleDefinitionDao(applicationContext, contextFactory, resolver));
				}
				if (factory instanceof Refreshable) {
					((Refreshable) factory).refresh();
				}
				return factory;
			}
			else {
				return super.createDefinitionsFactory(applicationContext, contextFactory, resolver);
			}
		}

		@Override
		protected PreparerFactory createPreparerFactory(TilesApplicationContext applicationContext,
				TilesRequestContextFactory contextFactory) {
			if (preparerFactoryClass != null) {
				return BeanUtils.instantiate(preparerFactoryClass);
			}
			else {
				return super.createPreparerFactory(applicationContext, contextFactory);
			}
		}

		@Override
		protected LocaleResolver createLocaleResolver(TilesApplicationContext applicationContext,
				TilesRequestContextFactory contextFactory) {
			return new SpringLocaleResolver();
		}
	}


	private class JspExpressionChecker {

		public boolean isExpressionFactoryAvailable() {
			try {
				JspFactory factory = JspFactory.getDefaultFactory();
				if (factory != null &&
						factory.getJspApplicationContext(servletContext).getExpressionFactory() != null) {
					logger.info("Found JSP 2.1 ExpressionFactory");
					return true;
				}
			}
			catch (Throwable ex) {
				logger.warn("Could not obtain JSP 2.1 ExpressionFactory", ex);
			}
			return false;
		}
	}


	private class TilesElActivator {

		public void registerEvaluator(BasicTilesContainer container) {
			logger.debug("Registering Tiles 2.2 AttributeEvaluatorFactory for JSP 2.1");
			try {
				ClassLoader cl = TilesElActivator.class.getClassLoader();
				Class aef = cl.loadClass("org.apache.tiles.evaluator.AttributeEvaluatorFactory");
				Class baef = cl.loadClass("org.apache.tiles.evaluator.BasicAttributeEvaluatorFactory");
				Constructor baefCtor = baef.getConstructor(AttributeEvaluator.class);
				ELAttributeEvaluator evaluator = new ELAttributeEvaluator();
				evaluator.setApplicationContext(container.getApplicationContext());
				evaluator.init(new HashMap<String, String>());
				Object baefValue = baefCtor.newInstance(evaluator);
				Method setter = container.getClass().getMethod("setAttributeEvaluatorFactory", aef);
				setter.invoke(container, baefValue);
				Method getRequestContextFactory = BasicTilesContainer.class.getDeclaredMethod("getRequestContextFactory");
				getRequestContextFactory.setAccessible(true);
				Method createRendererFactory = BasicTilesContainerFactory.class.getDeclaredMethod("createRendererFactory",
						TilesApplicationContext.class, TilesRequestContextFactory.class, TilesContainer.class, aef);
				createRendererFactory.setAccessible(true);
				BasicTilesContainerFactory tcf = new BasicTilesContainerFactory();
				RendererFactory rendererFactory = (RendererFactory) createRendererFactory.invoke(
						tcf, container.getApplicationContext(), getRequestContextFactory.invoke(container),
						container, baefValue);
				container.setRendererFactory(rendererFactory);
			}
			catch (Exception ex) {
				throw new IllegalStateException("Cannot activate ELAttributeEvaluator", ex);
			}
		}
	}

}
