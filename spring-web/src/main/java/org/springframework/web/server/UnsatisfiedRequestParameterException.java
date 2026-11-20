/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
