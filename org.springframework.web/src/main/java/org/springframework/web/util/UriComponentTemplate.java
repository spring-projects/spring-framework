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

package org.springframework.web.util;

import org.springframework.util.Assert;

/**
 * Subclass of {@link UriTemplate} that operates on URI components, rather than full URIs.
 *
 * @author Arjen Poutsma
 * @since 3.1
 */
class UriComponentTemplate extends UriTemplate {

	private final UriComponent uriComponent;

	private boolean encodeUriVariableValues;

	UriComponentTemplate(String uriTemplate, UriComponent uriComponent, boolean encodeUriVariableValues) {
		super(uriTemplate);
		Assert.notNull(uriComponent, "'uriComponent' must not be null");
		this.uriComponent = uriComponent;
		this.encodeUriVariableValues = encodeUriVariableValues;
	}

	@Override
	protected String getVariableValueAsString(Object variableValue) {
		String variableValueString = super.getVariableValueAsString(variableValue);
		return encodeUriVariableValues ? UriUtils.encode(variableValueString, uriComponent, false) :
				variableValueString;
	}
}
