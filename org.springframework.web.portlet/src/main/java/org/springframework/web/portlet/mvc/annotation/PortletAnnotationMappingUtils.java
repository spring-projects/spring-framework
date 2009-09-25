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

package org.springframework.web.portlet.mvc.annotation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.portlet.ClientDataRequest;
import javax.portlet.PortletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * Helper class for annotation-based request mapping.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
abstract class PortletAnnotationMappingUtils {

	/**
	 * Check whether the given portlet modes matches the specified type-level modes.
	 * @param modes the mapped portlet modes to check
	 * @param typeLevelModes the type-level mode mappings to check against
	 */
	public static boolean validateModeMapping(String[] modes, String[] typeLevelModes) {
		if (!ObjectUtils.isEmpty(modes) && !ObjectUtils.isEmpty(typeLevelModes)) {
			for (String mode : modes) {
				boolean match = false;
				for (String typeLevelMode : typeLevelModes) {
					if (mode.equalsIgnoreCase(typeLevelMode)) {
						match = true;
					}
				}
				if (!match) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check whether the given request matches the specified request methods.
	 * @param methods the request methods to check against
	 * @param request the current request to check
	 */
	public static boolean checkRequestMethod(RequestMethod[] methods, PortletRequest request) {
		if (methods.length == 0) {
			return true;
		}
		if (!(request instanceof ClientDataRequest)) {
			return false;
		}
		String method = ((ClientDataRequest) request).getMethod();
		for (RequestMethod candidate : methods) {
			if (method.equals(candidate.name())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given request matches the specified request methods.
	 * @param methods the request methods to check against
	 * @param request the current request to check
	 */
	public static boolean checkRequestMethod(Set<String> methods, PortletRequest request) {
		if (!methods.isEmpty()) {
			if (!(request instanceof ClientDataRequest)) {
				return false;
			}
			String method = ((ClientDataRequest) request).getMethod();
			if (!methods.contains(method)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the given request matches the specified parameter conditions.
	 * @param params the parameter conditions, following
	 * {@link org.springframework.web.bind.annotation.RequestMapping#params()}
	 * @param request the current request to check
	 */
	public static boolean checkParameters(String[] params, PortletRequest request) {
		if (!ObjectUtils.isEmpty(params)) {
			for (String param : params) {
				int separator = param.indexOf('=');
				if (separator == -1) {
					if (param.startsWith("!")) {
						if (PortletUtils.hasSubmitParameter(request, param.substring(1))) {
							return false;
						}
					}
					else if (!PortletUtils.hasSubmitParameter(request, param)) {
						return false;
					}
				}
				else {
					String key = param.substring(0, separator);
					String value = param.substring(separator + 1);
					if (!value.equals(request.getParameter(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Check whether the given request matches the specified header conditions.
	 * @param headers the header conditions, following {@link RequestMapping#headers()}
	 * @param request the current HTTP request to check
	 */
	public static boolean checkHeaders(String[] headers, PortletRequest request) {
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				int separator = header.indexOf('=');
				if (separator == -1) {
					if (header.startsWith("!")) {
						if (request.getProperty(header.substring(1)) != null) {
							return false;
						}
					}
					else if (request.getProperty(header) == null) {
						return false;
					}
				}
				else {
					String key = header.substring(0, separator);
					String value = header.substring(separator + 1);
					if (isMediaTypeHeader(key)) {
						List<MediaType> requestMediaTypes = MediaType.parseMediaTypes(request.getProperty(key));
						List<MediaType> valueMediaTypes = MediaType.parseMediaTypes(value);
						boolean found = false;
						for (Iterator<MediaType> valIter = valueMediaTypes.iterator(); valIter.hasNext() && !found;) {
							MediaType valueMediaType = valIter.next();
							for (Iterator<MediaType> reqIter = requestMediaTypes.iterator(); reqIter.hasNext() && !found;) {
								MediaType requestMediaType = reqIter.next();
								if (valueMediaType.includes(requestMediaType)) {
									found = true;
								}
							}

						}
						if (!found) {
							return false;
						}
					}
					else if (!value.equals(request.getProperty(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static boolean isMediaTypeHeader(String headerName) {
		return "Accept".equalsIgnoreCase(headerName) || "Content-Type".equalsIgnoreCase(headerName);
	}

}
