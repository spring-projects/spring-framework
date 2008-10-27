/*
 * Copyright 2002-2008 the original author or authors.
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

import javax.portlet.PortletRequest;

import org.springframework.util.ObjectUtils;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * Helper class for annotation-based request mapping.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
abstract class PortletAnnotationMappingUtils {

	/**
	 * Check whether the given request matches the specified request methods.
	 * @param modes the mapped portlet modes to check
	 * @param typeLevelModes the type-level mode mappings to check against
	 */
	public static boolean validateModeMapping(String[] modes, String[] typeLevelModes) {
		if (!ObjectUtils.isEmpty(modes)) {
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
	 * Check whether the given request matches the specified parameter conditions.
	 * @param params the parameter conditions, following
	 * {@link org.springframework.web.bind.annotation.RequestMapping#params()}
	 * @param request the current HTTP request to check
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

}
