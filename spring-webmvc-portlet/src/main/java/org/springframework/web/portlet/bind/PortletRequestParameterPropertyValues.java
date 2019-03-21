/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.portlet.bind;

import javax.portlet.PortletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * PropertyValues implementation created from parameters in a PortletRequest.
 * Can look for all property values beginning with a certain prefix and
 * prefix separator (default is "_").
 *
 * <p>For example, with a prefix of "spring", "spring_param1" and
 * "spring_param2" result in a Map with "param1" and "param2" as keys.
 *
 * <p>This class is not immutable to be able to efficiently remove property
 * values that should be ignored for binding.
 *
 * @author Juergen Hoeller
 * @author John A. Lewis
 * @since 2.0
 * @see org.springframework.web.portlet.util.PortletUtils#getParametersStartingWith
 */
@SuppressWarnings("serial")
public class PortletRequestParameterPropertyValues extends MutablePropertyValues {

	/** Default prefix separator */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * Create new PortletRequestPropertyValues using no prefix
	 * (and hence, no prefix separator).
	 * @param request portlet request
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request) {
		this(request, null, null);
	}

	/**
	 * Create new PortletRequestPropertyValues using the given prefix and
	 * the default prefix separator (the underscore character "_").
	 * @param request portlet request
	 * @param prefix the prefix for parameters (the full prefix will
	 * consist of this plus the separator)
	 * @see #DEFAULT_PREFIX_SEPARATOR
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request, String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * Create new PortletRequestPropertyValues supplying both prefix and
	 * prefix separator.
	 * @param request portlet request
	 * @param prefix the prefix for parameters (the full prefix will
	 * consist of this plus the separator)
	 * @param prefixSeparator separator delimiting prefix (e.g. "spring")
	 * and the rest of the parameter name ("param1", "param2")
	 */
	public PortletRequestParameterPropertyValues(PortletRequest request, String prefix, String prefixSeparator) {
		super(PortletUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
