/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Performs the actual initialization work for the root application context.
 * Called by {@link ContextLoaderListener}.
 *
 * <p>Looks for a {@link #CONTEXT_CLASS_PARAM "contextClass"} parameter
 * at the {@code web.xml} context-param level to specify the context
 * class type, falling back to the default of
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}
 * if not found. With the default ContextLoader implementation, any context class
 * specified needs to implement the ConfigurableWebApplicationContext interface.
 *
 * <p>Processes a {@link #CONFIG_LOCATION_PARAM "contextConfigLocation"}
 * context-param and passes its value to the context instance, parsing it into
 * potentially multiple file paths which can be separated by any number of
 * commas and spaces, e.g. "WEB-INF/applicationContext1.xml,
 * WEB-INF/applicationContext2.xml". Ant-style path patterns are supported as well,
 * e.g. "WEB-INF/*Context.xml,WEB-INF/spring*.xml" or "WEB-INF/&#42;&#42;/*Context.xml".
 * If not explicitly specified, the context implementation is supposed to use a
 * default location (with XmlWebApplicationContext: "/WEB-INF/applicationContext.xml").
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in previously loaded files, at least when using one of
 * Spring's default ApplicationContext implementations. This can be leveraged
 * to deliberately override certain bean definitions via an extra XML file.
 *
 * <p>Above and beyond loading the root application context, this class
 * can optionally load or obtain and hook up a shared parent context to
 * the root application context. See the
 * {@link #loadParentContext(ServletContext)} method for more information.
 *
 * <p>As of Spring 3.1, {@code ContextLoader} supports injecting the root web
 * application context via the {@link #ContextLoader(WebApplicationContext)}
 * constructor, allowing for programmatic configuration in Servlet 3.0+ environments. See
 * {@link org.springframework.web.WebApplicationInitializer} for usage examples.
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @author Sam Brannen
 * @since 17.02.2003
 * @see ContextLoaderListener
 * @see ConfigurableWebApplicationContext
 * @see org.springframework.web.context.support.XmlWebApplicationContext
 */
public class ContextLoader {

	/**
	 * Config param for the root WebApplicationContext implementation class to use: {@value}
	 * @see #determineContextClass(ServletContext)
	 * @see #createWebApplicationContext(ServletContext, ApplicationContext)
	 */
	public static final String CONTEXT_CLASS_PARAM = "contextClass";

	/**
	 * Config param for the root WebApplicationContext id,
	 * to be used as serialization id for the underlying BeanFactory: {@value}
	 */
	public static final String CONTEXT_ID_PARAM = "contextId";

	/**
	 * Config param for which {@link ApplicationContextInitializer} classes to use
	 * for initializing the web application context: {@value}
	 * @see #customizeContext(ServletContext, ConfigurableWebApplicationContext)
	 */
	public static final String CONTEXT_INITIALIZER_CLASSES_PARAM = "contextInitializerClasses";

	/**
	 * Name of servlet context parameter (i.e., {@value}) that can specify the
	 * config location for the root context, falling back to the implementation's
	 * default otherwise.
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION
	 */
	public static final String CONFIG_LOCATION_PARAM = "contextConfigLocation";

	/**
	 * Optional servlet context parameter (i.e., "{@code locatorFactorySelector}")
	 * used only when obtaining a parent context using the default implementation
	 * of {@link #loadParentContext(ServletContext servletContext)}.
	 * Specifies the 'selector' used in the
	 * {@link ContextSingletonBeanFactoryLocator#getInstance(String selector)}
	 * method call, which is used to obtain the BeanFactoryLocator instance from
	 * which the parent context is obtained.
	 * <p>The default is {@code classpath*:beanRefContext.xml},
	 * matching the default applied for the
	 * {@link ContextSingletonBeanFactoryLocator#getInstance()} method.
	 * Supplying the "parentContextKey" parameter is sufficient in this case.
	 */
	public static final String LOCATOR_FACTORY_SELECTOR_PARAM = "locatorFactorySelector";

	/**
	 * Optional servlet context parameter (i.e., "{@code parentContextKey}")
	 * used only when obtaining a parent context using the default implementation
	 * of {@link #loadParentContext(ServletContext servletContext)}.
	 * Specifies the 'factoryKey' used in the
	 * {@link BeanFactoryLocator#useBeanFactory(String factoryKey)} method call,
	 * obtaining the parent application context from the BeanFactoryLocator instance.
	 * <p>Supplying this "parentContextKey" parameter is sufficient when relying
	 * on the default {@code classpath*:beanRefContext.xml} selector for
	 * candidate factory references.
	 */
	public static final String LOCATOR_FACTORY_KEY_PARAM = "parentContextKey";

	/**
	 * Name of the class path resource (relative to the ContextLoader class)
	 * that defines ContextLoader's default strategy names.
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "ContextLoader.properties";


	private static final Properties defaultStrategies;

	static {
		// Load default strategy implementations from properties file.
		// This is currently strictly internal and not meant to be customized
		// by application developers.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, ContextLoader.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'ContextLoader.properties': " + ex.getMessage());
		}
	}


	/**
	 * Map from (thread context) ClassLoader to corresponding 'current' WebApplicationContext.
	 */
	private static final Map<ClassLoader, WebApplicationContext> currentContextPerThread =
			new ConcurrentHashMap<ClassLoader, WebApplicationContext>(1);

	/**
	 * The 'current' WebApplicationContext, if the ContextLoader class is
	 * deployed in the web app ClassLoader itself.
	 */
	private static volatile WebApplicationContext currentContext;

	/**
	 * The root WebApplicationContext instance that this loader manages.
	 */
	private WebApplicationContext context;

	/**
	 * Holds BeanFactoryReference when loading parent factory via
	 * ContextSingletonBeanFactoryLocator.
	 */
	private BeanFactoryReference parentContextRef;


	/**
	 * Create a new {@code ContextLoader} that will create a web application context
	 * based on the "contextClass" and "contextConfigLocation" servlet context-params.
	 * See class-level documentation for details on default values for each.
	 * <p>This constructor is typically used when declaring the {@code
	 * ContextLoaderListener} subclass as a {@code <listener>} within {@code web.xml}, as
	 * a no-arg constructor is required.
	 * <p>The created application context will be registered into the ServletContext under
	 * the attribute name {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE}
	 * and subclasses are free to call the {@link #closeWebApplicationContext} method on
	 * container shutdown to close the application context.
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader() {
	}

	/**
	 * Create a new {@code ContextLoader} with the given application context. This
	 * constructor is useful in Servlet 3.0+ environments where instance-based
	 * registration of listeners is possible through the {@link ServletContext#addListener}
	 * API.
	 * <p>The context may or may not yet be {@linkplain
	 * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
	 * of {@link ConfigurableWebApplicationContext} and (b) has <strong>not</strong>
	 * already been refreshed (the recommended approach), then the following will occur:
	 * <ul>
	 * <li>If the given context has not already been assigned an {@linkplain
	 * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
	 * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
	 * the application context</li>
	 * <li>{@link #customizeContext} will be called</li>
	 * <li>Any {@link ApplicationContextInitializer}s specified through the
	 * "contextInitializerClasses" init-param will be applied.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
	 * </ul>
	 * If the context has already been refreshed or does not implement
	 * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
	 * assumption that the user has performed these actions (or not) per his or her
	 * specific needs.
	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
	 * <p>In any case, the given application context will be registered into the
	 * ServletContext under the attribute name {@link
	 * WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} and subclasses are
	 * free to call the {@link #closeWebApplicationContext} method on container shutdown
	 * to close the application context.
	 * @param context the application context to manage
	 * @see #initWebApplicationContext(ServletContext)
	 * @see #closeWebApplicationContext(ServletContext)
	 */
	public ContextLoader(WebApplicationContext context) {
		this.context = context;
	}

	/**
	 * Initialize Spring's web application context for the given servlet context,
	 * using the application context provided at construction time, or creating a new one
	 * according to the "{@link #CONTEXT_CLASS_PARAM contextClass}" and
	 * "{@link #CONFIG_LOCATION_PARAM contextConfigLocation}" context-params.
	 * @param servletContext current servlet context
	 * @return the new WebApplicationContext
	 * @see #ContextLoader(WebApplicationContext)
	 * @see #CONTEXT_CLASS_PARAM
	 * @see #CONFIG_LOCATION_PARAM
	 */
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
			throw new IllegalStateException(
					"Cannot initialize context because there is already a root application context present - " +
					"check whether you have multiple ContextLoader* definitions in your web.xml!");
		}

		Log logger = LogFactory.getLog(ContextLoader.class);
		servletContext.log("Initializing Spring root WebApplicationContext");
		if (logger.isInfoEnabled()) {
			logger.info("Root WebApplicationContext: initialization started");
		}
		long startTime = System.currentTimeMillis();

		try {
			// Store context in local instance variable, to guarantee that
			// it is available on ServletContext shutdown.
			if (this.context == null) {
				this.context = createWebApplicationContext(servletContext);
			}
			if (this.context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent ->
						// determine parent for root web application context, if any.
						ApplicationContext parent = loadParentContext(servletContext);
						cwac.setParent(parent);
					}
					configureAndRefreshWebApplicationContext(cwac, servletContext);
				}
			}
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);

			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = this.context;
			}
			else if (ccl != null) {
				currentContextPerThread.put(ccl, this.context);
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" +
						WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
			}
			if (logger.isInfoEnabled()) {
				long elapsedTime = System.currentTimeMillis() - startTime;
				logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
			}

			return this.context;
		}
		catch (RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
			throw ex;
		}
		catch (Error err) {
			logger.error("Context initialization failed", err);
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, err);
			throw err;
		}
	}

	/**
	 * Instantiate the root WebApplicationContext for this loader, either the
	 * default context class or a custom context class if specified.
	 * <p>This implementation expects custom contexts to implement the
	 * {@link ConfigurableWebApplicationContext} interface.
	 * Can be overridden in subclasses.
	 * <p>In addition, {@link #customizeContext} gets called prior to refreshing the
	 * context, allowing subclasses to perform custom modifications to the context.
	 * @param sc current servlet context
	 * @return the root WebApplicationContext
	 * @see ConfigurableWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(ServletContext sc) {
		Class<?> contextClass = determineContextClass(sc);
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException("Custom context class [" + contextClass.getName() +
					"] is not of type [" + ConfigurableWebApplicationContext.class.getName() + "]");
		}
		return (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link #createWebApplicationContext(ServletContext)} and
	 * {@link #configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext, ServletContext)}
	 */
	@Deprecated
	protected WebApplicationContext createWebApplicationContext(ServletContext sc, ApplicationContext parent) {
		return createWebApplicationContext(sc);
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac, ServletContext sc) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			String idParam = sc.getInitParameter(CONTEXT_ID_PARAM);
			if (idParam != null) {
				wac.setId(idParam);
			}
			else {
				// Generate default id...
				if (sc.getMajorVersion() == 2 && sc.getMinorVersion() < 5) {
					// Servlet <= 2.4: resort to name specified in web.xml, if any.
					wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
							ObjectUtils.getDisplayString(sc.getServletContextName()));
				}
				else {
					wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
							ObjectUtils.getDisplayString(sc.getContextPath()));
				}
			}
		}

		wac.setServletContext(sc);
		String initParameter = sc.getInitParameter(CONFIG_LOCATION_PARAM);
		if (initParameter != null) {
			wac.setConfigLocation(initParameter);
		}
		customizeContext(sc, wac);
		wac.refresh();
	}

	/**
	 * Return the WebApplicationContext implementation class to use, either the
	 * default XmlWebApplicationContext or a custom context class if specified.
	 * @param servletContext current servlet context
	 * @return the WebApplicationContext implementation class to use
	 * @see #CONTEXT_CLASS_PARAM
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected Class<?> determineContextClass(ServletContext servletContext) {
		String contextClassName = servletContext.getInitParameter(CONTEXT_CLASS_PARAM);
		if (contextClassName != null) {
			try {
				return ClassUtils.forName(contextClassName, ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load custom context class [" + contextClassName + "]", ex);
			}
		}
		else {
			contextClassName = defaultStrategies.getProperty(WebApplicationContext.class.getName());
			try {
				return ClassUtils.forName(contextClassName, ContextLoader.class.getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new ApplicationContextException(
						"Failed to load default context class [" + contextClassName + "]", ex);
			}
		}
	}

	/**
	 * Return the {@link ApplicationContextInitializer} implementation classes to use
	 * if any have been specified by {@link #CONTEXT_INITIALIZER_CLASSES_PARAM}.
	 * @param servletContext current servlet context
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 */
	@SuppressWarnings("unchecked")
	protected List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>
			determineContextInitializerClasses(ServletContext servletContext) {
		String classNames = servletContext.getInitParameter(CONTEXT_INITIALIZER_CLASSES_PARAM);
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> classes =
			new ArrayList<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>>();
		if (classNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(classNames, ",")) {
				try {
					Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
					Assert.isAssignable(ApplicationContextInitializer.class, clazz,
							"class [" + className + "] must implement ApplicationContextInitializer");
					classes.add((Class<ApplicationContextInitializer<ConfigurableApplicationContext>>)clazz);
				}
				catch (ClassNotFoundException ex) {
					throw new ApplicationContextException(
							"Failed to load context initializer class [" + className + "]", ex);
				}
			}
		}
		return classes;
	}

	/**
	 * Customize the {@link ConfigurableWebApplicationContext} created by this
	 * ContextLoader after config locations have been supplied to the context
	 * but before the context is <em>refreshed</em>.
	 * <p>The default implementation {@linkplain #determineContextInitializerClasses(ServletContext)
	 * determines} what (if any) context initializer classes have been specified through
	 * {@linkplain #CONTEXT_INITIALIZER_CLASSES_PARAM context init parameters} and
	 * {@linkplain ApplicationContextInitializer#initialize invokes each} with the
	 * given web application context.
	 * <p>Any {@code ApplicationContextInitializers} implementing
	 * {@link org.springframework.core.Ordered Ordered} or marked with @{@link
	 * org.springframework.core.annotation.Order Order} will be sorted appropriately.
	 * @param servletContext the current servlet context
	 * @param applicationContext the newly created application context
	 * @see #createWebApplicationContext(ServletContext, ApplicationContext)
	 * @see #CONTEXT_INITIALIZER_CLASSES_PARAM
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 */
	protected void customizeContext(ServletContext servletContext, ConfigurableWebApplicationContext applicationContext) {
		List<Class<ApplicationContextInitializer<ConfigurableApplicationContext>>> initializerClasses =
				determineContextInitializerClasses(servletContext);

		if (initializerClasses.size() == 0) {
			// no ApplicationContextInitializers have been declared -> nothing to do
			return;
		}

		Class<?> contextClass = applicationContext.getClass();
		ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerInstances =
				new ArrayList<ApplicationContextInitializer<ConfigurableApplicationContext>>();

		for (Class<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			Assert.isAssignable(initializerContextClass, contextClass, String.format(
					"Could not add context initializer [%s] as its generic parameter [%s] " +
					"is not assignable from the type of application context used by this " +
					"context loader [%s]: ", initializerClass.getName(), initializerContextClass.getName(),
					contextClass.getName()));
			initializerInstances.add(BeanUtils.instantiateClass(initializerClass));
		}

		applicationContext.getEnvironment().initPropertySources(servletContext, null);

		Collections.sort(initializerInstances, new AnnotationAwareOrderComparator());
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializerInstances) {
			initializer.initialize(applicationContext);
		}
	}

	/**
	 * Template method with default implementation (which may be overridden by a
	 * subclass), to load or obtain an ApplicationContext instance which will be
	 * used as the parent context of the root WebApplicationContext. If the
	 * return value from the method is null, no parent context is set.
	 * <p>The main reason to load a parent context here is to allow multiple root
	 * web application contexts to all be children of a shared EAR context, or
	 * alternately to also share the same parent context that is visible to
	 * EJBs. For pure web applications, there is usually no need to worry about
	 * having a parent context to the root web application context.
	 * <p>The default implementation uses
	 * {@link org.springframework.context.access.ContextSingletonBeanFactoryLocator},
	 * configured via {@link #LOCATOR_FACTORY_SELECTOR_PARAM} and
	 * {@link #LOCATOR_FACTORY_KEY_PARAM}, to load a parent context
	 * which will be shared by all other users of ContextsingletonBeanFactoryLocator
	 * which also use the same configuration parameters.
	 * @param servletContext current servlet context
	 * @return the parent application context, or {@code null} if none
	 * @see org.springframework.context.access.ContextSingletonBeanFactoryLocator
	 */
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		ApplicationContext parentContext = null;
		String locatorFactorySelector = servletContext.getInitParameter(LOCATOR_FACTORY_SELECTOR_PARAM);
		String parentContextKey = servletContext.getInitParameter(LOCATOR_FACTORY_KEY_PARAM);

		if (parentContextKey != null) {
			// locatorFactorySelector may be null, indicating the default "classpath*:beanRefContext.xml"
			BeanFactoryLocator locator = ContextSingletonBeanFactoryLocator.getInstance(locatorFactorySelector);
			Log logger = LogFactory.getLog(ContextLoader.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Getting parent context definition: using parent context key of '" +
						parentContextKey + "' with BeanFactoryLocator");
			}
			this.parentContextRef = locator.useBeanFactory(parentContextKey);
			parentContext = (ApplicationContext) this.parentContextRef.getFactory();
		}

		return parentContext;
	}

	/**
	 * Close Spring's web application context for the given servlet context. If
	 * the default {@link #loadParentContext(ServletContext)} implementation,
	 * which uses ContextSingletonBeanFactoryLocator, has loaded any shared
	 * parent context, release one reference to that shared parent context.
	 * <p>If overriding {@link #loadParentContext(ServletContext)}, you may have
	 * to override this method as well.
	 * @param servletContext the ServletContext that the WebApplicationContext runs in
	 */
	public void closeWebApplicationContext(ServletContext servletContext) {
		servletContext.log("Closing Spring root WebApplicationContext");
		try {
			if (this.context instanceof ConfigurableWebApplicationContext) {
				((ConfigurableWebApplicationContext) this.context).close();
			}
		}
		finally {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl == ContextLoader.class.getClassLoader()) {
				currentContext = null;
			}
			else if (ccl != null) {
				currentContextPerThread.remove(ccl);
			}
			servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			if (this.parentContextRef != null) {
				this.parentContextRef.release();
			}
		}
	}


	/**
	 * Obtain the Spring root web application context for the current thread
	 * (i.e. for the current thread's context ClassLoader, which needs to be
	 * the web application's ClassLoader).
	 * @return the current root web application context, or {@code null}
	 * if none found
	 * @see org.springframework.web.context.support.SpringBeanAutowiringSupport
	 */
	public static WebApplicationContext getCurrentWebApplicationContext() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		if (ccl != null) {
			WebApplicationContext ccpt = currentContextPerThread.get(ccl);
			if (ccpt != null) {
				return ccpt;
			}
		}
		return currentContext;
	}

}
