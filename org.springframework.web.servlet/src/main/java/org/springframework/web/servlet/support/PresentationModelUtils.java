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
package org.springframework.web.servlet.support;

import javax.servlet.ServletRequest;

import org.springframework.model.ui.PresentationModelFactory;
import org.springframework.model.ui.support.DefaultPresentationModelFactory;
import org.springframework.web.context.request.WebRequest;

/**
 * Utilities for working with the <code>model.ui</code> PresentationModel system.
 * @author Keith Donald
 */
public final class PresentationModelUtils {

	private static final String PRESENTATION_MODEL_FACTORY_ATTRIBUTE = "presentationModelFactory";

	private PresentationModelUtils() {		
	}
	
	/**
	 * Get the PresentationModelFactory for the current web request.
	 * Will create a new one and cache it as a request attribute if one does not exist.
	 * @param request the web request
	 * @return the presentation model factory
	 */
	public static PresentationModelFactory getPresentationModelFactory(WebRequest request) {
		PresentationModelFactory factory = (PresentationModelFactory) request.getAttribute(PRESENTATION_MODEL_FACTORY_ATTRIBUTE, WebRequest.SCOPE_REQUEST);
		if (factory == null) {
			factory = new DefaultPresentationModelFactory();
			request.setAttribute(PRESENTATION_MODEL_FACTORY_ATTRIBUTE, factory, WebRequest.SCOPE_REQUEST);
		}
		return factory;
	}
	
	/**
	 * Get the PresentationModelFactory for the current servlet request.
	 * Will create a new one and cache it as a request attribute if one does not exist.
	 * @param request the servlet
	 * @return the presentation model factory
	 */
	public static PresentationModelFactory getPresentationModelFactory(ServletRequest request) {
		PresentationModelFactory factory = (PresentationModelFactory) request.getAttribute(PRESENTATION_MODEL_FACTORY_ATTRIBUTE);
		if (factory == null) {
			factory = new DefaultPresentationModelFactory();
			request.setAttribute(PRESENTATION_MODEL_FACTORY_ATTRIBUTE, factory);
		}
		return factory;
	}
	
}
