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

package org.springframework.web.servlet.mvc.multiaction;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.web.util.UrlPathHelper;

/**
 * Abstract base class for URL-based {@link MethodNameResolver} implementations.
 *
 * <p>Provides infrastructure for mapping handlers to URLs and configurable
 * URL lookup. For information on the latter, see the
 * {@link #setAlwaysUseFullPath} "alwaysUseFullPath"}
 * and {@link #setUrlDecode "urlDecode"} properties.
 *
 * @author Juergen Hoeller
 * @since 14.01.2004
 * @deprecated as of 4.3, in favor of annotation-driven handler methods
 */
@Deprecated
public abstract class AbstractUrlMethodNameResolver implements MethodNameResolver {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * Set if URL lookup should always use full path within current servlet
	 * context. Else, the path within the current servlet mapping is used
	 * if applicable (i.e. in the case of a ".../*" servlet mapping in web.xml).
	 * Default is "false".
	 * @see org.springframework.web.util.UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if context path and request URI should be URL-decoded.
	 * Both are returned <i>undecoded</i> by the Servlet API,
	 * in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
	 * @see org.springframework.web.util.UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set the UrlPathHelper to use for resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple MethodNameResolvers
	 * and HandlerMappings.
	 * @see org.springframework.web.servlet.handler.AbstractUrlHandlerMapping#setUrlPathHelper
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}


	/**
	 * Retrieves the URL path to use for lookup and delegates to
	 * {@code getHandlerMethodNameForUrlPath}.
	 * Converts {@code null} values to NoSuchRequestHandlingMethodExceptions.
	 * @see #getHandlerMethodNameForUrlPath
	 */
	@Override
	public final String getHandlerMethodName(HttpServletRequest request)
			throws NoSuchRequestHandlingMethodException {

		String urlPath = this.urlPathHelper.getLookupPathForRequest(request);
		String name = getHandlerMethodNameForUrlPath(urlPath);
		if (name == null) {
			throw new NoSuchRequestHandlingMethodException(urlPath, request.getMethod(), request.getParameterMap());
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Returning handler method name '" + name + "' for lookup path: " + urlPath);
		}
		return name;
	}

	/**
	 * Return a method name that can handle this request, based on the
	 * given lookup path. Called by {@code getHandlerMethodName}.
	 * @param urlPath the URL path to use for lookup,
	 * according to the settings in this class
	 * @return a method name that can handle this request.
	 * Should return null if no matching method found.
	 * @see #getHandlerMethodName
	 * @see #setAlwaysUseFullPath
	 * @see #setUrlDecode
	 */
	protected abstract String getHandlerMethodNameForUrlPath(String urlPath);

}
