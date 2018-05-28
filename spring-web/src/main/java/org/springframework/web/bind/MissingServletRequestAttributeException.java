/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * {@link ServletRequestBindingException} subclass that indicates a missing attribute.
 *
 * @author Carter Kozak
 * @since 5.0.7
 */
@SuppressWarnings("serial")
public class MissingServletRequestAttributeException extends ServletRequestBindingException {

	private final String attributeName;

	private final Class<?> attributeType;


	/**
	 * Constructor for MissingServletRequestAttributeException.
	 * @param attributeName the name of the missing attribute
	 * @param attributeType the expected type of the missing attribute
	 */
	public MissingServletRequestAttributeException(String attributeName, Class<?> attributeType) {
		super("");
		this.attributeName = attributeName;
		this.attributeType = attributeType;
	}


	@Override
	public String getMessage() {
		return "Missing request attribute '" + this.attributeName + "' of type " + this.attributeType.getSimpleName();
	}

	/**
	 * Return the name of the offending parameter.
	 */
	public final String getAttributeName() {
		return this.attributeName;
	}

	/**
	 * Return the expected type of the offending parameter.
	 */
	public final Class<?> getAttributeType() {
		return this.attributeType;
	}

}
