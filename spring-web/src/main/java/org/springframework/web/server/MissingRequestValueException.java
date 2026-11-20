/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server;

import org.springframework.core.MethodParameter;

/**
 * {@link ServerWebInputException} subclass that indicates a missing request
 * value such as a request header, cookie value, query parameter, etc.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
@SuppressWarnings("serial")
public class MissingRequestValueException extends ServerWebInputException {

	private final String name;

	private final Class<?> type;

	private final String label;


	public MissingRequestValueException(String name, Class<?> type, String label, MethodParameter parameter) {
		super("Required " + label + " '" + name + "' is not present.", parameter,
				null, null, new Object[] {label, name});

		this.name = name;
		this.type = type;
		this.label = label;
	}


	/**
	 * Return the name of the missing value, for example, the name of the missing request
	 * header, or cookie, etc.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the target type the value is converted when present.
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return a label that describes the request value, for example, "request header",
	 * "cookie value", etc. Use this to create a custom message.
	 */
	public String getLabel() {
		return this.label;
	}

}
