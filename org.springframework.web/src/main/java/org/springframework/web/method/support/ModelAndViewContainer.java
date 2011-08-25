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

import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;

/**
 * Record model and view related decisions made by {@link HandlerMethodArgumentResolver}s 
 * and {@link HandlerMethodReturnValueHandler}s during the course of invocation of a 
 * request-handling method.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewContainer {

	private Object view;
	
	private boolean resolveView = true;
	
	private final ModelMap model = new BindingAwareModelMap();

	private ModelMap redirectModel;

	private boolean redirectModelEnabled;

	/**
	 * Create a new instance.
	 */
	public ModelAndViewContainer() {
	}

	/**
	 * Set a view name to be resolved by the DispatcherServlet via a ViewResolver. 
	 * Will override any pre-existing view name or View.
	 */
	public void setViewName(String viewName) {
		this.view = viewName;
	}

	/**
	 * Return the view name to be resolved by the DispatcherServlet via a 
	 * ViewResolver, or {@code null} if a View object is set.
	 */
	public String getViewName() {
		return (this.view instanceof String ? (String) this.view : null);
	}
	
	/**
	 * Set a View object to be used by the DispatcherServlet. 
	 * Will override any pre-existing view name or View.
	 */
	public void setView(Object view) {
		this.view = view;
	}

	/**
	 * Return the View object, or {@code null} if we using a view name
	 * to be resolved by the DispatcherServlet via a ViewResolver.
	 */
	public Object getView() {
		return this.view;
	}

	/**
	 * Whether the view is a view reference specified via a name to be 
	 * resolved by the DispatcherServlet via a ViewResolver.
	 */
	public boolean isViewReference() {
		return (this.view instanceof String);
	}
	
	/**
	 * Whether view resolution is required or not. The default value is "true".
	 * <p>When set to "false" by a {@link HandlerMethodReturnValueHandler}, the response 
	 * is considered complete and view resolution is not be performed. 
	 * <p>When set to "false" by {@link HandlerMethodArgumentResolver}, the response is
	 * considered complete only in combination with the request mapping method 
	 * returning {@code null} or void.
	 */
	public void setResolveView(boolean resolveView) {
		this.resolveView = resolveView;
	}
	
	/**
	 * Whether view resolution is required or not.
	 */
	public boolean isResolveView() {
		return this.resolveView;
	}

	/**
	 * Return the model to use, never {@code null}.
	 */
	public ModelMap getModel() {
		if (this.redirectModelEnabled && (this.redirectModel != null)) {
			return this.redirectModel;
		}
		else {
			return this.model;
		}
	}

	/**
	 * Provide an alternative model that may be prepared for a specific redirect
	 * case. To enable use of this model, {@link #setRedirectModelEnabled()} 
	 * must also be called.
	 */
	public void setRedirectModel(ModelMap redirectModel) {
		this.redirectModel = redirectModel;
	}

	/**
	 * Signals that a redirect model provided via {@link #setRedirectModel} 
	 * may be used if it was provided.
	 */
	public void setRedirectModelEnabled() {
		this.redirectModelEnabled = true;
	}

	/**
	 * Add the supplied attribute to the underlying model.
	 * @see ModelMap#addAttribute(String, Object)
	 */
	public ModelAndViewContainer addAttribute(String name, Object value) {
		getModel().addAttribute(name, value);
		return this;
	}
	
	/**
	 * Add the supplied attribute to the underlying model.
	 * @see Model#addAttribute(Object)
	 */
	public ModelAndViewContainer addAttribute(Object value) {
		getModel().addAttribute(value);
		return this;
	}

	/**
	 * Copy all attributes to the underlying model.
	 * @see ModelMap#addAllAttributes(Map)
	 */
	public ModelAndViewContainer addAllAttributes(Map<String, ?> attributes) {
		getModel().addAllAttributes(attributes);
		return this;
	}

	/**
	 * Copy attributes in the supplied <code>Map</code> with existing objects of 
	 * the same name taking precedence (i.e. not getting replaced).
	 * @see ModelMap#mergeAttributes(Map)
	 */
	public ModelAndViewContainer mergeAttributes(Map<String, ?> attributes) {
		getModel().mergeAttributes(attributes);
		return this;
	}

	/**
	 * Whether the underlying model contains the given attribute name.
	 * @see ModelMap#containsAttribute(String)
	 */
	public boolean containsAttribute(String name) {
		return getModel().containsAttribute(name);
	}
	
}
