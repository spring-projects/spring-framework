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

package org.springframework.ui;

import java.util.Collection;
import java.util.Map;

/**
 * Subclass of {@link ModelMap} that implements the {@link Model} interface.
 * Java 5 specific like the {@code Model} interface itself.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
@SuppressWarnings("serial")
public class ExtendedModelMap extends ModelMap implements Model {

	@Override
	public ExtendedModelMap addAttribute(String attributeName, Object attributeValue) {
		super.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(Map<String, ?> attributes) {
		super.addAllAttributes(attributes);
		return this;
	}

	@Override
	public ExtendedModelMap mergeAttributes(Map<String, ?> attributes) {
		super.mergeAttributes(attributes);
		return this;
	}

	public Map<String, Object> asMap() {
		return this;
	}

}
