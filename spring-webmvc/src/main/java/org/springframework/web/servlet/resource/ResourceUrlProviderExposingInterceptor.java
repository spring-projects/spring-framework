/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * An interceptor that exposes the {@link ResourceUrlProvider} instance it
 * is configured with as a request attribute.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ResourceUrlProviderExposingInterceptor implements HandlerInterceptor {

	/**
	 * Name of the request attribute that holds the {@link ResourceUrlProvider}.
	 */
	public static final String RESOURCE_URL_PROVIDER_ATTR = ResourceUrlProvider.class.getName();

	private final ResourceUrlProvider resourceUrlProvider;


	public ResourceUrlProviderExposingInterceptor(ResourceUrlProvider resourceUrlProvider) {
		Assert.notNull(resourceUrlProvider, "ResourceUrlProvider is required");
		this.resourceUrlProvider = resourceUrlProvider;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		try {
			request.setAttribute(RESOURCE_URL_PROVIDER_ATTR, this.resourceUrlProvider);
		}
		catch (ResourceUrlEncodingFilter.LookupPathIndexException ex) {
			throw new ServletRequestBindingException(ex.getMessage(), ex);
		}
		return true;
	}

}
