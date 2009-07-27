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
import org.springframework.model.binder.BindingResult;
import org.springframework.model.binder.support.AbstractBinder;
import org.springframework.model.binder.support.AlertBindingResult;
import org.springframework.model.binder.support.FieldBinder;
import org.springframework.model.binder.support.FieldNotEditableResult;
import org.springframework.model.binder.support.FieldNotFoundResult;
import org.springframework.model.ui.BindingStatus;
import org.springframework.model.ui.FieldModel;
import org.springframework.model.ui.FieldNotFoundException;
import org.springframework.model.ui.PresentationModel;

/**
 * Binds field values to PresentationModel objects.
 * @author Keith Donald
 * @since 3.0
 * @see #setMessageSource(MessageSource)
 * @see #setRequiredFields(String[])
 * @see #setCommitDirtyValue(boolean)
 * @see #bind(Map, PresentationModel)
 */
public class PresentationModelBinder extends AbstractBinder<PresentationModel> {

	private boolean commitDirtyValue;
	
	/**
	 * Configures if this PresentationModelBinder should eagerly commit the dirty value after a successful field binding.
	 * Default is false.
	 */
	public void setCommitDirtyValue(boolean commitDirtyValue) {
		this.commitDirtyValue = commitDirtyValue;
	}

	// subclass hooks

	@Override
	protected FieldBinder createFieldBinder(PresentationModel model) {
		return new FieldModelBinder(model);
	}

	// internal helpers
	
	private class FieldModelBinder implements FieldBinder {
		
		private PresentationModel presentationModel;

		public FieldModelBinder(PresentationModel presentationModel) {
			this.presentationModel = presentationModel;
		}

		public BindingResult bind(String fieldName, Object value) {
			FieldModel field;
			try {
				field = presentationModel.getFieldModel(fieldName);
			} catch (FieldNotFoundException e) {
				return new FieldNotFoundResult(fieldName, value,  getMessageSource());
			}
			if (!field.isEditable()) {
				return new FieldNotEditableResult(fieldName, value, getMessageSource());
			} else {
				field.applySubmittedValue(value);
				if (field.getBindingStatus() == BindingStatus.DIRTY && commitDirtyValue) {
					field.commit();
				}
				return new AlertBindingResult(fieldName, value, field.getStatusAlert());
			}
		}
	}

}
