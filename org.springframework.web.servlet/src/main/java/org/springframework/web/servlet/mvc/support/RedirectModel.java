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

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.validation.DataBinder;

/**
 * A {@link Model} implementation that controllers can use when they wish to 
 * redirect. For a redirect a controller needs to use an empty model and 
 * only add those attributes that will be used in the redirect URL --
 * either embedded as URI template variables or appended as query parameters. 
 * To be used in the URL such attributes need to be formatted as String 
 * values. Alternatively a controller may choose to keep attributes in 
 * flash storage instead for the duration of the redirect.
 * 
 * <p>A RedirectModel serves the above needs as follows:
 * <ul>
 * 	<li>Formats attribute values as Strings before adding them using a 
 * 	registered {@link Converter}, {@link Formatter}, {@link PropertyEditor},
 * 	or the attribute's {@link #toString()} method.
 * 	<li>Wraps the the model of the current request and provides a method to
 * 	copy attributes from it	(see {@link #addModelAttributes(String...)}).
 * 	<li>Provides methods to store attributes candidate for flash storage.
 * </ul>
 * 
 * <p>Note that a RedirectModel will not be used unless the controller decides 
 * to redirect. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class RedirectModel extends ExtendedModelMap {
	
	private final DataBinder dataBinder;

	private final ModelMap flashAttributes = new ModelMap();

	private final ModelMap implicitModel;
	
	/**
	 * Create a new instance without a DataBinder. Attribute values will be 
	 * formatted via {@link #toString()}.
	 */
	public RedirectModel() {
		this(null, null);
	}

	/**
	 * Create a new instance providing a DataBinder to use for formatting 
	 * attribute values. 
	 * @param dataBinder a DataBinder for converting attribute values to String.
	 * @param implicitModel the implicit model for the current request; 
	 * used in conjunction with {@link #addModelAttributes(String...)} 
	 * to copy attributes from the implicit model to the redirect model. 
	 */
	public RedirectModel(DataBinder dataBinder, ModelMap implicitModel) {
		this.dataBinder = dataBinder;
		this.implicitModel = implicitModel;
	}

	/**
	 * Return the attributes candidate for flash storage.
	 */
	public Map<String, ?> getFlashAttributes() {
		return flashAttributes;
	}

	/**
	 * Add an attribute. Before being added the attribute value is formatted as 
	 * a String in preparation for use in the redirect URL. If the attribute 
	 * value is null it is not be added to the model. 
	 * @param attributeName the attribute name; never null
	 * @param attributeValue the attribute value; skipped if null
	 */
	public RedirectModel addAttribute(String attributeName, Object attributeValue) {
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
	public RedirectModel addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	/**
	 * Copy all attributes in the supplied <code>Collection</code> into this
	 * Model using attribute name generation for each element. 
	 * @see #addAttribute(Object)
	 */
	public RedirectModel addAllAttributes(Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	/**
	 * Copy all supplied attributes into this redirect model.
	 * @see #addAttribute(String, Object)
	 */
	public RedirectModel addAllAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				addAttribute(key, attributes.get(key));
			}
		}
		return this;
	}

	/**
	 * Copy all supplied attributes into this redirect model with with existing 
	 * attributes of the same name taking precedence (i.e. not getting replaced).
	 * @see #addAttribute(String, Object)
	 */
	public RedirectModel mergeAttributes(Map<String, ?> attributes) {
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				if (!containsKey(key)) {
					addAttribute(key, attributes.get(key));
				}
			}
		}
		return this;
	}

	/**
	 * Copy the attributes specified by name from the "implicit" model of the 
	 * current request to this redirect model instance.
	 * @param attributeNames the names of attributes present in the implicit model.
	 * attribute names are required to be present; if an attribute is present
	 * but is null, it is skipped
	 * @see #addAttribute(String, Object)
	 */
	public RedirectModel addModelAttributes(String... attributeNames) {
		Assert.notNull(this.implicitModel, "The implicit model has not been set.");
		for (String name : attributeNames) {
			Assert.isTrue(this.implicitModel.containsAttribute(name), name + " not found in implicit model");
			Object value = this.implicitModel.get(name);
			addAttribute(name, value);
		}
		return this;
	}

	/**
	 * Add the given attribute as a candidate for flash storage. 
	 * @param attributeName the flash attribute name; never null
	 * @param attributeValue the flash attribute value; may be null
	 */
	public RedirectModel addFlashAttribute(String attributeName, Object attributeValue) {
		this.flashAttributes.addAttribute(attributeName, attributeValue);
		return this;
	}
	
	/**
	 * Add the given attribute value as a candidate for flash storage using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}. 
	 * @param attributeValue the flash attribute value; never null
	 */
	public RedirectModel addFlashAttribute(Object attributeValue) {
		this.flashAttributes.addAttribute(attributeValue);
		return this;
	}

}
