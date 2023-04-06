/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.server;

import java.util.List;

import org.springframework.util.MultiValueMap;

/**
 * {@link ServerWebInputException} subclass that indicates an unsatisfied
 * parameter condition, as typically expressed using an {@code @RequestMapping}
 * annotation at the {@code @Controller} type level.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@SuppressWarnings("serial")
public class UnsatisfiedRequestParameterException extends ServerWebInputException {

	private final List<String> conditions;

	private final MultiValueMap<String, String> requestParams;


	public UnsatisfiedRequestParameterException(List<String> conditions, MultiValueMap<String, String> params) {
		super(initReason(conditions, params), null, null, null, new Object[] {conditions});
		this.conditions = conditions;
		this.requestParams = params;
		setDetail("Invalid request parameters.");
	}

	private static String initReason(List<String> conditions, MultiValueMap<String, String> queryParams) {
		StringBuilder sb = new StringBuilder("Parameter conditions ");
		int i = 0;
		for (String condition : conditions) {
			if (i > 0) {
				sb.append(" OR ");
			}
			sb.append('"').append(condition).append('"');
			i++;
		}
		sb.append(" not met for actual request parameters: ").append(queryParams);
		return sb.toString();
	}


	/**
	 * Return String representations of the unsatisfied condition(s).
	 */
	public List<String> getConditions() {
		return this.conditions;
	}

	/**
	 * Return the actual request parameters.
	 */
	public MultiValueMap<String, String> getRequestParams() {
		return this.requestParams;
	}

}
