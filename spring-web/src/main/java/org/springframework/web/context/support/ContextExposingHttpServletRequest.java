/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.support;

import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * HttpServletRequest decorator that makes all Spring beans in a
 * given WebApplicationContext accessible as request attributes,
 * through lazy checking once an attribute gets accessed.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class ContextExposingHttpServletRequest extends HttpServletRequestWrapper {

	private final WebApplicationContext webApplicationContext;

	private final @Nullable Set<String> exposedContextBeanNames;

	private @Nullable Set<String> explicitAttributes;


	/**
	 * Create a new ContextExposingHttpServletRequest for the given request.
	 * @param originalRequest the original HttpServletRequest
	 * @param context the WebApplicationContext that this request runs in
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context) {
		this(originalRequest, context, null);
	}

	/**
	 * Create a new ContextExposingHttpServletRequest for the given request.
	 * @param originalRequest the original HttpServletRequest
	 * @param context the WebApplicationContext that this request runs in
	 * @param exposedContextBeanNames the names of beans in the context which
	 * are supposed to be exposed (if this is non-null, only the beans in this
	 * Set are eligible for exposure as attributes)
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context,
			@Nullable Set<String> exposedContextBeanNames) {

		super(originalRequest);
		Assert.notNull(context, "WebApplicationContext must not be null");
		this.webApplicationContext = context;
		this.exposedContextBeanNames = exposedContextBeanNames;
	}


	/**
	 * Return the WebApplicationContext that this request runs in.
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	@Override
	public @Nullable Object getAttribute(String name) {
		if ((this.explicitAttributes == null || !this.explicitAttributes.contains(name)) &&
				(this.exposedContextBeanNames == null || this.exposedContextBeanNames.contains(name)) &&
				this.webApplicationContext.containsBean(name)) {
			return this.webApplicationContext.getBean(name);
		}
		else {
			return super.getAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, value);
		if (this.explicitAttributes == null) {
			this.explicitAttributes = new HashSet<>(8);
		}
		this.explicitAttributes.add(name);
	}

}
