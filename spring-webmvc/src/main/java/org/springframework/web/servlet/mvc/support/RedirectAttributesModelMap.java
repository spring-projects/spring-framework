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

package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.ui.ModelMap;
import org.springframework.validation.DataBinder;

/**
 * A {@link ModelMap} implementation of {@link RedirectAttributes} that formats
 * values as Strings using a {@link DataBinder}. Also provides a place to store
 * flash attributes so they can survive a redirect without the need to be
 * embedded in the redirect URL.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class RedirectAttributesModelMap extends ModelMap implements RedirectAttributes {

	private final DataBinder dataBinder;

	private final ModelMap flashAttributes = new ModelMap();

	/**
	 * Class constructor.
	 * @param dataBinder used to format attribute values as Strings.
	 */
	public RedirectAttributesModelMap(DataBinder dataBinder) {
		this.dataBinder = dataBinder;
	}

	/**
	 * Default constructor without a DataBinder.
	 * Attribute values are converted to String via {@link #toString()}.
	 */
	public RedirectAttributesModelMap() {
		this(null);
	}

	/**
	 * Return the attributes candidate for flash storage or an empty Map.
	 */
	@Override
	public Map<String, ?> getFlashAttributes() {
		return this.flashAttributes;
	}

	/**
	 * {@inheritDoc}
	 * <p>Formats the attribute value as a String before adding it.
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(String attributeName, Object attributeValue) {
		super.addAttribute(attributeName, formatValue(attributeValue));
		return this;
	}

	private String formatValue(Object value) {
		if (value == null) {
			return null;
		}
		return (dataBinder != null) ? dataBinder.convertIfNecessary(value, String.class) : value.toString();
	}

	/**
	 * {@inheritDoc}
	 * <p>Formats the attribute value as a String before adding it.
	 */
	@Override
	public RedirectAttributesModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>Each attribute value is formatted as a String before being added.
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>Each attribute value is formatted as a String before being added.
	 */
	@Override
	public RedirectAttributesModelMap addAllAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				addAttribute(key, attributes.get(key));
			}
		}
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>Each attribute value is formatted as a String before being merged.
	 */
	@Override
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

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * <p>The value is formatted as a String before being added.
	 */
	@Override
	public Object put(String key, Object value) {
		return super.put(key, formatValue(value));
	}

	/**
	 * {@inheritDoc}
	 * <p>Each value is formatted as a String before being added.
	 */
	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		if (map != null) {
			for (String key : map.keySet()) {
				put(key, formatValue(map.get(key)));
			}
		}
	}

	@Override
	public RedirectAttributes addFlashAttribute(String attributeName, Object attributeValue) {
		this.flashAttributes.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public RedirectAttributes addFlashAttribute(Object attributeValue) {
		this.flashAttributes.addAttribute(attributeValue);
		return this;
	}

}
