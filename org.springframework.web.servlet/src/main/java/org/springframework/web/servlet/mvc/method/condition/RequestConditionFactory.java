/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.util.ObjectUtils;

/**
 * Factory for {@link RequestCondition} objects.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class RequestConditionFactory {

	private static final RequestCondition TRUE_CONDITION = new RequestCondition() {
		public boolean match(HttpServletRequest request) {
			return true;
		}

		public int weight() {
			return 0;
		}

		@Override
		public String toString() {
			return "TRUE";
		}
	};

	private static final RequestCondition FALSE_CONDITION = new RequestCondition() {
		public boolean match(HttpServletRequest request) {
			return false;
		}

		public int weight() {
			return 0;
		}


		@Override
		public String toString() {
			return "FALSE";
		}
	};

	public static RequestCondition trueCondition() {
		return TRUE_CONDITION;
	}

	public static RequestCondition falseCondition() {
		return FALSE_CONDITION;
	}

	/**
	 * Combines the given conditions into a logical AND, i.e. the returned condition will return {@code true} for {@link
	 * RequestCondition#match(HttpServletRequest)} if all of the given conditions do so.
	 *
	 * @param conditions the conditions
	 * @return a condition that represents a logical AND
	 */
	public static RequestCondition and(RequestCondition... conditions) {
		List<RequestCondition> filteredConditions = new ArrayList<RequestCondition>(Arrays.asList(conditions));
		for (Iterator<RequestCondition> iterator = filteredConditions.iterator(); iterator.hasNext();) {
			RequestCondition condition = iterator.next();
			if (condition == TRUE_CONDITION) {
				iterator.remove();
			}
		}
		return new LogicalConjunctionRequestCondition(filteredConditions);
	}

	/**
	 * Combines the given conditions into a logical OR, i.e. the returned condition will return {@code true} for {@link
	 * RequestCondition#match(HttpServletRequest)} if any of the given conditions do so.
	 *
	 * @param conditions the conditions
	 * @return a condition that represents a logical OR
	 */
	public static RequestCondition or(RequestCondition... conditions) {
		List<RequestCondition> filteredConditions = new ArrayList<RequestCondition>(Arrays.asList(conditions));
		for (Iterator<RequestCondition> iterator = filteredConditions.iterator(); iterator.hasNext();) {
			RequestCondition condition = iterator.next();
			if (condition == TRUE_CONDITION) {
				return trueCondition();
			}
			else if (condition == FALSE_CONDITION) {
				iterator.remove();
			}
		}
		return new LogicalDisjunctionRequestCondition(filteredConditions);
	}

	/**
	 * Parses the given parameters, and returns them as a single request conditions.
	 *
	 * @param params the parameters
	 * @return the request condition
	 * @see org.springframework.web.bind.annotation.RequestMapping#params()
	 */
	public static RequestCondition parseParams(String... params) {
		if (ObjectUtils.isEmpty(params)) {
			return TRUE_CONDITION;
		}
		RequestCondition[] result = new RequestCondition[params.length];
		for (int i = 0; i < params.length; i++) {
			result[i] = new ParamRequestCondition(params[i]);
		}
		return and(result);
	}

	/**
	 * Parses the given headers, and returns them as a single request condition.
	 *
	 * @param headers the headers
	 * @return the request condition
	 * @see org.springframework.web.bind.annotation.RequestMapping#headers()
	 */
	public static RequestCondition parseHeaders(String... headers) {
		if (ObjectUtils.isEmpty(headers)) {
			return TRUE_CONDITION;
		}
		RequestCondition[] result = new RequestCondition[headers.length];
		for (int i = 0; i < headers.length; i++) {
			HeaderRequestCondition header = new HeaderRequestCondition(headers[i]);
			if (isMediaTypeHeader(header.name)) {
				result[i] = new MediaTypeHeaderRequestCondition(headers[i]);
			}
			else {
				result[i] = header;
			}
		}
		return and(result);
	}

	private static boolean isMediaTypeHeader(String name) {
		return "Accept".equalsIgnoreCase(name) || "Content-Type".equalsIgnoreCase(name);
	}

	public static RequestCondition parseConsumes(String... consumes) {
		if (ObjectUtils.isEmpty(consumes)) {
			return TRUE_CONDITION;
		}
		RequestCondition[] result = new RequestCondition[consumes.length];
		for (int i = 0; i < consumes.length; i++) {
			result[i] = new ConsumesRequestCondition(consumes[i]);
		}
		return or(result);
	}

	//
	// Conditions
	//

}
