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

package org.springframework.web.servlet;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

/**
 * Base servlet for Spring's web framework. Provides integration with
 * a Spring application context, in a JavaBean-based overall solution.
 *
 * <p>This class offers the following functionality:
 * <ul>
 * <li>Manages a {@link org.springframework.web.context.WebApplicationContext
 * WebApplicationContext} instance per servlet. The servlet's configuration is determined
 * by beans in the servlet's namespace.
 * <li>Publishes events on request processing, whether or not a request is
 * successfully handled.
 * </ul>
 *
 * <p>Subclasses must implement {@link #doService} to handle requests. Because this extends
 * {@link HttpServletBean} rather than HttpServlet directly, bean properties are
 * automatically mapped onto it. Subclasses can override {@link #initFrameworkServlet()}
 * for custom initialization.
 *
 * <p>Detects a "contextClass" parameter at the servlet init-param level,
 * falling back to the default context class,
 * {@link org.springframework.web.context.support.XmlWebApplicationContext
 * XmlWebApplicationContext}, if not found. Note that, with the default
 * {@code FrameworkServlet}, a custom context class needs to implement the
 * {@link org.springframework.web.context.ConfigurableWebApplicationContext
 * ConfigurableWebApplicationContext} SPI.
 *
 * <p>Accepts an optional "contextInitializerClasses" servlet init-param that
 * specifies one or more {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} classes. The managed web application context will be
 * delegated to these initializers, allowing for additional programmatic configuration,
 * e.g. adding property sources or activating profiles against the {@linkplain
 * org.springframework.context.ConfigurableApplicationContext#getEnvironment() context's
 * environment}. See also {@link org.springframework.web.context.ContextLoader} which
 * supports a "contextInitializerClasses" context-param with identical semantics for
 * the "root" web application context.
 *
 * <p>Passes a "contextConfigLocation" servlet init-param to the context instance,
 * parsing it into potentially multiple file paths which can be separated by any
 * number of commas and spaces, like "test-servlet.xml, myServlet.xml".
 * If not explicitly specified, the context implementation is supposed to build a
 * default location from the namespace of the servlet.
 *
 * <p>Note: In case of multiple config locations, later bean definitions will
 * override ones defined in earlier loaded files, at least when using Spring's
 * default ApplicationContext implementation. This can be leveraged to
 * deliberately override certain bean definitions via an extra XML file.
 *
 * <p>The default namespace is "'servlet-name'-servlet", e.g. "test-servlet" for a
 * servlet-name "test" (leading to a "/WEB-INF/test-servlet.xml" default location
 * with XmlWebApplicationContext). The namespace can also be set explicitly via
 * the "namespace" servlet init-param.
 *
 * <p>As of Spring 3.1, {@code FrameworkServlet} may now be injected with a web
 * application context, rather than creating its own internally. This is useful in Servlet
 * 3.0+ environments, which support programmatic registration of servlet instances. See
 * {@link #FrameworkServlet(WebApplicationContext)} Javadoc for details.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @see #doService
 * @see #setContextClass
 * @see #setContextConfigLocation
 * @see #setContextInitializerClasses
 * @see #setNamespace
 */
@SuppressWarnings("serial")
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

	/**
	 * Suffix for WebApplicationContext namespaces. If a servlet of this class is
	 * given the name "test" in a context, the namespace used by the servlet will
	 * resolve to "test-servlet".
	 */
	public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

	/**
	 * Default context class for FrameworkServlet.
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

	/**
	 * Prefix for the ServletContext attribute for the WebApplicationContext.
	 * The completion is the servlet name.
	 */
	public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";

	/**
	 * Any number of these characters are considered delimiters between
	 * multiple values in a single init-param String value.
	 */
	private static final String INIT_PARAM_DELIMITERS = ",; \t\n";


	/** ServletContext attribute to find the WebApplicationContext in. */
	@Nullable
	private String contextAttribute;

	/** WebApplicationContext implementation class to create. */
	private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

	/** WebApplicationContext id to assign. */
	@Nullable
	private String contextId;

	/** Namespace for this servlet. */
	@Nullable
	private String namespace;

	/** Explicit context config location. */
	@Nullable
	private String contextConfigLocation;

	/** Actual ApplicationContextInitializer instances to apply to the context. */
	private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers =
			new ArrayList<>();

	/** Comma-delimited ApplicationContextInitializer class names set through init param. */
	@Nullable
	private String contextInitializerClasses;

	/** Should we publish the context as a ServletContext attribute?. */
	private boolean publishContext = true;

	/** Should we publish a ServletRequestHandledEvent at the end of each request?. */
	private boolean publishEvents = true;

	/** Expose LocaleContext and RequestAttributes as inheritable for child threads?. */
	private boolean threadContextInheritable = false;

	/** Should we dispatch an HTTP OPTIONS request to {@link #doService}?. */
	private boolean dispatchOptionsRequest = false;

	/** Should we dispatch an HTTP TRACE request to {@link #doService}?. */
	private boolean dispatchTraceRequest = false;

	/** Should we set the status to 500 for unhandled failures? */
	private boolean shouldHandleFailure = false;

	/** WebApplicationContext for this servlet. */
	@Nullable
	private WebApplicationContext webApplicationContext;

	/** If the WebApplicationContext was injected via {@link #setApplicationContext}. */
	private boolean webApplicationContextInjected = false;

	/** Flag used to detect whether onRefresh has already been called. */
	private boolean refreshEventReceived = false;

	/** Whether to log potentially sensitive info (request params at DEBUG + headers at TRACE). */
	private boolean enableLoggingRequestDetails = false;


	/**
	 * Create a new {@code FrameworkServlet} that will create its own internal web
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
	 * indicates which {@link ApplicationContextInitializer} classes should be used to
	 * further configure the internal application context prior to refresh().
	 * @see #FrameworkServlet(WebApplicationContext)
	 */
	public FrameworkServlet() {
	}

	/**
	 * Create a new {@code FrameworkServlet} with the given web application context. This
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
	 * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
	 * of {@link ConfigurableWebApplicationContext} and (b) has <strong>not</strong>
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
	 * <li>Any {@link ApplicationContextInitializer ApplicationContextInitializers} specified through the
	 * "contextInitializerClasses" init-param or through the {@link
	 * #setContextInitializers} property will be applied.</li>
	 * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
	 * </ul>
	 * If the context has already been refreshed or does not implement
	 * {@code ConfigurableWebApplicationContext}, none of the above will occur under the
	 * assumption that the user has performed these actions (or not) per his or her
	 * specific needs.
	 * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
	 * @param webApplicationContext the context to use
	 * @see #initWebApplicationContext
	 * @see #configureAndRefreshWebApplicationContext
	 * @see org.springframework.web.WebApplicationInitializer
	 */
	public FrameworkServlet(WebApplicationContext webApplicationContext) {
		this.webApplicationContext = webApplicationContext;
	}


	/**
	 * Set the name of the ServletContext attribute which should be used to retrieve the
	 * {@link WebApplicationContext} that this servlet is supposed to use.
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * Return the name of the ServletContext attribute which should be used to retrieve the
	 * {@link WebApplicationContext} that this servlet is supposed to use.
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * Set a custom context class. This class must be of type
	 * {@link org.springframework.web.context.WebApplicationContext}.
	 * <p>When using the default FrameworkServlet implementation,
	 * the context class must also implement the
	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
	 * interface.
	 * @see #createWebApplicationContext
	 */
	public void setContextClass(Class<?> contextClass) {
		this.contextClass = contextClass;
	}

	/**
	 * Return the custom context class.
	 */
	public Class<?> getContextClass() {
		return this.contextClass;
	}

	/**
	 * Specify a custom WebApplicationContext id,
	 * to be used as serialization id for the underlying BeanFactory.
	 */
	public void setContextId(@Nullable String contextId) {
		this.contextId = contextId;
	}

	/**
	 * Return the custom WebApplicationContext id, if any.
	 */
	@Nullable
	public String getContextId() {
		return this.contextId;
	}

	/**
	 * Set a custom namespace for this servlet,
	 * to be used for building a default context config location.
	 */
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Return the namespace for this servlet, falling back to default scheme if
	 * no custom namespace was set: e.g. "test-servlet" for a servlet named "test".
	 */
	public String getNamespace() {
		return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
	}

	/**
	 * Set the context config location explicitly, instead of relying on the default
	 * location built from the namespace. This location string can consist of
	 * multiple locations separated by any number of commas and spaces.
	 */
	public void setContextConfigLocation(@Nullable String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
	}

	/**
	 * Return the explicit context config location, if any.
	 */
	@Nullable
	public String getContextConfigLocation() {
		return this.contextConfigLocation;
	}

	/**
	 * Specify which {@link ApplicationContextInitializer} instances should be used
	 * to initialize the application context used by this {@code FrameworkServlet}.
	 * @see #configureAndRefreshWebApplicationContext
	 * @see #applyInitializers
	 */
	@SuppressWarnings("unchecked")
	public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
		if (initializers != null) {
			for (ApplicationContextInitializer<?> initializer : initializers) {
				this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
			}
		}
	}

	/**
	 * Specify the set of fully-qualified {@link ApplicationContextInitializer} class
	 * names, per the optional "contextInitializerClasses" servlet init-param.
	 * @see #configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext)
	 * @see #applyInitializers(ConfigurableApplicationContext)
	 */
	public void setContextInitializerClasses(String contextInitializerClasses) {
		this.contextInitializerClasses = contextInitializerClasses;
	}

	/**
	 * Set whether to publish this servlet's context as a ServletContext attribute,
	 * available to all objects in the web container. Default is "true".
	 * <p>This is especially handy during testing, although it is debatable whether
	 * it's good practice to let other application objects access the context this way.
	 */
	public void setPublishContext(boolean publishContext) {
		this.publishContext = publishContext;
	}

	/**
	 * Set whether this servlet should publish a ServletRequestHandledEvent at the end
	 * of each request. Default is "true"; can be turned off for a slight performance
	 * improvement, provided that no ApplicationListeners rely on such events.
	 * @see org.springframework.web.context.support.ServletRequestHandledEvent
	 */
	public void setPublishEvents(boolean publishEvents) {
		this.publishEvents = publishEvents;
	}

	/**
	 * Set whether to expose the LocaleContext and RequestAttributes as inheritable
	 * for child threads (using an {@link java.lang.InheritableThreadLocal}).
	 * <p>Default is "false", to avoid side effects on spawned background threads.
	 * Switch this to "true" to enable inheritance for custom child threads which
	 * are spawned during request processing and only used for this request
	 * (that is, ending after their initial task, without reuse of the thread).
	 * <p><b>WARNING:</b> Do not use inheritance for child threads if you are
	 * accessing a thread pool which is configured to potentially add new threads
	 * on demand (e.g. a JDK {@link java.util.concurrent.ThreadPoolExecutor}),
	 * since this will expose the inherited context to such a pooled thread.
	 */
	public void setThreadContextInheritable(boolean threadContextInheritable) {
		this.threadContextInheritable = threadContextInheritable;
	}

	/**
	 * Set whether this servlet should dispatch an HTTP OPTIONS request to
	 * the {@link #doService} method.
	 * <p>Default in the {@code FrameworkServlet} is "false", applying
	 * {@link javax.servlet.http.HttpServlet}'s default behavior (i.e.enumerating
	 * all standard HTTP request methods as a response to the OPTIONS request).
	 * Note however that as of 4.3 the {@code DispatcherServlet} sets this
	 * property to "true" by default due to its built-in support for OPTIONS.
	 * <p>Turn this flag on if you prefer OPTIONS requests to go through the
	 * regular dispatching chain, just like other HTTP requests. This usually
	 * means that your controllers will receive those requests; make sure
	 * that those endpoints are actually able to handle an OPTIONS request.
	 * <p>Note that HttpServlet's default OPTIONS processing will be applied
	 * in any case if your controllers happen to not set the 'Allow' header
	 * (as required for an OPTIONS response).
	 */
	public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
		this.dispatchOptionsRequest = dispatchOptionsRequest;
	}

	/**
	 * Set whether this servlet should dispatch an HTTP TRACE request to
	 * the {@link #doService} method.
	 * <p>Default is "false", applying {@link javax.servlet.http.HttpServlet}'s
	 * default behavior (i.e. reflecting the message received back to the client).
	 * <p>Turn this flag on if you prefer TRACE requests to go through the
	 * regular dispatching chain, just like other HTTP requests. This usually
	 * means that your controllers will receive those requests; make sure
	 * that those endpoints are actually able to handle a TRACE request.
	 * <p>Note that HttpServlet's default TRACE processing will be applied
	 * in any case if your controllers happen to not generate a response
	 * of content type 'message/http' (as required for a TRACE response).
	 */
	public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
		this.dispatchTraceRequest = dispatchTraceRequest;
	}

	/**
	 * Whether to handle failures wth {@code response.sendError(500)} as opposed
	 * to letting them propagate to the container which may log a stacktrace
	 * even if an error dispatch (e.g. Spring Boot app) handles the exception.
	 * @param shouldHandleFailure whether to handle failures or propagate
	 * @since 5.1
	 */
	public void setShouldHandleFailure(boolean shouldHandleFailure) {
		this.shouldHandleFailure = shouldHandleFailure;
	}

	/**
	 * Whether to log request params at DEBUG level, and headers at TRACE level.
	 * Both may contain sensitive information.
	 * <p>By default set to {@code false} so that request details are not shown.
	 * @param enable whether to enable or not
	 * @since 5.1
	 */
	public void setEnableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	/**
	 * Whether logging of potentially sensitive, request details at DEBUG and
	 * TRACE level is allowed.
	 * @since 5.1
	 */
	public boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * Called by Spring via {@link ApplicationContextAware} to inject the current
	 * application context. This method allows FrameworkServlets to be registered as
	 * Spring beans inside an existing {@link WebApplicationContext} rather than
	 * {@link #findWebApplicationContext() finding} a
	 * {@link org.springframework.web.context.ContextLoaderListener bootstrapped} context.
	 * <p>Primarily added to support use in embedded servlet containers.
	 * @since 4.0
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext) {
			this.webApplicationContext = (WebApplicationContext) applicationContext;
			this.webApplicationContextInjected = true;
		}
	}


	/**
	 * Overridden method of {@link HttpServletBean}, invoked after any bean properties
	 * have been set. Creates this servlet's WebApplicationContext.
	 */
	@Override
	protected final void initServletBean() throws ServletException {
		getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
		if (logger.isInfoEnabled()) {
			logger.info("Initializing Servlet '" + getServletName() + "'");
		}
		long startTime = System.currentTimeMillis();

		try {
			this.webApplicationContext = initWebApplicationContext();
			initFrameworkServlet();
		}
		catch (ServletException | RuntimeException ex) {
			logger.error("Context initialization failed", ex);
			throw ex;
		}

		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': request parameters and headers will be " + value);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	/**
	 * Initialize and publish the WebApplicationContext for this servlet.
	 * <p>Delegates to {@link #createWebApplicationContext} for actual creation
	 * of the context. Can be overridden in subclasses.
	 * @return the WebApplicationContext instance
	 * @see #FrameworkServlet(WebApplicationContext)
	 * @see #setContextClass
	 * @see #setContextConfigLocation
	 */
	protected WebApplicationContext initWebApplicationContext() {
		WebApplicationContext rootContext =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		WebApplicationContext wac = null;

		if (this.webApplicationContext != null) {
			// A context instance was injected at construction time -> use it
			wac = this.webApplicationContext;
			if (wac instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
				if (!cwac.isActive()) {
					// The context has not yet been refreshed -> provide services such as
					// setting the parent context, setting the application context id, etc
					if (cwac.getParent() == null) {
						// The context instance was injected without an explicit parent -> set
						// the root application context (if any; may be null) as the parent
						cwac.setParent(rootContext);
					}
					configureAndRefreshWebApplicationContext(cwac);
				}
			}
		}
		if (wac == null) {
			// No context instance was injected at construction time -> see if one
			// has been registered in the servlet context. If one exists, it is assumed
			// that the parent context (if any) has already been set and that the
			// user has performed any initialization such as setting the context id
			wac = findWebApplicationContext();
		}
		if (wac == null) {
			// No context instance is defined for this servlet -> create a local one
			wac = createWebApplicationContext(rootContext);
		}

		if (!this.refreshEventReceived) {
			// Either the context is not a ConfigurableApplicationContext with refresh
			// support or the context injected at construction time had already been
			// refreshed -> trigger initial onRefresh manually here.
			onRefresh(wac);
		}

		if (this.publishContext) {
			// Publish the context as a servlet context attribute.
			String attrName = getServletContextAttributeName();
			getServletContext().setAttribute(attrName, wac);
		}

		return wac;
	}

	/**
	 * Retrieve a {@code WebApplicationContext} from the {@code ServletContext}
	 * attribute with the {@link #setContextAttribute configured name}. The
	 * {@code WebApplicationContext} must have already been loaded and stored in the
	 * {@code ServletContext} before this servlet gets initialized (or invoked).
	 * <p>Subclasses may override this method to provide a different
	 * {@code WebApplicationContext} retrieval strategy.
	 * @return the WebApplicationContext for this servlet, or {@code null} if not found
	 * @see #getContextAttribute()
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		String attrName = getContextAttribute();
		if (attrName == null) {
			return null;
		}
		WebApplicationContext wac =
				WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		if (wac == null) {
			throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
		}
		return wac;
	}

	/**
	 * Instantiate the WebApplicationContext for this servlet, either a default
	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}
	 * or a {@link #setContextClass custom context class}, if set.
	 * <p>This implementation expects custom contexts to implement the
	 * {@link org.springframework.web.context.ConfigurableWebApplicationContext}
	 * interface. Can be overridden in subclasses.
	 * <p>Do not forget to register this servlet instance as application listener on the
	 * created context (for triggering its {@link #onRefresh callback}, and to call
	 * {@link org.springframework.context.ConfigurableApplicationContext#refresh()}
	 * before returning the context instance.
	 * @param parent the parent ApplicationContext to use, or {@code null} if none
	 * @return the WebApplicationContext for this servlet
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
		Class<?> contextClass = getContextClass();
		if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
			throw new ApplicationContextException(
					"Fatal initialization error in servlet with name '" + getServletName() +
					"': custom WebApplicationContext class [" + contextClass.getName() +
					"] is not of type ConfigurableWebApplicationContext");
		}
		ConfigurableWebApplicationContext wac =
				(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);

		wac.setEnvironment(getEnvironment());
		wac.setParent(parent);
		String configLocation = getContextConfigLocation();
		if (configLocation != null) {
			wac.setConfigLocation(configLocation);
		}
		configureAndRefreshWebApplicationContext(wac);

		return wac;
	}

	protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
		if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
			// The application context id is still set to its original default value
			// -> assign a more useful id based on available information
			if (this.contextId != null) {
				wac.setId(this.contextId);
			}
			else {
				// Generate default id...
				wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX +
						ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
			}
		}

		wac.setServletContext(getServletContext());
		wac.setServletConfig(getServletConfig());
		wac.setNamespace(getNamespace());
		wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));

		// The wac environment's #initPropertySources will be called in any case when the context
		// is refreshed; do it eagerly here to ensure servlet property sources are in place for
		// use in any post-processing or initialization that occurs below prior to #refresh
		ConfigurableEnvironment env = wac.getEnvironment();
		if (env instanceof ConfigurableWebEnvironment) {
			((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
		}

		postProcessWebApplicationContext(wac);
		applyInitializers(wac);
		wac.refresh();
	}

	/**
	 * Instantiate the WebApplicationContext for this servlet, either a default
	 * {@link org.springframework.web.context.support.XmlWebApplicationContext}
	 * or a {@link #setContextClass custom context class}, if set.
	 * Delegates to #createWebApplicationContext(ApplicationContext).
	 * @param parent the parent WebApplicationContext to use, or {@code null} if none
	 * @return the WebApplicationContext for this servlet
	 * @see org.springframework.web.context.support.XmlWebApplicationContext
	 * @see #createWebApplicationContext(ApplicationContext)
	 */
	protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
		return createWebApplicationContext((ApplicationContext) parent);
	}

	/**
	 * Post-process the given WebApplicationContext before it is refreshed
	 * and activated as context for this servlet.
	 * <p>The default implementation is empty. {@code refresh()} will
	 * be called automatically after this method returns.
	 * <p>Note that this method is designed to allow subclasses to modify the application
	 * context, while {@link #initWebApplicationContext} is designed to allow
	 * end-users to modify the context through the use of
	 * {@link ApplicationContextInitializer ApplicationContextInitializers}.
	 * @param wac the configured WebApplicationContext (not refreshed yet)
	 * @see #createWebApplicationContext
	 * @see #initWebApplicationContext
	 * @see ConfigurableWebApplicationContext#refresh()
	 */
	protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
	}

	/**
	 * Delegate the WebApplicationContext before it is refreshed to any
	 * {@link ApplicationContextInitializer} instances specified by the
	 * "contextInitializerClasses" servlet init-param.
	 * <p>See also {@link #postProcessWebApplicationContext}, which is designed to allow
	 * subclasses (as opposed to end-users) to modify the application context, and is
	 * called immediately before this method.
	 * @param wac the configured WebApplicationContext (not refreshed yet)
	 * @see #createWebApplicationContext
	 * @see #postProcessWebApplicationContext
	 * @see ConfigurableApplicationContext#refresh()
	 */
	protected void applyInitializers(ConfigurableApplicationContext wac) {
		String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
		if (globalClassNames != null) {
			for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		if (this.contextInitializerClasses != null) {
			for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
				this.contextInitializers.add(loadInitializer(className, wac));
			}
		}

		AnnotationAwareOrderComparator.sort(this.contextInitializers);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
			initializer.initialize(wac);
		}
	}

	@SuppressWarnings("unchecked")
	private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(
			String className, ConfigurableApplicationContext wac) {
		try {
			Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						wac.getClass().getName()));
			}
			return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
		}
		catch (ClassNotFoundException ex) {
			throw new ApplicationContextException(String.format("Could not load class [%s] specified " +
					"via 'contextInitializerClasses' init-param", className), ex);
		}
	}

	/**
	 * Return the ServletContext attribute name for this servlet's WebApplicationContext.
	 * <p>The default implementation returns
	 * {@code SERVLET_CONTEXT_PREFIX + servlet name}.
	 * @see #SERVLET_CONTEXT_PREFIX
	 * @see #getServletName
	 */
	public String getServletContextAttributeName() {
		return SERVLET_CONTEXT_PREFIX + getServletName();
	}

	/**
	 * Return this servlet's WebApplicationContext.
	 */
	@Nullable
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	/**
	 * This method will be invoked after any bean properties have been set and
	 * the WebApplicationContext has been loaded. The default implementation is empty;
	 * subclasses may override this method to perform any initialization they require.
	 * @throws ServletException in case of an initialization exception
	 */
	protected void initFrameworkServlet() throws ServletException {
	}

	/**
	 * Refresh this servlet's application context, as well as the
	 * dependent state of the servlet.
	 * @see #getWebApplicationContext()
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	public void refresh() {
		WebApplicationContext wac = getWebApplicationContext();
		if (!(wac instanceof ConfigurableApplicationContext)) {
			throw new IllegalStateException("WebApplicationContext does not support refresh: " + wac);
		}
		((ConfigurableApplicationContext) wac).refresh();
	}

	/**
	 * Callback that receives refresh events from this servlet's WebApplicationContext.
	 * <p>The default implementation calls {@link #onRefresh},
	 * triggering a refresh of this servlet's context-dependent state.
	 * @param event the incoming ApplicationContext event
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.refreshEventReceived = true;
		onRefresh(event.getApplicationContext());
	}

	/**
	 * Template method which can be overridden to add servlet-specific refresh work.
	 * Called after successful context refresh.
	 * <p>This implementation is empty.
	 * @param context the current WebApplicationContext
	 * @see #refresh()
	 */
	protected void onRefresh(ApplicationContext context) {
		// For subclasses: do nothing by default.
	}

	/**
	 * Close the WebApplicationContext of this servlet.
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	@Override
	public void destroy() {
		getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
		// Only call close() on WebApplicationContext if locally managed...
		if (this.webApplicationContext instanceof ConfigurableApplicationContext && !this.webApplicationContextInjected) {
			((ConfigurableApplicationContext) this.webApplicationContext).close();
		}
	}


	/**
	 * Override the parent class implementation in order to intercept PATCH requests.
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
		if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
			processRequest(request, response);
		}
		else {
			super.service(request, response);
		}
	}

	/**
	 * Delegate GET requests to processRequest/doService.
	 * <p>Will also be invoked by HttpServlet's default implementation of {@code doHead},
	 * with a {@code NoBodyResponse} that just captures the content length.
	 * @see #doService
	 * @see #doHead
	 */
	@Override
	protected final void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * Delegate POST requests to {@link #processRequest}.
	 * @see #doService
	 */
	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * Delegate PUT requests to {@link #processRequest}.
	 * @see #doService
	 */
	@Override
	protected final void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * Delegate DELETE requests to {@link #processRequest}.
	 * @see #doService
	 */
	@Override
	protected final void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		processRequest(request, response);
	}

	/**
	 * Delegate OPTIONS requests to {@link #processRequest}, if desired.
	 * <p>Applies HttpServlet's standard OPTIONS processing otherwise,
	 * and also if there is still no 'Allow' header set after dispatching.
	 * @see #doService
	 */
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
			processRequest(request, response);
			if (response.containsHeader("Allow")) {
				// Proper OPTIONS response coming from a handler - we're done.
				return;
			}
		}

		// Use response wrapper in order to always add PATCH to the allowed methods
		super.doOptions(request, new HttpServletResponseWrapper(response) {
			@Override
			public void setHeader(String name, String value) {
				if ("Allow".equals(name)) {
					value = (StringUtils.hasLength(value) ? value + ", " : "") + HttpMethod.PATCH.name();
				}
				super.setHeader(name, value);
			}
		});
	}

	/**
	 * Delegate TRACE requests to {@link #processRequest}, if desired.
	 * <p>Applies HttpServlet's standard TRACE processing otherwise.
	 * @see #doService
	 */
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		if (this.dispatchTraceRequest) {
			processRequest(request, response);
			if ("message/http".equals(response.getContentType())) {
				// Proper TRACE response coming from a handler - we're done.
				return;
			}
		}
		super.doTrace(request, response);
	}

	/**
	 * Process this request, publishing an event regardless of the outcome.
	 * <p>The actual event handling is performed by the abstract
	 * {@link #doService} template method.
	 */
	protected final void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		Throwable failureCause = null;

		LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
		LocaleContext localeContext = buildLocaleContext(request);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());

		initContextHolders(request, localeContext, requestAttributes);

		try {
			doService(request, response);
		}
		catch (ServletException | IOException ex) {
			if (!handleFailure(request, response, ex)) {
				failureCause = ex;
				throw ex;
			}
		}
		catch (Throwable ex) {
			if (!handleFailure(request, response, ex)) {
				failureCause = ex;
				throw new NestedServletException("Request processing failed", ex);
			}
		}

		finally {
			resetContextHolders(request, previousLocaleContext, previousAttributes);
			if (requestAttributes != null) {
				requestAttributes.requestCompleted();
			}
			logResult(request, response, failureCause, asyncManager);
			publishRequestHandledEvent(request, response, startTime, failureCause);
		}
	}

	/**
	 * Build a LocaleContext for the given request, exposing the request's
	 * primary locale as current locale.
	 * @param request current HTTP request
	 * @return the corresponding LocaleContext, or {@code null} if none to bind
	 * @see LocaleContextHolder#setLocaleContext
	 */
	@Nullable
	protected LocaleContext buildLocaleContext(HttpServletRequest request) {
		return new SimpleLocaleContext(request.getLocale());
	}

	/**
	 * Build ServletRequestAttributes for the given request (potentially also
	 * holding a reference to the response), taking pre-bound attributes
	 * (and their type) into consideration.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param previousAttributes pre-bound RequestAttributes instance, if any
	 * @return the ServletRequestAttributes to bind, or {@code null} to preserve
	 * the previously bound instance (or not binding any, if none bound before)
	 * @see RequestContextHolder#setRequestAttributes
	 */
	@Nullable
	protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request,
			@Nullable HttpServletResponse response, @Nullable RequestAttributes previousAttributes) {

		if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
			return new ServletRequestAttributes(request, response);
		}
		else {
			return null;  // preserve the pre-bound RequestAttributes instance
		}
	}

	private void initContextHolders(HttpServletRequest request,
			@Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {

		if (localeContext != null) {
			LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
		}
		if (requestAttributes != null) {
			RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
		}
	}

	private void resetContextHolders(HttpServletRequest request,
			@Nullable LocaleContext prevLocaleContext, @Nullable RequestAttributes previousAttributes) {

		LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
		RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
	}

	private boolean handleFailure(HttpServletRequest request, HttpServletResponse response, Throwable ex) {
		if (this.shouldHandleFailure) {
			try {
				response.sendError(500);
			}
			catch (IOException ex2) {
				if (logger.isDebugEnabled()) {
					logger.debug("Handling of failure failed: " + ex2);
				}
				return false;
			}
			request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, 500);
			WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
			return true;
		}
		return false;
	}

	private void logResult(HttpServletRequest request, HttpServletResponse response,
			@Nullable Throwable failureCause, WebAsyncManager asyncManager) {

		if (!logger.isDebugEnabled()) {
			return;
		}

		String dispatchType = request.getDispatcherType().name();
		boolean initialDispatch = request.getDispatcherType().equals(DispatcherType.REQUEST);

		if (failureCause != null) {
			if (!initialDispatch) {
				// FORWARD/ERROR/ASYNC: minimal message (there should be enough context already)
				logger.debug("Unresolved failure from \"" + dispatchType + "\" dispatch: " + failureCause);
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Failed to complete request", failureCause);
			}
			else {
				logger.debug("Failed to complete request: " + failureCause);
			}
			return;
		}

		if (asyncManager.isConcurrentHandlingStarted()) {
			logger.debug("Exiting but response remains open for further handling");
			return;
		}

		int status = response.getStatus();
		String headers = ""; // nothing below trace

		if (logger.isTraceEnabled()) {
			Collection<String> names = response.getHeaderNames();
			if (this.enableLoggingRequestDetails) {
				headers = names.stream().map(name -> name + ":" + response.getHeaders(name))
						.collect(Collectors.joining(", "));
			}
			else {
				headers = names.isEmpty() ? "" : "masked";
			}
			headers = ", headers={" + headers + "}";
		}

		if (!initialDispatch) {
			logger.debug("Exiting from \"" + dispatchType + "\" dispatch, status " + status + headers);
		}
		else {
			HttpStatus httpStatus = HttpStatus.resolve(status);
			logger.debug("Completed " + (httpStatus != null ? httpStatus : status) + headers);
		}
	}

	private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response,
			long startTime, @Nullable Throwable failureCause) {

		if (this.publishEvents && this.webApplicationContext != null) {
			// Whether or not we succeeded, publish an event.
			long processingTime = System.currentTimeMillis() - startTime;
			this.webApplicationContext.publishEvent(
					new ServletRequestHandledEvent(this,
							request.getRequestURI(), request.getRemoteAddr(),
							request.getMethod(), getServletConfig().getServletName(),
							WebUtils.getSessionId(request), getUsernameForRequest(request),
							processingTime, failureCause, response.getStatus()));
		}
	}

	/**
	 * Determine the username for the given request.
	 * <p>The default implementation takes the name of the UserPrincipal, if any.
	 * Can be overridden in subclasses.
	 * @param request current HTTP request
	 * @return the username, or {@code null} if none found
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Nullable
	protected String getUsernameForRequest(HttpServletRequest request) {
		Principal userPrincipal = request.getUserPrincipal();
		return (userPrincipal != null ? userPrincipal.getName() : null);
	}


	/**
	 * Subclasses must implement this method to do the work of request handling,
	 * receiving a centralized callback for GET, POST, PUT and DELETE.
	 * <p>The contract is essentially the same as that for the commonly overridden
	 * {@code doGet} or {@code doPost} methods of HttpServlet.
	 * <p>This class intercepts calls to ensure that exception handling and
	 * event publication takes place.
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @throws Exception in case of any kind of processing failure
	 * @see javax.servlet.http.HttpServlet#doGet
	 * @see javax.servlet.http.HttpServlet#doPost
	 */
	protected abstract void doService(HttpServletRequest request, HttpServletResponse response)
			throws Exception;


	/**
	 * ApplicationListener endpoint that receives events from this servlet's WebApplicationContext
	 * only, delegating to {@code onApplicationEvent} on the FrameworkServlet instance.
	 */
	private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			FrameworkServlet.this.onApplicationEvent(event);
		}
	}


	/**
	 * CallableProcessingInterceptor implementation that initializes and resets
	 * FrameworkServlet's context holders, i.e. LocaleContextHolder and RequestContextHolder.
	 */
	private class RequestBindingInterceptor implements CallableProcessingInterceptor {

		@Override
		public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
				initContextHolders(request, buildLocaleContext(request),
						buildRequestAttributes(request, response, null));
			}
		}
		@Override
		public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
			HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
			if (request != null) {
				resetContextHolders(request, null, null);
			}
		}
	}

}
