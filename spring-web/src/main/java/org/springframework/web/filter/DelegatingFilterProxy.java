/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Proxy for a standard Servlet Filter, delegating to a Spring-managed bean that
 * implements the Filter interface. Supports a "targetBeanName" filter init-param
 * in {@code web.xml}, specifying the name of the target bean in the Spring
 * application context.
 *
 * <p>{@code web.xml} will usually contain a {@code DelegatingFilterProxy} definition,
 * with the specified {@code filter-name} corresponding to a bean name in
 * Spring's root application context. All calls to the filter proxy will then
 * be delegated to that bean in the Spring context, which is required to implement
 * the standard Servlet Filter interface.
 *
 * <p>This approach is particularly useful for Filter implementations with complex
 * setup needs, allowing to apply the full Spring bean definition machinery to
 * Filter instances. Alternatively, consider standard Filter setup in combination
 * with looking up service beans from the Spring root application context.
 *
 * <p><b>NOTE:</b> The lifecycle methods defined by the Servlet Filter interface
 * will by default <i>not</i> be delegated to the target bean, relying on the
 * Spring application context to manage the lifecycle of that bean. Specifying
 * the "targetFilterLifecycle" filter init-param as "true" will enforce invocation
 * of the {@code Filter.init} and {@code Filter.destroy} lifecycle methods
 * on the target bean, letting the Servlet container manage the filter lifecycle.
 *
 * <p>{@code DelegatingFilterProxy} can optionally accept constructor parameters
 * when using a Servlet container's instance-based filter registration methods,
 * usually in conjunction with Spring's
 * {@link org.springframework.web.WebApplicationInitializer} SPI. These constructors allow
 * for providing the delegate Filter bean directly, or providing the application context
 * and bean name to fetch, avoiding the need to look up the application context from the
 * ServletContext.
 *
 * <p>This class was originally inspired by Spring Security's {@code FilterToBeanProxy}
 * class, written by Ben Alex.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @since 1.2
 * @see #setTargetBeanName
 * @see #setTargetFilterLifecycle
 * @see javax.servlet.Filter#doFilter
 * @see javax.servlet.Filter#init
 * @see javax.servlet.Filter#destroy
 * @see #DelegatingFilterProxy(Filter)
 * @see #DelegatingFilterProxy(String)
 * @see #DelegatingFilterProxy(String, WebApplicationContext)
 * @see javax.servlet.ServletContext#addFilter(String, Filter)
 * @see org.springframework.web.WebApplicationInitializer
 */
public class DelegatingFilterProxy extends GenericFilterBean {

	@Nullable
	private String contextAttribute;

	@Nullable
	private WebApplicationContext webApplicationContext;

	@Nullable
	private String targetBeanName;

	private boolean targetFilterLifecycle = false;

	@Nullable
	private volatile Filter delegate;

	private final Object delegateMonitor = new Object();


	/**
	 * Create a new {@code DelegatingFilterProxy}. For traditional use in {@code web.xml}.
	 * @see #setTargetBeanName(String)
	 */
	public DelegatingFilterProxy() {
	}

	/**
	 * Create a new {@code DelegatingFilterProxy} with the given {@link Filter} delegate.
	 * Bypasses entirely the need for interacting with a Spring application context,
	 * specifying the {@linkplain #setTargetBeanName target bean name}, etc.
	 * <p>For use with instance-based registration of filters.
	 * @param delegate the {@code Filter} instance that this proxy will delegate to and
	 * manage the lifecycle for (must not be {@code null}).
	 * @see #doFilter(ServletRequest, ServletResponse, FilterChain)
	 * @see #invokeDelegate(Filter, ServletRequest, ServletResponse, FilterChain)
	 * @see #destroy()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(Filter delegate) {
		Assert.notNull(delegate, "Delegate Filter must not be null");
		this.delegate = delegate;
	}

	/**
	 * Create a new {@code DelegatingFilterProxy} that will retrieve the named target
	 * bean from the Spring {@code WebApplicationContext} found in the {@code ServletContext}
	 * (either the 'root' application context or the context named by
	 * {@link #setContextAttribute}).
	 * <p>For use with instance-based registration of filters.
	 * <p>The target bean must implement the standard Servlet Filter interface.
	 * @param targetBeanName name of the target filter bean to look up in the Spring
	 * application context (must not be {@code null}).
	 * @see #findWebApplicationContext()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(String targetBeanName) {
		this(targetBeanName, null);
	}

	/**
	 * Create a new {@code DelegatingFilterProxy} that will retrieve the named target
	 * bean from the given Spring {@code WebApplicationContext}.
	 * <p>For use with instance-based registration of filters.
	 * <p>The target bean must implement the standard Servlet Filter interface.
	 * <p>The given {@code WebApplicationContext} may or may not be refreshed when passed
	 * in. If it has not, and if the context implements {@link ConfigurableApplicationContext},
	 * a {@link ConfigurableApplicationContext#refresh() refresh()} will be attempted before
	 * retrieving the named target bean.
	 * <p>This proxy's {@code Environment} will be inherited from the given
	 * {@code WebApplicationContext}.
	 * @param targetBeanName name of the target filter bean in the Spring application
	 * context (must not be {@code null}).
	 * @param wac the application context from which the target filter will be retrieved;
	 * if {@code null}, an application context will be looked up from {@code ServletContext}
	 * as a fallback.
	 * @see #findWebApplicationContext()
	 * @see #setEnvironment(org.springframework.core.env.Environment)
	 */
	public DelegatingFilterProxy(String targetBeanName, @Nullable WebApplicationContext wac) {
		Assert.hasText(targetBeanName, "Target Filter bean name must not be null or empty");
		setTargetBeanName(targetBeanName);
		this.webApplicationContext = wac;
		if (wac != null) {
			setEnvironment(wac.getEnvironment());
		}
	}

	/**
	 * Set the name of the ServletContext attribute which should be used to retrieve the
	 * {@link WebApplicationContext} from which to load the delegate {@link Filter} bean.
	 */
	public void setContextAttribute(@Nullable String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * Return the name of the ServletContext attribute which should be used to retrieve the
	 * {@link WebApplicationContext} from which to load the delegate {@link Filter} bean.
	 */
	@Nullable
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * Set the name of the target bean in the Spring application context.
	 * The target bean must implement the standard Servlet Filter interface.
	 * <p>By default, the {@code filter-name} as specified for the
	 * DelegatingFilterProxy in {@code web.xml} will be used.
	 */
	public void setTargetBeanName(@Nullable String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Return the name of the target bean in the Spring application context.
	 */
	@Nullable
	protected String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * Set whether to invoke the {@code Filter.init} and
	 * {@code Filter.destroy} lifecycle methods on the target bean.
	 * <p>Default is "false"; target beans usually rely on the Spring application
	 * context for managing their lifecycle. Setting this flag to "true" means
	 * that the servlet container will control the lifecycle of the target
	 * Filter, with this proxy delegating the corresponding calls.
	 */
	public void setTargetFilterLifecycle(boolean targetFilterLifecycle) {
		this.targetFilterLifecycle = targetFilterLifecycle;
	}

	/**
	 * Return whether to invoke the {@code Filter.init} and
	 * {@code Filter.destroy} lifecycle methods on the target bean.
	 */
	protected boolean isTargetFilterLifecycle() {
		return this.targetFilterLifecycle;
	}


	@Override
	protected void initFilterBean() throws ServletException {
		synchronized (this.delegateMonitor) {
			if (this.delegate == null) {
				// If no target bean name specified, use filter name.
				if (this.targetBeanName == null) {
					this.targetBeanName = getFilterName();
				}
				// Fetch Spring root application context and initialize the delegate early,
				// if possible. If the root application context will be started after this
				// filter proxy, we'll have to resort to lazy initialization.
				WebApplicationContext wac = findWebApplicationContext();
				if (wac != null) {
					this.delegate = initDelegate(wac);
				}
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Lazily initialize the delegate if necessary.
		Filter delegateToUse = this.delegate;
		if (delegateToUse == null) {
			synchronized (this.delegateMonitor) {
				delegateToUse = this.delegate;
				if (delegateToUse == null) {
					WebApplicationContext wac = findWebApplicationContext();
					if (wac == null) {
						throw new IllegalStateException("No WebApplicationContext found: " +
								"no ContextLoaderListener or DispatcherServlet registered?");
					}
					delegateToUse = initDelegate(wac);
				}
				this.delegate = delegateToUse;
			}
		}

		// Let the delegate perform the actual doFilter operation.
		invokeDelegate(delegateToUse, request, response, filterChain);
	}

	@Override
	public void destroy() {
		Filter delegateToUse = this.delegate;
		if (delegateToUse != null) {
			destroyDelegate(delegateToUse);
		}
	}


	/**
	 * Return the {@code WebApplicationContext} passed in at construction time, if available.
	 * Otherwise, attempt to retrieve a {@code WebApplicationContext} from the
	 * {@code ServletContext} attribute with the {@linkplain #setContextAttribute
	 * configured name} if set. Otherwise look up a {@code WebApplicationContext} under
	 * the well-known "root" application context attribute. The
	 * {@code WebApplicationContext} must have already been loaded and stored in the
	 * {@code ServletContext} before this filter gets initialized (or invoked).
	 * <p>Subclasses may override this method to provide a different
	 * {@code WebApplicationContext} retrieval strategy.
	 * @return the {@code WebApplicationContext} for this proxy, or {@code null} if not found
	 * @see #DelegatingFilterProxy(String, WebApplicationContext)
	 * @see #getContextAttribute()
	 * @see WebApplicationContextUtils#getWebApplicationContext(javax.servlet.ServletContext)
	 * @see WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
	 */
	@Nullable
	protected WebApplicationContext findWebApplicationContext() {
		if (this.webApplicationContext != null) {
			// The user has injected a context at construction time -> use it...
			if (this.webApplicationContext instanceof ConfigurableApplicationContext) {
				ConfigurableApplicationContext cac = (ConfigurableApplicationContext) this.webApplicationContext;
				if (!cac.isActive()) {
					// The context has not yet been refreshed -> do so before returning it...
					cac.refresh();
				}
			}
			return this.webApplicationContext;
		}
		String attrName = getContextAttribute();
		if (attrName != null) {
			return WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		}
		else {
			return WebApplicationContextUtils.findWebApplicationContext(getServletContext());
		}
	}

	/**
	 * Initialize the Filter delegate, defined as bean the given Spring
	 * application context.
	 * <p>The default implementation fetches the bean from the application context
	 * and calls the standard {@code Filter.init} method on it, passing
	 * in the FilterConfig of this Filter proxy.
	 * @param wac the root application context
	 * @return the initialized delegate Filter
	 * @throws ServletException if thrown by the Filter
	 * @see #getTargetBeanName()
	 * @see #isTargetFilterLifecycle()
	 * @see #getFilterConfig()
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	protected Filter initDelegate(WebApplicationContext wac) throws ServletException {
		String targetBeanName = getTargetBeanName();
		Assert.state(targetBeanName != null, "No target bean name set");
		Filter delegate = wac.getBean(targetBeanName, Filter.class);
		if (isTargetFilterLifecycle()) {
			delegate.init(getFilterConfig());
		}
		return delegate;
	}

	/**
	 * Actually invoke the delegate Filter with the given request and response.
	 * @param delegate the delegate Filter
	 * @param request the current HTTP request
	 * @param response the current HTTP response
	 * @param filterChain the current FilterChain
	 * @throws ServletException if thrown by the Filter
	 * @throws IOException if thrown by the Filter
	 */
	protected void invokeDelegate(
			Filter delegate, ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		delegate.doFilter(request, response, filterChain);
	}

	/**
	 * Destroy the Filter delegate.
	 * Default implementation simply calls {@code Filter.destroy} on it.
	 * @param delegate the Filter delegate (never {@code null})
	 * @see #isTargetFilterLifecycle()
	 * @see javax.servlet.Filter#destroy()
	 */
	protected void destroyDelegate(Filter delegate) {
		if (isTargetFilterLifecycle()) {
			delegate.destroy();
		}
	}

}
