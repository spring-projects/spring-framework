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

package org.springframework.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

/**
 * Central dispatcher for HTTP request handlers/controllers, e.g. for web UI controllers or HTTP-based remote service
 * exporters. Dispatches to registered handlers for processing a web request, providing convenient mapping and exception
 * handling facilities.
 *
 * <p>This servlet is very flexible: It can be used with just about any workflow, with the installation of the
 * appropriate adapter classes. It offers the following functionality that distinguishes it from other request-driven
 * web MVC frameworks:
 *
 * <ul> <li>It is based around a JavaBeans configuration mechanism.
 *
 * <li>It can use any {@link HandlerMapping} implementation - pre-built or provided as part of an application - to
 * control the routing of requests to handler objects. Default is {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * and {@link org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping}. HandlerMapping objects
 * can be defined as beans in the servlet's application context, implementing the HandlerMapping interface, overriding
 * the default HandlerMapping if present. HandlerMappings can be given any bean name (they are tested by type).
 *
 * <li>It can use any {@link HandlerAdapter}; this allows for using any handler interface. Default adapters are {@link
 * org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter}, {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter},
 * for Spring's {@link org.springframework.web.HttpRequestHandler} and {@link org.springframework.web.servlet.mvc.Controller}
 * interfaces, respectively. A default {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}
 * will be registered as well. HandlerAdapter objects can be added as beans in the application context, overriding the
 * default HandlerAdapters. Like HandlerMappings, HandlerAdapters can be given any bean name (they are tested by type).
 *
 * <li>The dispatcher's exception resolution strategy can be specified via a {@link HandlerExceptionResolver}, for
 * example mapping certain exceptions to error pages. Default are
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver},
 * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver}, and
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}. These HandlerExceptionResolvers can be overridden
 * through the application context. HandlerExceptionResolver can be given any bean name (they are tested by type).
 *
 * <li>Its view resolution strategy can be specified via a {@link ViewResolver} implementation, resolving symbolic view
 * names into View objects. Default is {@link org.springframework.web.servlet.view.InternalResourceViewResolver}.
 * ViewResolver objects can be added as beans in the application context, overriding the default ViewResolver.
 * ViewResolvers can be given any bean name (they are tested by type).
 *
 * <li>If a {@link View} or view name is not supplied by the user, then the configured {@link
 * RequestToViewNameTranslator} will translate the current request into a view name. The corresponding bean name is
 * "viewNameTranslator"; the default is {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}.
 *
 * <li>The dispatcher's strategy for resolving multipart requests is determined by a {@link
 * org.springframework.web.multipart.MultipartResolver} implementation. Implementations for Jakarta Commons FileUpload
 * and Jason Hunter's COS are included; the typical choise is {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}.
 * The MultipartResolver bean name is "multipartResolver"; default is none.
 *
 * <li>Its locale resolution strategy is determined by a {@link LocaleResolver}. Out-of-the-box implementations work via
 * HTTP accept header, cookie, or session. The LocaleResolver bean name is "localeResolver"; default is {@link
 * org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}.
 *
 * <li>Its theme resolution strategy is determined by a {@link ThemeResolver}. Implementations for a fixed theme and for
 * cookie and session storage are included. The ThemeResolver bean name is "themeResolver"; default is {@link
 * org.springframework.web.servlet.theme.FixedThemeResolver}. </ul>
 *
 * <p><b>NOTE: The {@code @RequestMapping} annotation will only be processed if a corresponding
 * {@code HandlerMapping} (for type level annotations) and/or {@code HandlerAdapter} (for method level
 * annotations) is present in the dispatcher.</b> This is the case by default. However, if you are defining custom
 * {@code HandlerMappings} or {@code HandlerAdapters}, then you need to make sure that a corresponding custom
 * {@code DefaultAnnotationHandlerMapping} and/or {@code AnnotationMethodHandlerAdapter} is defined as well -
 * provided that you intend to use {@code @RequestMapping}.
 *
 * <p><b>A web application can define any number of DispatcherServlets.</b> Each servlet will operate in its own
 * namespace, loading its own application context with mappings, handlers, etc. Only the root application context as
 * loaded by {@link org.springframework.web.context.ContextLoaderListener}, if any, will be shared.
 *
 * <p>As of Spring 3.1, {@code DispatcherServlet} may now be injected with a web
 * application context, rather than creating its own internally. This is useful in Servlet
 * 3.0+ environments, which support programmatic registration of servlet instances. See
 * {@link #DispatcherServlet(WebApplicationContext)} Javadoc for details.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.context.ContextLoaderListener
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

	/** Well-known name for the MultipartResolver object in the bean factory for this namespace. */
	public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

	/** Well-known name for the LocaleResolver object in the bean factory for this namespace. */
	public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

	/** Well-known name for the ThemeResolver object in the bean factory for this namespace. */
	public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

	/**
	 * Well-known name for the HandlerMapping object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerMappings" is turned off.
	 * @see #setDetectAllHandlerMappings
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

	/**
	 * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerAdapters" is turned off.
	 * @see #setDetectAllHandlerAdapters
	 */
	public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

	/**
	 * Well-known name for the HandlerExceptionResolver object in the bean factory for this namespace.
	 * Only used when "detectAllHandlerExceptionResolvers" is turned off.
	 * @see #setDetectAllHandlerExceptionResolvers
	 */
	public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

	/**
	 * Well-known name for the RequestToViewNameTranslator object in the bean factory for this namespace.
	 */
	public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

	/**
	 * Well-known name for the ViewResolver object in the bean factory for this namespace.
	 * Only used when "detectAllViewResolvers" is turned off.
	 * @see #setDetectAllViewResolvers
	 */
	public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

	/**
	 * Well-known name for the FlashMapManager object in the bean factory for this namespace.
	 */
	public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

	/**
	 * Request attribute to hold the current web application context.
	 * Otherwise only the global web app context is obtainable by tags etc.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getWebApplicationContext
	 */
	public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

	/**
	 * Request attribute to hold the current LocaleResolver, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
	 */
	public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

	/**
	 * Request attribute to hold the current ThemeResolver, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeResolver
	 */
	public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

	/**
	 * Request attribute to hold the current ThemeSource, retrievable by views.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeSource
	 */
	public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

	/**
	 * Name of request attribute that holds a read-only {@code Map<String,?>}
	 * with "input" flash attributes saved by a previous request, if any.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
	 */
	public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the "output" {@link FlashMap} with
	 * attributes to save for a subsequent request.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
	 */
	public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

	/**
	 * Name of request attribute that holds the {@link FlashMapManager}.
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
	 */
	public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

	/** Log category to use when no mapped handler is found for a request. */
	public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

	/**
	 * Name of the class path resource (relative to the DispatcherServlet class)
	 * that defines DispatcherServlet's default strategy names.
	 */
	private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";


	/** Additional logger to use when no mapped handler is found for a request. */
	protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

	private static final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private static final Properties defaultStrategies;

	static {
		// Load default strategy implementations from properties file.
		// This is currently strictly internal and not meant to be customized
		// by application developers.
		try {
			ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
			defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load 'DispatcherServlet.properties': " + ex.getMessage());
		}
	}

	/** Detect all HandlerMappings or just expect "handlerMapping" bean? */
	private boolean detectAllHandlerMappings = true;

	/** Detect all HandlerAdapters or just expect "handlerAdapter" bean? */
	private boolean detectAllHandlerAdapters = true;

	/** Detect all HandlerExceptionResolvers or just expect "handlerExceptionResolver" bean? */
	private boolean detectAllHandlerExceptionResolvers = true;

	/** Detect all ViewResolvers or just expect "viewResolver" bean? */
	private boolean detectAllViewResolvers = true;

	/** Perform cleanup of request attributes after include request? */
	private boolean cleanupAfterInclude = true;

	/** MultipartResolver used by this servlet */
	private MultipartResolver multipartResolver;

	/** LocaleResolver used by this servlet */
	private LocaleResolver localeResolver;

	/** ThemeResolver used by this servlet */
	private ThemeResolver themeResolver;

	/** List of HandlerMappings used by this servlet */
	private List<HandlerMapping> handlerMappings;

	/** List of HandlerAdapters used by this servlet */
	private List<HandlerAdapter> handlerAdapters;

	/** List of HandlerExceptionResolvers used by this servlet */
	private List<HandlerExceptionResolver> handlerExceptionResolvers;

	/** RequestToViewNameTranslator used by this servlet */
	private RequestToViewNameTranslator viewNameTranslator;

	/** FlashMapManager used by this servlet */
	private FlashMapManager flashMapManager;

	/** List of ViewResolvers used by this servlet */
	private List<ViewResolver> viewResolvers;

	/**
	 * Create a new {@code DispatcherServlet} that will create its own internal web
	 * application context based on defaults and values provided through servlet
	 * init-params. Typically used in Servlet 2.5 or earlier environments, where the only
	 * option for servlet registration is through {@code web.xml} which requires the use
	 * of a no-arg constructor.
	 * <p>Calling {@link #setContextConfigLocation} (init-param 'contextConfigLocation')
	 * will dictate which XML files will be loaded by the
	 * {@linkplain #DEFAULT_CONTEXT_CLASS default XmlWebApplicationContext}
	 * <p>Calling {@link #setContextClass} (init-param 'contextClass') overrides the
	 * default {@code XmlWebApplicationContext} and allows for specifying an alternative class,
	 * such as {@code AnnotationConfigWebApplicationContext}.
	 * <p>Calling {@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
	 * indicates which {@code ApplicationContextInitializer} classes should be used to
	 * further configure the internal application context prior to refresh().
	 * @see #DispatcherServlet(WebApplicationContext)
	 */
	public DispatcherServlet() {
		super();
	}

	/**
	 * Create a new {@code DispatcherServlet} with the given web application context. This
	 * constructor is useful in Servlet 3.0+ environments where instance-based registration
	 * of servlets is possible through the {@link ServletContext#addServlet} API.
	 * <p>Using this constructor indicates that the following properties / init-params
	 * will be ignored:
	 * <ul>
	 * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
	 * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
	 * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
	 * <li>{@link #setNamespace(String)} / 'namespace'</li>
	 * </ul>
	 * <p>The given web application context may or may not yet be {@linkplain
	 * ConfigurableApplicationContext#refresh() refreshed}. If it has <strong>not</strong>
	 * already been refreshed (the recommended approach), then the following will occur:
	 * <ul>
	 * <li>If the given context does not already have a {@linkplain
	 * ConfigurableApplicationContext#setParent parent}, the root application context
	 * will be set as the parent.</li>
	 * <li>If the given context has not already been assigned an {@linkplain
	 * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
	 * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
	 * the application context</li>
	 * <li>{@link #postProcessWebApplicationContext} will be called</li>
	 * <li>Any {@code ApplicationContextInitializer}s specified through the
	 * "contextInitializerClasses" init-param or through the {@link
	 * #setContextInitializers} property will be applied.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called if the
	 * context implements {@link ConfigurableApplicationContext}</li>
	 * </ul>
	 * If the context has already been refreshed, none of the above will occur, under the
	 * assumption that the user has performed these actions (or not) per their specific
	 * needs.
	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
	 * @param webApplicationContext the context to use
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public DispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
	}

	/**
	 * Set whether to detect all HandlerMapping beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerMapping" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerMapping, despite multiple HandlerMapping beans being defined in the context.
	 */
	public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
		this.detectAllHandlerMappings = detectAllHandlerMappings;
	}

	/**
	 * Set whether to detect all HandlerAdapter beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerAdapter" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerAdapter, despite multiple HandlerAdapter beans being defined in the context.
	 */
	public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
		this.detectAllHandlerAdapters = detectAllHandlerAdapters;
	}

	/**
	 * Set whether to detect all HandlerExceptionResolver beans in this servlet's context. Otherwise,
	 * just a single bean with name "handlerExceptionResolver" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * HandlerExceptionResolver, despite multiple HandlerExceptionResolver beans being defined in the context.
	 */
	public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
		this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
	}

	/**
	 * Set whether to detect all ViewResolver beans in this servlet's context. Otherwise,
	 * just a single bean with name "viewResolver" will be expected.
	 * <p>Default is "true". Turn this off if you want this servlet to use a single
	 * ViewResolver, despite multiple ViewResolver beans being defined in the context.
	 */
	public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
		this.detectAllViewResolvers = detectAllViewResolvers;
	}

	/**
	 * Set whether to perform cleanup of request attributes after an include request, that is,
	 * whether to reset the original state of all request attributes after the DispatcherServlet
	 * has processed within an include request. Otherwise, just the DispatcherServlet's own
	 * request attributes will be reset, but not model attributes for JSPs or special attributes
	 * set by views (for example, JSTL's).
	 * <p>Default is "true", which is strongly recommended. Views should not rely on request attributes
	 * having been set by (dynamic) includes. This allows JSP views rendered by an included controller
	 * to use any model attributes, even with the same names as in the main JSP, without causing side
	 * effects. Only turn this off for special needs, for example to deliberately allow main JSPs to
	 * access attributes from JSP views rendered by an included controller.
	 */
	public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
		this.cleanupAfterInclude = cleanupAfterInclude;
	}

	/**
	 * This implementation calls {@link #initStrategies}.
	 */
	@Override
	protected void onRefresh(ApplicationContext context) {
		initStrategies(context);
	}

	/**
	 * Initialize the strategy objects that this servlet uses.
	 * <p>May be overridden in subclasses in order to initialize further strategy objects.
	 */
	protected void initStrategies(ApplicationContext context) {
		initMultipartResolver(context);
		initLocaleResolver(context);
		initThemeResolver(context);
		initHandlerMappings(context);
		initHandlerAdapters(context);
		initHandlerExceptionResolvers(context);
		initRequestToViewNameTranslator(context);
		initViewResolvers(context);
		initFlashMapManager(context);
	}

	/**
	 * Initialize the MultipartResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * no multipart handling is provided.
	 */
	private void initMultipartResolver(ApplicationContext context) {
		try {
			this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MultipartResolver [" + this.multipartResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Default is no multipart resolver.
			this.multipartResolver = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MultipartResolver with name '" + MULTIPART_RESOLVER_BEAN_NAME +
						"': no multipart request handling provided");
			}
		}
	}

	/**
	 * Initialize the LocaleResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * we default to AcceptHeaderLocaleResolver.
	 */
	private void initLocaleResolver(ApplicationContext context) {
		try {
			this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using LocaleResolver [" + this.localeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate LocaleResolver with name '" + LOCALE_RESOLVER_BEAN_NAME +
						"': using default [" + this.localeResolver + "]");
			}
		}
	}

	/**
	 * Initialize the ThemeResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * we default to a FixedThemeResolver.
	 */
	private void initThemeResolver(ApplicationContext context) {
		try {
			this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeResolver [" + this.themeResolver + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Unable to locate ThemeResolver with name '" + THEME_RESOLVER_BEAN_NAME + "': using default [" +
								this.themeResolver + "]");
			}
		}
	}

	/**
	 * Initialize the HandlerMappings used by this class.
	 * <p>If no HandlerMapping beans are defined in the BeanFactory for this namespace,
	 * we default to BeanNameUrlHandlerMapping.
	 */
	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		if (this.detectAllHandlerMappings) {
			// Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerMapping> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
				// We keep HandlerMappings in sorted order.
				OrderComparator.sort(this.handlerMappings);
			}
		}
		else {
			try {
				HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
				this.handlerMappings = Collections.singletonList(hm);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerMapping later.
			}
		}

		// Ensure we have at least one HandlerMapping, by registering
		// a default HandlerMapping if no other mappings are found.
		if (this.handlerMappings == null) {
			this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerMappings found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the HandlerAdapters used by this class.
	 * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
	 * we default to SimpleControllerHandlerAdapter.
	 */
	private void initHandlerAdapters(ApplicationContext context) {
		this.handlerAdapters = null;

		if (this.detectAllHandlerAdapters) {
			// Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerAdapter> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerAdapters = new ArrayList<HandlerAdapter>(matchingBeans.values());
				// We keep HandlerAdapters in sorted order.
				OrderComparator.sort(this.handlerAdapters);
			}
		}
		else {
			try {
				HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
				this.handlerAdapters = Collections.singletonList(ha);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default HandlerAdapter later.
			}
		}

		// Ensure we have at least some HandlerAdapters, by registering
		// default HandlerAdapters if no other adapters are found.
		if (this.handlerAdapters == null) {
			this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerAdapters found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the HandlerExceptionResolver used by this class.
	 * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
	 * we default to no exception resolver.
	 */
	private void initHandlerExceptionResolvers(ApplicationContext context) {
		this.handlerExceptionResolvers = null;

		if (this.detectAllHandlerExceptionResolvers) {
			// Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
					.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>(matchingBeans.values());
				// We keep HandlerExceptionResolvers in sorted order.
				OrderComparator.sort(this.handlerExceptionResolvers);
			}
		}
		else {
			try {
				HandlerExceptionResolver her =
						context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
				this.handlerExceptionResolvers = Collections.singletonList(her);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, no HandlerExceptionResolver is fine too.
			}
		}

		// Ensure we have at least some HandlerExceptionResolvers, by registering
		// default HandlerExceptionResolvers if no other resolvers are found.
		if (this.handlerExceptionResolvers == null) {
			this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No HandlerExceptionResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the RequestToViewNameTranslator used by this servlet instance.
	 * <p>If no implementation is configured then we default to DefaultRequestToViewNameTranslator.
	 */
	private void initRequestToViewNameTranslator(ApplicationContext context) {
		try {
			this.viewNameTranslator =
					context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using RequestToViewNameTranslator [" + this.viewNameTranslator + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate RequestToViewNameTranslator with name '" +
						REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME + "': using default [" + this.viewNameTranslator +
						"]");
			}
		}
	}

	/**
	 * Initialize the ViewResolvers used by this class.
	 * <p>If no ViewResolver beans are defined in the BeanFactory for this
	 * namespace, we default to InternalResourceViewResolver.
	 */
	private void initViewResolvers(ApplicationContext context) {
		this.viewResolvers = null;

		if (this.detectAllViewResolvers) {
			// Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
			Map<String, ViewResolver> matchingBeans =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
			if (!matchingBeans.isEmpty()) {
				this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.values());
				// We keep ViewResolvers in sorted order.
				OrderComparator.sort(this.viewResolvers);
			}
		}
		else {
			try {
				ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
				this.viewResolvers = Collections.singletonList(vr);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore, we'll add a default ViewResolver later.
			}
		}

		// Ensure we have at least one ViewResolver, by registering
		// a default ViewResolver if no other resolvers are found.
		if (this.viewResolvers == null) {
			this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
			if (logger.isDebugEnabled()) {
				logger.debug("No ViewResolvers found in servlet '" + getServletName() + "': using default");
			}
		}
	}

	/**
	 * Initialize the {@link FlashMapManager} used by this servlet instance.
	 * <p>If no implementation is configured then we default to
	 * {@code org.springframework.web.servlet.support.DefaultFlashMapManager}.
	 */
	private void initFlashMapManager(ApplicationContext context) {
		try {
			this.flashMapManager =
					context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using FlashMapManager [" + this.flashMapManager + "]");
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate FlashMapManager with name '" +
						FLASH_MAP_MANAGER_BEAN_NAME + "': using default [" + this.flashMapManager + "]");
			}
		}
	}

	/**
	 * Return this servlet's ThemeSource, if any; else return {@code null}.
	 * <p>Default is to return the WebApplicationContext as ThemeSource,
	 * provided that it implements the ThemeSource interface.
	 * @return the ThemeSource, if any
	 * @see #getWebApplicationContext()
	 */
	public final ThemeSource getThemeSource() {
		if (getWebApplicationContext() instanceof ThemeSource) {
			return (ThemeSource) getWebApplicationContext();
		}
		else {
			return null;
		}
	}

	/**
	 * Obtain this servlet's MultipartResolver, if any.
	 * @return the MultipartResolver used by this servlet, or {@code null} if none
	 * (indicating that no multipart support is available)
	 */
	public final MultipartResolver getMultipartResolver() {
		return this.multipartResolver;
	}

	/**
	 * Return the default strategy object for the given strategy interface.
	 * <p>The default implementation delegates to {@link #getDefaultStrategies},
	 * expecting a single object in the list.
	 * @param context the current WebApplicationContext
	 * @param strategyInterface the strategy interface
	 * @return the corresponding strategy object
	 * @see #getDefaultStrategies
	 */
	protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
		List<T> strategies = getDefaultStrategies(context, strategyInterface);
		if (strategies.size() != 1) {
			throw new BeanInitializationException(
					"DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
		}
		return strategies.get(0);
	}

	/**
	 * Create a List of default strategy objects for the given strategy interface.
	 * <p>The default implementation uses the "DispatcherServlet.properties" file (in the same
	 * package as the DispatcherServlet class) to determine the class names. It instantiates
	 * the strategy objects through the context's BeanFactory.
	 * @param context the current WebApplicationContext
	 * @param strategyInterface the strategy interface
	 * @return the List of corresponding strategy objects
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
		String key = strategyInterface.getName();
		String value = defaultStrategies.getProperty(key);
		if (value != null) {
			String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
			List<T> strategies = new ArrayList<T>(classNames.length);
			for (String className : classNames) {
				try {
					Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
					Object strategy = createDefaultStrategy(context, clazz);
					strategies.add((T) strategy);
				}
				catch (ClassNotFoundException ex) {
					throw new BeanInitializationException(
							"Could not find DispatcherServlet's default strategy class [" + className +
									"] for interface [" + key + "]", ex);
				}
				catch (LinkageError err) {
					throw new BeanInitializationException(
							"Error loading DispatcherServlet's default strategy class [" + className +
									"] for interface [" + key + "]: problem with class file or dependent class", err);
				}
			}
			return strategies;
		}
		else {
			return new LinkedList<T>();
		}
	}

	/**
	 * Create a default strategy.
	 * <p>The default implementation uses {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}.
	 * @param context the current WebApplicationContext
	 * @param clazz the strategy implementation class to instantiate
	 * @return the fully configured strategy instance
	 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean
	 */
	protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
		return context.getAutowireCapableBeanFactory().createBean(clazz);
	}


	/**
	 * Exposes the DispatcherServlet-specific request attributes and delegates to {@link #doDispatch}
	 * for the actual dispatching.
	 */
	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (logger.isDebugEnabled()) {
			String requestUri = urlPathHelper.getRequestUri(request);
			String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
			logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
					" processing " + request.getMethod() + " request for [" + requestUri + "]");
		}

		// Keep a snapshot of the request attributes in case of an include,
		// to be able to restore the original attributes after the include.
		Map<String, Object> attributesSnapshot = null;
		if (WebUtils.isIncludeRequest(request)) {
			logger.debug("Taking snapshot of request attributes before include");
			attributesSnapshot = new HashMap<String, Object>();
			Enumeration<?> attrNames = request.getAttributeNames();
			while (attrNames.hasMoreElements()) {
				String attrName = (String) attrNames.nextElement();
				if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
					attributesSnapshot.put(attrName, request.getAttribute(attrName));
				}
			}
		}

		// Make framework objects available to handlers and view objects.
		request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
		request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
		request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
		request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

		FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
		if (inputFlashMap != null) {
			request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
		}
		request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

		try {
			doDispatch(request, response);
		}
		finally {
			if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
				return;
			}
			// Restore the original attribute snapshot, in case of an include.
			if (attributesSnapshot != null) {
				restoreAttributesAfterInclude(request, attributesSnapshot);
			}
		}
	}

	/**
	 * Process the actual dispatching to the handler.
	 * <p>The handler will be obtained by applying the servlet's HandlerMappings in order.
	 * The HandlerAdapter will be obtained by querying the servlet's installed HandlerAdapters
	 * to find the first that supports the handler class.
	 * <p>All HTTP methods are handled by this method. It's up to HandlerAdapters or handlers
	 * themselves to decide which methods are acceptable.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 */
	protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpServletRequest processedRequest = request;
		HandlerExecutionChain mappedHandler = null;
		boolean multipartRequestParsed = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		try {
			ModelAndView mv = null;
			Exception dispatchException = null;

			try {
				processedRequest = checkMultipart(request);
				multipartRequestParsed = processedRequest != request;

				// Determine handler for the current request.
				mappedHandler = getHandler(processedRequest);
				if (mappedHandler == null || mappedHandler.getHandler() == null) {
					noHandlerFound(processedRequest, response);
					return;
				}

				// Determine handler adapter for the current request.
				HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

				// Process last-modified header, if supported by the handler.
				String method = request.getMethod();
				boolean isGet = "GET".equals(method);
				if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					if (logger.isDebugEnabled()) {
						String requestUri = urlPathHelper.getRequestUri(request);
						logger.debug("Last-Modified value for [" + requestUri + "] is: " + lastModified);
					}
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
				}

				if (!mappedHandler.applyPreHandle(processedRequest, response)) {
					return;
				}

				try {
					// Actually invoke the handler.
					mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
				}
				finally {
					if (asyncManager.isConcurrentHandlingStarted()) {
						return;
					}
				}

				applyDefaultViewName(request, mv);
				mappedHandler.applyPostHandle(processedRequest, response, mv);
			}
			catch (Exception ex) {
				dispatchException = ex;
			}
			processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
		}
		catch (Exception ex) {
			triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
		}
		catch (Error err) {
			triggerAfterCompletionWithError(processedRequest, response, mappedHandler, err);
		}
		finally {
			if (asyncManager.isConcurrentHandlingStarted()) {
				// Instead of postHandle and afterCompletion
				mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
				return;
			}
			// Clean up any resources used by a multipart request.
			if (multipartRequestParsed) {
				cleanupMultipart(processedRequest);
			}
		}
	}

	/**
	 * Do we need view name translation?
	 */
	private void applyDefaultViewName(HttpServletRequest request, ModelAndView mv) throws Exception {
		if (mv != null && !mv.hasView()) {
			mv.setViewName(getDefaultViewName(request));
		}
	}

	/**
	 * Handle the result of handler selection and handler invocation, which is
	 * either a ModelAndView or an Exception to be resolved to a ModelAndView.
	 */
	private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {

		boolean errorView = false;

		if (exception != null) {
			if (exception instanceof ModelAndViewDefiningException) {
				logger.debug("ModelAndViewDefiningException encountered", exception);
				mv = ((ModelAndViewDefiningException) exception).getModelAndView();
			}
			else {
				Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
				mv = processHandlerException(request, response, handler, exception);
				errorView = (mv != null);
			}
		}

		// Did the handler return a view to render?
		if (mv != null && !mv.wasCleared()) {
			render(mv, request, response);
			if (errorView) {
				WebUtils.clearErrorRequestAttributes(request);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Null ModelAndView returned to DispatcherServlet with name '" + getServletName() +
						"': assuming HandlerAdapter completed request handling");
			}
		}

		if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
			// Concurrent handling started during a forward
			return;
		}

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, null);
		}
	}

	/**
	 * Build a LocaleContext for the given request, exposing the request's primary locale as current locale.
	 * <p>The default implementation uses the dispatcher's LocaleResolver to obtain the current locale,
	 * which might change during a request.
	 * @param request current HTTP request
	 * @return the corresponding LocaleContext
	 */
	@Override
	protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
		return new LocaleContext() {
			@Override
			public Locale getLocale() {
				return localeResolver.resolveLocale(request);
			}
			@Override
			public String toString() {
				return getLocale().toString();
			}
		};
	}

	/**
	 * Convert the request into a multipart request, and make multipart resolver available.
	 * <p>If no multipart resolver is set, simply use the existing request.
	 * @param request current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 * @see MultipartResolver#resolveMultipart
	 */
	protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
			if (request instanceof MultipartHttpServletRequest) {
				logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
						"this typically results from an additional MultipartFilter in web.xml");
			}
			else {
				return this.multipartResolver.resolveMultipart(request);
			}
		}
		// If not returned before: return original request.
		return request;
	}

	/**
	 * Clean up any resources used by the given multipart request (if any).
	 * @param servletRequest current HTTP request
	 * @see MultipartResolver#cleanupMultipart
	 */
	protected void cleanupMultipart(HttpServletRequest servletRequest) {
		MultipartHttpServletRequest req = WebUtils.getNativeRequest(servletRequest, MultipartHttpServletRequest.class);
		if (req != null) {
			this.multipartResolver.cleanupMultipart(req);
		}
	}

	/**
	 * Return the HandlerExecutionChain for this request.
	 * <p>Tries all handler mappings in order.
	 * @param request current HTTP request
	 * @return the HandlerExecutionChain, or {@code null} if no handler could be found
	 */
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		for (HandlerMapping hm : this.handlerMappings) {
			if (logger.isTraceEnabled()) {
				logger.trace(
						"Testing handler map [" + hm + "] in DispatcherServlet with name '" + getServletName() + "'");
			}
			HandlerExecutionChain handler = hm.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	/**
	 * No handler found -> set appropriate HTTP response status.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception if preparing the response failed
	 */
	protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (pageNotFoundLogger.isWarnEnabled()) {
			String requestUri = urlPathHelper.getRequestUri(request);
			pageNotFoundLogger.warn("No mapping found for HTTP request with URI [" + requestUri +
					"] in DispatcherServlet with name '" + getServletName() + "'");
		}
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Return the HandlerAdapter for this handler object.
	 * @param handler the handler object to find an adapter for
	 * @throws ServletException if no HandlerAdapter can be found for the handler. This is a fatal error.
	 */
	protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
		for (HandlerAdapter ha : this.handlerAdapters) {
			if (logger.isTraceEnabled()) {
				logger.trace("Testing handler adapter [" + ha + "]");
			}
			if (ha.supports(handler)) {
				return ha;
			}
		}
		throw new ServletException("No adapter for handler [" + handler +
				"]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
	}

	/**
	 * Determine an error ModelAndView via the registered HandlerExceptionResolvers.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
	 * (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to
	 * @throws Exception if no error ModelAndView found
	 */
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		// Check registered HandlerExceptionResolvers...
		ModelAndView exMv = null;
		for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
			exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
			if (exMv != null) {
				break;
			}
		}
		if (exMv != null) {
			if (exMv.isEmpty()) {
				return null;
			}
			// We might still need view name translation for a plain error model...
			if (!exMv.hasView()) {
				exMv.setViewName(getDefaultViewName(request));
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Handler execution resulted in exception - forwarding to resolved error view: " + exMv, ex);
			}
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return exMv;
		}

		throw ex;
	}

	/**
	 * Render the given ModelAndView.
	 * <p>This is the last stage in handling a request. It may involve resolving the view by name.
	 * @param mv the ModelAndView to render
	 * @param request current HTTP servlet request
	 * @param response current HTTP servlet response
	 * @throws ServletException if view is missing or cannot be resolved
	 * @throws Exception if there's a problem rendering the view
	 */
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Determine locale for request and apply it to the response.
		Locale locale = this.localeResolver.resolveLocale(request);
		response.setLocale(locale);

		View view;
		if (mv.isReference()) {
			// We need to resolve the view name.
			view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
			if (view == null) {
				throw new ServletException(
						"Could not resolve view with name '" + mv.getViewName() + "' in servlet with name '" +
								getServletName() + "'");
			}
		}
		else {
			// No need to lookup: the ModelAndView object contains the actual View object.
			view = mv.getView();
			if (view == null) {
				throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
						"View object in servlet with name '" + getServletName() + "'");
			}
		}

		// Delegate to the View object for rendering.
		if (logger.isDebugEnabled()) {
			logger.debug("Rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'");
		}
		try {
			view.render(mv.getModelInternal(), request, response);
		} catch (Exception x) {
			if (logger.isDebugEnabled()) {
				logger.debug("Error rendering view [" + view + "] in DispatcherServlet with name '" + getServletName() + "'", x);
			}
			throw x;
		}
	}

	/**
	 * Translate the supplied request into a default view name.
	 * @param request current HTTP servlet request
	 * @return the view name (or {@code null} if no default found)
	 * @throws Exception if view name translation failed
	 */
	protected String getDefaultViewName(HttpServletRequest request) throws Exception {
		return this.viewNameTranslator.getViewName(request);
	}

	/**
	 * Resolve the given view name into a View object (to be rendered).
	 * <p>The default implementations asks all ViewResolvers of this dispatcher.
	 * Can be overridden for custom resolution strategies, potentially based on
	 * specific model attributes or request parameters.
	 * @param viewName the name of the view to resolve
	 * @param model the model to be passed to the view
	 * @param locale the current locale
	 * @param request current HTTP servlet request
	 * @return the View object, or {@code null} if none found
	 * @throws Exception if the view cannot be resolved
	 * (typically in case of problems creating an actual View object)
	 * @see ViewResolver#resolveViewName
	 */
	protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale,
			HttpServletRequest request) throws Exception {

		for (ViewResolver viewResolver : this.viewResolvers) {
			View view = viewResolver.resolveViewName(viewName, locale);
			if (view != null) {
				return view;
			}
		}
		return null;
	}

	private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	private void triggerAfterCompletionWithError(HttpServletRequest request, HttpServletResponse response,
			HandlerExecutionChain mappedHandler, Error error) throws Exception, ServletException {

		ServletException ex = new NestedServletException("Handler processing failed", error);
		if (mappedHandler != null) {
			mappedHandler.triggerAfterCompletion(request, response, ex);
		}
		throw ex;
	}

	/**
	 * Restore the request attributes after an include.
	 * @param request current HTTP request
	 * @param attributesSnapshot the snapshot of the request attributes before the include
	 */
	@SuppressWarnings("unchecked")
	private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?,?> attributesSnapshot) {
		logger.debug("Restoring snapshot of request attributes after include");

		// Need to copy into separate Collection here, to avoid side effects
		// on the Enumeration when removing attributes.
		Set<String> attrsToCheck = new HashSet<String>();
		Enumeration<?> attrNames = request.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String attrName = (String) attrNames.nextElement();
			if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
				attrsToCheck.add(attrName);
			}
		}

		// Add attributes that may have been removed
		attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

		// Iterate over the attributes to check, restoring the original value
		// or removing the attribute, respectively, if appropriate.
		for (String attrName : attrsToCheck) {
			Object attrValue = attributesSnapshot.get(attrName);
			if (attrValue == null){
				if (logger.isDebugEnabled()) {
					logger.debug("Removing attribute [" + attrName + "] after include");
				}
				request.removeAttribute(attrName);
			}
			else if (attrValue != request.getAttribute(attrName)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Restoring original value of attribute [" + attrName + "] after include");
				}
				request.setAttribute(attrName, attrValue);
			}
		}
	}

}
