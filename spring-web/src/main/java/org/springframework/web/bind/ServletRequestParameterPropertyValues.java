/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind;

import jakarta.servlet.ServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.util.WebUtils;

/**
 * PropertyValues implementation created from parameters in a ServletRequest.
 * Can look for all property values beginning with a certain prefix and
 * prefix separator (default is "_").
 *
 * <p>For example, with a prefix of "spring", "spring_param1" and
 * "spring_param2" result in a Map with "param1" and "param2" as keys.
 *
 * <p>This class is not immutable to be able to efficiently remove property
 * values that should be ignored for binding.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.util.WebUtils#getParametersStartingWith
 */
@SuppressWarnings("serial")
public class ServletRequestParameterPropertyValues extends MutablePropertyValues {

	/** Default prefix separator. */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * Create new ServletRequestPropertyValues using no prefix
	 * (and hence, no prefix separator).
	 * @param request the HTTP request
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request) {
		this(request, null, null);
	}

	/**
	 * Create new ServletRequestPropertyValues using the given prefix and
	 * the default prefix separator (the underscore character "_").
	 * @param request the HTTP request
	 * @param prefix the prefix for parameters (the full prefix will
	 * consist of this plus the separator)
	 * @see #DEFAULT_PREFIX_SEPARATOR
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request, @Nullable String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * Create new ServletRequestPropertyValues supplying both prefix and
	 * prefix separator.
	 * @param request the HTTP request
	 * @param prefix the prefix for parameters (the full prefix will
	 * consist of this plus the separator)
	 * @param prefixSeparator separator delimiting prefix (for example, "spring")
	 * and the rest of the parameter name ("param1", "param2")
	 */
	public ServletRequestParameterPropertyValues(
			ServletRequest request, @Nullable String prefix, @Nullable String prefixSeparator) {

		super(WebUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
