/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.validation;

import java.beans.PropertyEditor;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * Abstract base class for {@link BindingResult} implementations that work with
 * Spring's {@link org.springframework.beans.PropertyAccessor} mechanism.
 * Pre-implements field access through delegation to the corresponding
 * PropertyAccessor methods.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getPropertyAccessor()
 * @see org.springframework.beans.PropertyAccessor
 * @see org.springframework.beans.ConfigurablePropertyAccessor
 */
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {

	/**
	 * Create a new AbstractPropertyBindingResult instance.
	 * @param objectName the name of the target object
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractPropertyBindingResult(String objectName) {
		super(objectName);
	}


	/**
	 * Returns the underlying PropertyAccessor.
	 * @see #getPropertyAccessor()
	 */
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return getPropertyAccessor();
	}

	/**
	 * Returns the canonical property name.
	 * @see org.springframework.beans.PropertyAccessorUtils#canonicalPropertyName
	 */
	protected String canonicalFieldName(String field) {
		return PropertyAccessorUtils.canonicalPropertyName(field);
	}

	/**
	 * Determines the field type from the property type.
	 * @see #getPropertyAccessor()
	 */
	public Class getFieldType(String field) {
		return getPropertyAccessor().getPropertyType(fixedField(field));
	}

	/**
	 * Fetches the field value from the PropertyAccessor.
	 * @see #getPropertyAccessor()
	 */
	protected Object getActualFieldValue(String field) {
		return getPropertyAccessor().getPropertyValue(field);
	}

	/**
	 * Formats the field value based on registered PropertyEditors.
	 * @see #getCustomEditor
	 */
	protected Object formatFieldValue(String field, Object value) {
		PropertyEditor customEditor = getCustomEditor(field);
		if (customEditor != null) {
			customEditor.setValue(value);
			String textValue = customEditor.getAsText();
			// If the PropertyEditor returned null, there is no appropriate
			// text representation for this value: only use it if non-null.
			if (textValue != null) {
				return textValue;
			}
		}
		return value;
	}

	/**
	 * Retrieve the custom PropertyEditor for the given field, if any.
	 * @param field the field name
	 * @return the custom PropertyEditor, or <code>null</code>
	 */
	protected PropertyEditor getCustomEditor(String field) {
		String fixedField = fixedField(field);
		Class targetType = getPropertyAccessor().getPropertyType(fixedField);
		PropertyEditor editor = getPropertyAccessor().findCustomEditor(targetType, fixedField);
		if (editor == null) {
			editor = BeanUtils.findEditorByConvention(targetType);
		}
		return editor;
	}


	/**
	 * Provide the PropertyAccessor to work with, according to the
	 * concrete strategy of access.
	 * <p>Note that a PropertyAccessor used by a BindingResult should
	 * always have its "extractOldValueForEditor" flag set to "true"
	 * by default, since this is typically possible without side effects
	 * for model objects that serve as data binding target.
	 * @see ConfigurablePropertyAccessor#setExtractOldValueForEditor
	 */
	public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
