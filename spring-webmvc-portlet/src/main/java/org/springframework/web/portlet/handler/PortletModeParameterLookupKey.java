/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.portlet.handler;

import javax.portlet.PortletMode;

import org.springframework.util.ObjectUtils;

/**
 * Internal class used as lookup key, combining PortletMode and parameter value.
 *
 * @author Juergen Hoeller
 */
class PortletModeParameterLookupKey {

	private final PortletMode mode;

	private final String parameter;


	public PortletModeParameterLookupKey(PortletMode portletMode, String parameter) {
		this.mode = portletMode;
		this.parameter = parameter;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PortletModeParameterLookupKey)) {
			return false;
		}
		PortletModeParameterLookupKey otherKey = (PortletModeParameterLookupKey) other;
		return (this.mode.equals(otherKey.mode) &&
				ObjectUtils.nullSafeEquals(this.parameter, otherKey.parameter));
	}

	@Override
	public int hashCode() {
		return (this.mode.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.parameter));
	}

	@Override
	public String toString() {
		return "Portlet mode '" + this.mode + "', parameter '" + this.parameter + "'";
	}

}
