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

import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.support.BindingAwareModelMap;

/**
 * Provides access to the model and a place to record model and view related decisions made by
 * {@link HandlerMethodArgumentResolver}s or a {@link HandlerMethodReturnValueHandler}.
 * 
 * <p>In addition to storing model attributes and a view, the {@link ModelAndViewContainer} also provides
 * a {@link #setResolveView(boolean)} flag, which can be used to request or bypass a view resolution phase.
 * This is most commonly used from {@link HandlerMethodReturnValueHandler}s but in some cases may also be 
 * used from {@link HandlerMethodArgumentResolver}s such as when a handler method accepts an argument 
 * providing access to the response. When that is the case, if the handler method returns {@code null},
 * view resolution is skipped.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewContainer {

	private String viewName;
	
	private Object view;
	
	private final ModelMap model;
	
	private boolean resolveView = true;

	/**
	 * Create a {@link ModelAndViewContainer} instance with a {@link BindingAwareModelMap}.
	 */
	public ModelAndViewContainer() {
		this.model = new BindingAwareModelMap();
	}

	/**
	 * Create a {@link ModelAndViewContainer} instance with the given {@link ModelMap} instance.
	 * @param model the model to use
	 */
	public ModelAndViewContainer(ModelMap model) {
		Assert.notNull(model);
		this.model = model;
	}

	/**
	 * @return the model for the current request
	 */
	public ModelMap getModel() {
		return model;
	}

	/**
	 * @return the view name to use for view resolution, or {@code null}
	 */
	public String getViewName() {
		return this.viewName;
	}
	
	/**
	 * @param viewName the name of the view to use for view resolution
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * @return the view instance to use for view resolution  
	 */
	public Object getView() {
		return this.view;
	}
	
	/**
	 * @param view the view instance to use for view resolution
	 */
	public void setView(Object view) {
		this.view = view;
	}
	
	/**
	 * @return whether the view resolution is requested ({@code true}), or should be bypassed ({@code false})
	 */
	public boolean isResolveView() {
		return resolveView;
	}

	/**
	 * @param resolveView whether the view resolution is requested ({@code true}), or should be bypassed ({@code false}) 
	 */
	public void setResolveView(boolean resolveView) {
		this.resolveView = resolveView;
	}

	/**
	 * Whether model contains an attribute of the given name.
	 * @param name the name of the model attribute
	 * @return {@code true} if the model contains an attribute by that name and the name is not an empty string 
	 */
	public boolean containsAttribute(String name) {
		return (StringUtils.hasText(name) && model.containsAttribute(name));
	}
	
	/**
	 * @param name the attribute to get from the model
	 * @return the attribute or {@code null}
	 */
	public Object getAttribute(String name) {
		return model.get(name);
	}
	
	/**
	 * Add the supplied attribute under the given name.
	 * @param name the name of the model attribute (never null)
	 * @param value the model attribute value (can be null)
	 */
	public void addAttribute(String name, Object value) {
		model.addAttribute(name, value);
	}
	
	/**
	 * Copy all attributes in the supplied Map into the model
	 */
	public void addAllAttributes(Map<String, ?> attributes) {
		model.addAllAttributes(attributes);
	}

	/**
	 * Add the given attribute if the model does not already contain such an attribute.
	 * @param name the name of the attribute to check and add
	 * @param value the value of the attribute
	 */
	public void mergeAttribute(String name, Object value) {
		if (!containsAttribute(name)) {
			model.addAttribute(name, value);
		}
	}
	
}