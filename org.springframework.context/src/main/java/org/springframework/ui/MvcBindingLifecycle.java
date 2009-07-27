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
package org.springframework.ui;

import java.util.Map;

import org.springframework.model.ui.PresentationModel;
import org.springframework.model.ui.PresentationModelFactory;
import org.springframework.model.ui.config.BindingLifecycle;
import org.springframework.model.ui.support.PresentationModelBinder;

/**
 * Spring MVC implementation of the Binding Lifecycle.
 * @author Keith Donald
 */
public class MvcBindingLifecycle implements BindingLifecycle<Object> {

	private Class<?> modelType;

	private PresentationModelFactory presentationModelFactory;

	private ModelMap modelMap;

	private Map<String, ? extends Object> fieldValues;

	private Object model;

	public MvcBindingLifecycle(Class<?> modelType, PresentationModelFactory presentationModelFactory,
			ModelMap modelMap, Map<String, ? extends Object> fieldValues) {
		this.presentationModelFactory = presentationModelFactory;
		this.modelMap = modelMap;
		this.fieldValues = fieldValues;
	}

	public void setModel(Object model) {
		this.model = model;
	}

	public void execute() {
		boolean bindEagerCommit = false;
		if (model == null) {
			initModel();
			bindEagerCommit = true;
		}
		modelMap.addAttribute(model);
		PresentationModel presentationModel = presentationModelFactory.getPresentationModel(model);
		PresentationModelBinder binder = new PresentationModelBinder();
		binder.setCommitDirtyValue(bindEagerCommit);
		binder.bind(fieldValues, presentationModel);
		presentationModel.validate();
		if (!presentationModel.hasErrors() && !bindEagerCommit) {
			presentationModel.commit();
		}
	}

	public boolean hasErrors() {
		return false;
	}

	public Object getModel() {
		return model;
	}

	private void initModel() {
		try {
			model = modelType.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalStateException("Model of type [" + modelType.getName()
					+ "] does not have a default constructor");
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Model of type [" + modelType.getName()
					+ "] does not have a public default constructor");
		}
	}
}
