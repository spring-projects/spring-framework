/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.model.ui.support;

import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResult;
import org.springframework.model.binder.support.AbstractBinder;
import org.springframework.model.binder.support.AlertBindingResult;
import org.springframework.model.binder.support.FieldNotEditableResult;
import org.springframework.model.binder.support.FieldNotFoundResult;
import org.springframework.model.ui.BindingStatus;
import org.springframework.model.ui.FieldModel;
import org.springframework.model.ui.FieldNotFoundException;
import org.springframework.model.ui.PresentationModel;
import org.springframework.util.Assert;

/**
 * A generic {@link Binder binder} suitable for use in most environments.
 * @author Keith Donald
 * @since 3.0
 * @see #setMessageSource(MessageSource)
 * @see #setRequiredFields(String[])
 * @see #bind(Map)
 */
public class PresentationModelBinder extends AbstractBinder {

	private PresentationModel presentationModel;

	public PresentationModelBinder(PresentationModel presentationModel) {
		Assert.notNull(presentationModel, "The PresentationModel is required");
		this.presentationModel = presentationModel;
	}

	// subclassing hooks

	/**
	 * Get the model for the field.
	 * @param fieldName
	 * @return the field model
	 * @throws NoSuchFieldException if no such field exists
	 */
	protected FieldModel getFieldModel(String fieldName) {
		return presentationModel.getFieldModel(fieldName);
	}

	protected BindingResult bindField(String name, Object value) {
		FieldModel field;
		try {
			field = getFieldModel(name);
		} catch (FieldNotFoundException e) {
			return new FieldNotFoundResult(name, value, getMessageSource());
		}
		if (!field.isEditable()) {
			return new FieldNotEditableResult(name, value, getMessageSource());
		} else {
			field.applySubmittedValue(value);
			if (field.getBindingStatus() == BindingStatus.DIRTY) {
				field.commit();
			}
			return new AlertBindingResult(name, value, field.getStatusAlert());
		}
	}
}