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

package org.springframework.web.bind;

import java.util.Iterator;
import java.util.Map;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ServletRequestBindingException} subclass that indicates an unsatisfied
 * parameter condition, as typically expressed using an <code>@RequestMapping</code>
 * annotation at the <code>@Controller</code> type level.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.bind.annotation.RequestMapping#params()
 */
public class UnsatisfiedServletRequestParameterException extends ServletRequestBindingException {

	private final String[] paramConditions;

	private final Map<String, String[]> actualParams;


	/**
	 * Create a new UnsatisfiedServletRequestParameterException.
	 * @param paramConditions the parameter conditions that have been violated
	 * @param actualParams the actual parameter Map associated with the ServletRequest
	 */
	public UnsatisfiedServletRequestParameterException(String[] paramConditions, Map<String, String[]> actualParams) {
		super("");
		this.paramConditions = paramConditions;
		this.actualParams = actualParams;
	}


	@Override
	public String getMessage() {
		return "Parameter conditions \"" + StringUtils.arrayToDelimitedString(this.paramConditions, ", ") +
				"\" not met for actual request parameters: " + requestParameterMapToString(this.actualParams);
	}

	private static String requestParameterMapToString(Map<String, String[]> actualParams) {
		StringBuilder result = new StringBuilder();
		for (Iterator<Map.Entry<String, String[]>> it = actualParams.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, String[]> entry = it.next();
			result.append(entry.getKey()).append('=').append(ObjectUtils.nullSafeToString(entry.getValue()));
			if (it.hasNext()) {
				result.append(", ");
			}
		}
		return result.toString();
	}

	/**
	 * Return the parameter conditions that have been violated.
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public final String[] getParamConditions() {
		return this.paramConditions;
	}

	/**
	 * Return the actual parameter Map associated with the ServletRequest.
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	public final Map<String, String[]> getActualParams() {
		return this.actualParams;
	}

}
