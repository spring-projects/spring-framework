/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.bind;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ServletRequestBindingException} subclass that indicates an unsatisfied
 * parameter condition, as typically expressed using an {@code @RequestMapping}
 * annotation at the {@code @Controller} type level.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.bind.annotation.RequestMapping#params()
 */
@SuppressWarnings("serial")
public class UnsatisfiedServletRequestParameterException extends ServletRequestBindingException {

	private final List<String[]> paramConditions;

	private final Map<String, String[]> actualParams;


	/**
	 * Create a new UnsatisfiedServletRequestParameterException.
	 * @param paramConditions the parameter conditions that have been violated
	 * @param actualParams the actual parameter Map associated with the ServletRequest
	 */
	public UnsatisfiedServletRequestParameterException(String[] paramConditions, Map<String, String[]> actualParams) {
		super("");
		this.paramConditions = Arrays.<String[]>asList(paramConditions);
		this.actualParams = actualParams;
	}

	/**
	 * Create a new UnsatisfiedServletRequestParameterException.
	 * @param paramConditions all sets of parameter conditions that have been violated
	 * @param actualParams the actual parameter Map associated with the ServletRequest
	 * @since 4.2
	 */
	public UnsatisfiedServletRequestParameterException(List<String[]> paramConditions,
			Map<String, String[]> actualParams) {

		super("");
		Assert.notEmpty(paramConditions, "Parameter conditions must not be empty");
		this.paramConditions = paramConditions;
		this.actualParams = actualParams;
	}


	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder("Parameter conditions ");
		int i = 0;
		for (String[] conditions : this.paramConditions) {
			if (i > 0) {
				sb.append(" OR ");
			}
			sb.append('"');
			sb.append(StringUtils.arrayToDelimitedString(conditions, ", "));
			sb.append('"');
			i++;
		}
		sb.append(" not met for actual request parameters: ");
		sb.append(requestParameterMapToString(this.actualParams));
		return sb.toString();
	}

	/**
	 * Return the parameter conditions that have been violated or the first group
	 * in case of multiple groups.
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public final String[] getParamConditions() {
		return this.paramConditions.get(0);
	}

	/**
	 * Return all parameter condition groups that have been violated.
	 * @since 4.2
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public final List<String[]> getParamConditionGroups() {
		return this.paramConditions;
	}

	/**
	 * Return the actual parameter Map associated with the ServletRequest.
	 * @see jakarta.servlet.ServletRequest#getParameterMap()
	 */
	public final Map<String, String[]> getActualParams() {
		return this.actualParams;
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

}
