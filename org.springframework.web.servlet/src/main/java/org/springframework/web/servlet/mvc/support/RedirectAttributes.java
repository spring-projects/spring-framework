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

import org.springframework.ui.Model;

/**
 * A {@link Model} that can also store attributes candidate for flash storage.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface RedirectAttributes extends Model {

	/**
	 * Add the supplied attribute under the supplied name.
	 * @param attributeName the name of the model attribute (never <code>null</code>)
	 * @param attributeValue the model attribute value (can be <code>null</code>)
	 */
	RedirectAttributes addAttribute(String attributeName, Object attributeValue);

	/**
	 * Add the supplied attribute to this <code>Map</code> using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}.
	 * <p><emphasis>Note: Empty {@link java.util.Collection Collections} are not added to
	 * the model when using this method because we cannot correctly determine
	 * the true convention name. View code should check for <code>null</code> rather
	 * than for empty collections as is already done by JSTL tags.</emphasis>
	 * @param attributeValue the model attribute value (never <code>null</code>)
	 */
	RedirectAttributes addAttribute(Object attributeValue);

	/**
	 * Copy all attributes in the supplied <code>Collection</code> into this
	 * <code>Map</code>, using attribute name generation for each element.
	 * @see #addAttribute(Object)
	 */
	RedirectAttributes addAllAttributes(Collection<?> attributeValues);

	/**
	 * Copy all attributes in the supplied <code>Map</code> into this <code>Map</code>.
	 * @see #addAttribute(String, Object)
	 */
	Model addAllAttributes(Map<String, ?> attributes);

	/**
	 * Copy all attributes in the supplied <code>Map</code> into this <code>Map</code>,
	 * with existing objects of the same name taking precedence (i.e. not getting
	 * replaced).
	 */
	RedirectAttributes mergeAttributes(Map<String, ?> attributes);
	
	/**
	 * Add the given attribute as a candidate for flash storage. 
	 * @param attributeName the flash attribute name; never null
	 * @param attributeValue the flash attribute value; may be null
	 */
	RedirectAttributes addFlashAttribute(String attributeName, Object attributeValue);

	/**
	 * Add the given attribute value as a candidate for flash storage using a
	 * {@link org.springframework.core.Conventions#getVariableName generated name}. 
	 * @param attributeValue the flash attribute value; never null
	 */
	RedirectAttributes addFlashAttribute(Object attributeValue);

	/**
	 * Return the attributes candidate for flash storage.
	 */
	Map<String, ?> getFlashAttributes();

}