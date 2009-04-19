/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Proxy for a standard Servlet 2.3 Filter, delegating to a Spring-managed
 * bean that implements the Filter interface. Supports a "targetBeanName"
 * filter init-param in <code>web.xml</code>, specifying the name of the
 * target bean in the Spring application context.
 *
 * <p><code>web.xml</code> will usually contain a DelegatingFilterProxy definition,
 * with the specified <code>filter-name</code> corresponding to a bean name in
 * Spring's root application context. All calls to the filter proxy will then
 * be delegated to that bean in the Spring context, which is required to implement
 * the standard Servlet 2.3 Filter interface.
 *
 * <p>This approach is particularly useful for Filter implementation with complex
 * setup needs, allowing to apply the full Spring bean definition machinery to
 * Filter instances. Alternatively, consider standard Filter setup in combination
 * with looking up service beans from the Spring root application context.
 *
 * <p><b>NOTE:</b> The lifecycle methods defined by the Servlet Filter interface
 * will by default <i>not</i> be delegated to the target bean, relying on the
 * Spring application context to manage the lifecycle of that bean. Specifying
 * the "targetFilterLifecycle" filter init-param as "true" will enforce invocation
 * of the <code>Filter.init</code> and <code>Filter.destroy</code> lifecycle methods
 * on the target bean, letting the servlet container manage the filter lifecycle.
 *
 * <p>This class is inspired by Acegi Security's FilterToBeanProxy class,
 * written by Ben Alex.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see #setTargetBeanName
 * @see #setTargetFilterLifecycle
 * @see javax.servlet.Filter#doFilter
 * @see javax.servlet.Filter#init
 * @see javax.servlet.Filter#destroy
 */
public class DelegatingFilterProxy extends GenericFilterBean {

	private String contextAttribute;

	private String targetBeanName;

	private boolean targetFilterLifecycle = false;

	private Filter delegate;

	private final Object delegateMonitor = new Object();


	/**
	 * Set the name of the ServletContext attribute which should be used to retrieve the
	 * {@link WebApplicationContext} from which to load the delegate {@link Filter} bean.
	 */
	public void setContextAttribute(String contextAttribute) {
		this.contextAttribute = contextAttribute;
	}

	/**
	 * Return the name of the ServletContext attribute which should be used to retrieve the
	 * {@link WebApplicationContext} from which to load the delegate {@link Filter} bean.
	 */
	public String getContextAttribute() {
		return this.contextAttribute;
	}

	/**
	 * Set the name of the target bean in the Spring application context.
	 * The target bean must implement the standard Servlet 2.3 Filter interface.
	 * <p>By default, the <code>filter-name</code> as specified for the
	 * DelegatingFilterProxy in <code>web.xml</code> will be used.
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Return the name of the target bean in the Spring application context.
	 */
	protected String getTargetBeanName() {
		return this.targetBeanName;
	}

	/**
	 * Set whether to invoke the <code>Filter.init</code> and
	 * <code>Filter.destroy</code> lifecycle methods on the target bean.
	 * <p>Default is "false"; target beans usually rely on the Spring application
	 * context for managing their lifecycle. Setting this flag to "true" means
	 * that the servlet container will control the lifecycle of the target
	 * Filter, with this proxy delegating the corresponding calls.
	 */
	public void setTargetFilterLifecycle(boolean targetFilterLifecycle) {
		this.targetFilterLifecycle = targetFilterLifecycle;
	}

	/**
	 * Return whether to invoke the <code>Filter.init</code> and
	 * <code>Filter.destroy</code> lifecycle methods on the target bean.
	 */
	protected boolean isTargetFilterLifecycle() {
		return this.targetFilterLifecycle;
	}


	@Override
	protected void initFilterBean() throws ServletException {
		// If no target bean name specified, use filter name.
		if (this.targetBeanName == null) {
			this.targetBeanName = getFilterName();
		}

		// Fetch Spring root application context and initialize the delegate early,
		// if possible. If the root application context will be started after this
		// filter proxy, we'll have to resort to lazy initialization.
		synchronized (this.delegateMonitor) {
			WebApplicationContext wac = findWebApplicationContext();
			if (wac != null) {
				this.delegate = initDelegate(wac);
			}
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// Lazily initialize the delegate if necessary.
		Filter delegateToUse = null;
		synchronized (this.delegateMonitor) {
			if (this.delegate == null) {
				WebApplicationContext wac = findWebApplicationContext();
				if (wac == null) {
					throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
				}
				this.delegate = initDelegate(wac);
			}
			delegateToUse = this.delegate;
		}

		// Let the delegate perform the actual doFilter operation.
		invokeDelegate(delegateToUse, request, response, filterChain);
	}

	@Override
	public void destroy() {
		Filter delegateToUse = null;
		synchronized (this.delegateMonitor) {
			delegateToUse = this.delegate;
		}
		if (delegateToUse != null) {
			destroyDelegate(delegateToUse);
		}
	}


	/**
	 * Retrieve a <code>WebApplicationContext</code> from the <code>ServletContext</code>
	 * attribute with the {@link #setContextAttribute configured name}. The
	 * <code>WebApplicationContext</code> must have already been loaded and stored in the
	 * <code>ServletContext</code> before this filter gets initialized (or invoked).
	 * <p>Subclasses may override this method to provide a different
	 * <code>WebApplicationContext</code> retrieval strategy.
	 * @return the WebApplicationContext for this proxy, or <code>null</code> if not found
	 * @see #getContextAttribute()
	 */
	protected WebApplicationContext findWebApplicationContext() {
		String attrName = getContextAttribute();
		if (attrName != null) {
			return WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
		}
		else {
			return WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		}
	}

	/**
	 * Initialize the Filter delegate, defined as bean the given Spring
	 * application context.
	 * <p>The default implementation fetches the bean from the application context
	 * and calls the standard <code>Filter.init</code> method on it, passing
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
		Filter delegate = wac.getBean(getTargetBeanName(), Filter.class);
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
	 * Default implementation simply calls <code>Filter.destroy</code> on it.
	 * @param delegate the Filter delegate (never <code>null</code>)
	 * @see #isTargetFilterLifecycle()
	 * @see javax.servlet.Filter#destroy()
	 */
	protected void destroyDelegate(Filter delegate) {
		if (isTargetFilterLifecycle()) {
			delegate.destroy();
		}
	}

}
