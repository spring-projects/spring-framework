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

package org.springframework.web.method.support;

import java.util.Map;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;

/**
 * Contains model and view choices made by {@link HandlerMethodReturnValueHandler}s.
 * 
 * <p>Allows return value handlers to set only the bits that are relevant to them - i.e. model, view, 
 * or none, while also taking care of merging attributes added by the {@link HandlerMethodReturnValueHandler}
 * with attributes from the implicit model.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 *
 * @param <V> Servlet or Portlet specific View type.
 */
public class ModelAndViewContainer<V> {

	private String viewName;
	
	private V view;
	
	private final ModelMap actualModel = new ExtendedModelMap();
	
	private final ModelMap implicitModel;

	public ModelAndViewContainer(ModelMap implicitModel) {
		this.implicitModel = (implicitModel != null) ? implicitModel : new ExtendedModelMap();
	}

	public ModelMap getModel() {
		return new ExtendedModelMap().addAllAttributes(actualModel).mergeAttributes(implicitModel);
	}

	public String getViewName() {
		return this.viewName;
	}
	
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public V getView() {
		return this.view;
	}
	
	public void setView(V view) {
		this.view = view;
	}

	public void addModelAttributes(Model attributes) {
		actualModel.addAllAttributes(attributes.asMap());
	}
	
	public void addModelAttributes(Map<String, Object> attributes) {
		actualModel.addAllAttributes(attributes);
	}

	public void addModelAttribute(String name, Object value) {
		actualModel.addAttribute(name, value);
	}

}
