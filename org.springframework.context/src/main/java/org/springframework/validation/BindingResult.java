/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Map;

import org.springframework.beans.PropertyEditorRegistry;

/**
 * General interface that represents binding results. Extends the
 * {@link Errors interface} for error registration capabilities,
 * allowing for a {@link Validator} to be applied, and adds
 * binding-specific analysis and model building.
 *
 * <p>Serves as result holder for a {@link DataBinder}, obtained via
 * the {@link DataBinder#getBindingResult()} method. BindingResult
 * implementations can also be used directly, for example to invoke
 * a {@link Validator} on it (e.g. as part of a unit test).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder
 * @see Errors
 * @see Validator
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 * @see MapBindingResult
 */
public interface BindingResult extends Errors {

	/**
	 * Prefix for the name of the BindingResult instance in a model,
	 * followed by the object name.
	 */
	String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";


	/**
	 * Return the wrapped target object, which may be a bean, an object with
	 * public fields, a Map - depending on the concrete binding strategy.
	 */
	Object getTarget();

	/**
	 * Return a model Map for the obtained state, exposing a BindingResult
	 * instance as '{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName'
	 * and the object itself as 'objectName'.
	 * <p>Note that the Map is constructed every time you're calling this method.
	 * Adding things to the map and then re-calling this method will not work.
	 * <p>The attributes in the model Map returned by this method are usually
	 * included in the {@link org.springframework.web.servlet.ModelAndView}
	 * for a form view that uses Spring's <code>bind</code> tag in a JSP,
	 * which needs access to the BindingResult instance. Spring's pre-built
	 * form controllers will do this for you when rendering a form view.
	 * When building the ModelAndView instance yourself, you need to include
	 * the attributes from the model Map returned by this method.
	 * @see #getObjectName()
	 * @see #MODEL_KEY_PREFIX
	 * @see org.springframework.web.servlet.ModelAndView
	 * @see org.springframework.web.servlet.tags.BindTag
	 * @see org.springframework.web.servlet.mvc.SimpleFormController
	 */
	Map<String, Object> getModel();

	/**
	 * Extract the raw field value for the given field.
	 * Typically used for comparison purposes.
	 * @param field the field to check
	 * @return the current value of the field in its raw form,
	 * or <code>null</code> if not known
	 */
	Object getRawFieldValue(String field);

	/**
	 * Find a custom property editor for the given type and property.
	 * @param field the path of the property (name or nested path), or
	 * <code>null</code> if looking for an editor for all properties of the given type
	 * @param valueType the type of the property (can be <code>null</code> if a property
	 * is given but should be specified in any case for consistency checking)
	 * @return the registered editor, or <code>null</code> if none
	 */
	PropertyEditor findEditor(String field, Class valueType);

	/**
	 * Return the underlying PropertyEditorRegistry.
	 * @return the PropertyEditorRegistry, or <code>null</code> if none
	 * available for this BindingResult
	 */
	PropertyEditorRegistry getPropertyEditorRegistry();

	/**
	 * Add a custom {@link ObjectError} or {@link FieldError} to the errors list.
	 * <p>Intended to be used by cooperating strategies such as {@link BindingErrorProcessor}.
	 * @see ObjectError
	 * @see FieldError
	 * @see BindingErrorProcessor
	 */
	void addError(ObjectError error);

	/**
	 * Resolve the given error code into message codes for the given field.
	 * <p>Calls the configured {@link MessageCodesResolver} with appropriate parameters.
	 * @param errorCode the error code to resolve into message codes
	 * @param field the field to resolve message codes for
	 * @return the resolved message codes
	 */
	String[] resolveMessageCodes(String errorCode, String field);

	/**
	 * Mark the specified disallowed field as suppressed.
	 * <p>The data binder invokes this for each field value that was
	 * detected to target a disallowed field.
	 * @see DataBinder#setAllowedFields
	 */
	void recordSuppressedField(String field);

	/**
	 * Return the list of fields that were suppressed during the bind process.
	 * <p>Can be used to determine whether any field values were targeting
	 * disallowed fields.
	 * @see DataBinder#setAllowedFields
	 */
	String[] getSuppressedFields();

}
