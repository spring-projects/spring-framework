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

package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

/**
 * A {@link ModelMap} that implements the {@link RedirectAttributes} interface.
 * 
 * <p>Attributes are formatted and stored as Strings so they can be used as URI 
 * variables or as query parameters in the redirect URL. Alternatively, 
 * attributes may also be added as flash attributes in order to request storing
 * them until the next request without affecting the redirect URL.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class RedirectAttributesModelMap extends ModelMap implements RedirectAttributes {

	private final DataBinder dataBinder;

	private final ModelMap flashAttributes = new ModelMap();

	/**
	 * Default constructor without a DataBinder. 
	 * Redirect attribute values will be formatted via {@link #toString()}.
	 */
	public RedirectAttributesModelMap() {
		this(null);
	}

	/**
	 * Constructor with a DataBinder to use to format redirect attribute values.
	 * @param dataBinder a DataBinder for converting attribute values to String.
	 */
	public RedirectAttributesModelMap(DataBinder dataBinder) {
		this.dataBinder = dataBinder;
	}

	/**
	 * Return the attributes candidate for flash storage.
	 */
	public Map<String, ?> getFlashAttributes() {
		return flashAttributes;
	}

	/**
	 * Format the attribute value as a String and add it. If the value is 
	 * {@code null} it is not be added. 
	 * @param attributeName the attribute name; never null
	 * @param attributeValue the attribute value; skipped if null
	 */
	public RedirectAttributesModelMap addAttribute(String attributeName, Object attributeValue) {
		if (attributeValue != null) {
			super.addAttribute(attributeName, formatValue(attributeValue));
		}
		return this;
	}

	private String formatValue(Object value) {
		return (dataBinder != null) ? dataBinder.convertIfNecessary(value, String.class) : value.toString();
	}

	/**
	 * Add an attribute using a 
	 * {@link org.springframework.core.Conventions#getVariableName generated name}. 
	 * Before being added the attribute value is formatted as a String.
	 * @param attributeValue the attribute value; never null
	 */
	public RedirectAttributesModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	/**
	 * Copy all attributes in the supplied <code>Collection</code> into this
	 * Model using attribute name generation for each element. 
	 * @see #addAttribute(Object)
	 */
	public RedirectAttributesModelMap addAllAttributes(Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	/**
	 * Copy all supplied attributes into this model.
	 * @see #addAttribute(String, Object)
	 */
	public RedirectAttributesModelMap addAllAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				addAttribute(key, attributes.get(key));
			}
		}
		return this;
	}

	/**
	 * Copy all supplied attributes into this model with with existing 
	 * attributes of the same name taking precedence (i.e. not getting replaced).
	 * @see #addAttribute(String, Object)
	 */
	public RedirectAttributesModelMap mergeAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				if (!containsKey(key)) {
					addAttribute(key, attributes.get(key));
				}
			}
		}
		return this;
	}

	public Map<String, Object> asMap() {
		return this;
	}

	/**
	 * Add the given attribute as a candidate for flash storage. 
	 * @param attributeName the flash attribute name; never null
	 * @param attributeValue the flash attribute value; may be null
	 */
	public RedirectAttributes addFlashAttribute(String attributeName, Object attributeValue) {
		this.flashAttributes.addAttribute(attributeName, attributeValue);
		return this;
	}
	
	/**
	 * Add the given attribute value as a candidate for flash storage using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}. 
	 * @param attributeValue the flash attribute value; never null
	 */
	public RedirectAttributes addFlashAttribute(Object attributeValue) {
		this.flashAttributes.addAttribute(attributeValue);
		return this;
	}

}
