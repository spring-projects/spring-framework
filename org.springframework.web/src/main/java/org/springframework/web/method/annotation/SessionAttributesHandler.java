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

package org.springframework.web.method.annotation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * Provides operations for managing handler-specific session attributes as defined by the 
 * {@link SessionAttributes} type-level annotation performing all operations through an 
 * instance of a {@link SessionAttributeStore}.
 * 
 * <p>A typical scenario involves a handler adding attributes to the {@link Model} during
 * a request. At the end of the request, model attributes that match to session attribute
 * names defined through an {@link SessionAttributes} annotation are automatically 
 * "promoted" to the session. Handler session attributes are then removed when 
 * {@link SessionStatus#setComplete()} is called by a handler. 
 * 
 * <p>Therefore "session attributes" for this class means only attributes that have been 
 * previously confirmed by calls to {@link #isHandlerSessionAttribute(String, Class)}. 
 * Attribute names that have never been resolved that way will be filtered out from 
 * operations of this class. That means initially the actual set of resolved session 
 * attribute names is empty and it grows gradually as attributes are added to 
 * the {@link Model} and then considered for being added to the session. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<String>();

	@SuppressWarnings("rawtypes")
	private final Set<Class> attributeTypes = new HashSet<Class>();

	private final Set<String> resolvedAttributeNames = Collections.synchronizedSet(new HashSet<String>(4));

	private final SessionAttributeStore attributeStore;

	/**
	 * Creates a {@link SessionAttributesHandler} instance for the specified handlerType.
	 * <p>Inspects the given handler type for the presence of a {@link SessionAttributes} annotation and 
	 * stores that information for use in subsequent calls to {@link #isHandlerSessionAttribute(String, Class)}. 
	 * If the handler type does not contain such an annotation, 
	 * {@link #isHandlerSessionAttribute(String, Class)} always returns {@code false} and all other operations 
	 * on handler session attributes have no effect on the backend session. 
	 * <p>Use {@link #hasSessionAttributes()} to check if the handler type has defined any session attribute names 
	 * of interest through a {@link SessionAttributes} annotation.
	 * @param handlerType the handler type to inspect for a {@link SessionAttributes} annotation
	 * @param attributeStore the {@link SessionAttributeStore} to delegate to for the actual backend session access
	 */
	public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore attributeStore) {
		Assert.notNull(attributeStore, "SessionAttributeStore may not be null.");
		this.attributeStore = attributeStore;
		
		SessionAttributes annotation = AnnotationUtils.findAnnotation(handlerType, SessionAttributes.class);
		if (annotation != null) {
			this.attributeNames.addAll(Arrays.asList(annotation.value())); 
			this.attributeTypes.addAll(Arrays.asList(annotation.types()));
		}		
	}

	/**
	 * Returns true if the handler type has specified any session attribute names of interest through a 
	 * {@link SessionAttributes} annotation. 
	 */
	public boolean hasSessionAttributes() {
		return ((this.attributeNames.size() > 0) || (this.attributeTypes.size() > 0)); 
	}
	
	/**
	 * Indicate whether or not an attribute is a handler session attribute of interest as defined 
	 * in a {@link SessionAttributes} annotation. Attributes names successfully resolved through 
	 * this method are remembered and in other operations.
	 * @param attributeName the attribute name to check, must not be null
	 * @param attributeType the type for the attribute, not required but should be provided when
	 * available as session attributes of interest can be matched by type
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		Assert.notNull(attributeName, "Attribute name must not be null");
		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			this.resolvedAttributeNames.add(attributeName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Retrieves the specified attribute through the underlying {@link SessionAttributeStore}.
	 * Although not required use of this method implies a prior call to 
	 * {@link #isHandlerSessionAttribute(String, Class)} has been made to see if the attribute 
	 * name is a handler-specific session attribute of interest.
	 * @param request the request for the session operation
	 * @param attributeName the name of the attribute
	 * @return the attribute value or {@code null} if none
	 */
	public Object retrieveAttribute(WebRequest request, String attributeName) {
		return this.attributeStore.retrieveAttribute(request, attributeName);
	}
	
	/**
	 * Retrieve attributes for the underlying handler type from the backend session. 
	 * <p>Only attributes that have previously been successfully resolved via calls to
	 * {@link #isHandlerSessionAttribute(String, Class)} are considered.
	 * @param request the current request
	 * @return a map with attributes or an empty map
	 */
	public Map<String, ?> retrieveHandlerSessionAttributes(WebRequest request) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (String name : this.resolvedAttributeNames) {
			Object value = this.attributeStore.retrieveAttribute(request, name);
			if (value != null) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	/**
	 * Clean up attributes for the underlying handler type from the backend session.
	 * <p>Only attributes that have previously been successfully resolved via calls to
	 * {@link #isHandlerSessionAttribute(String, Class)} are removed.
	 * @param request the current request
	 */
	public void cleanupHandlerSessionAttributes(WebRequest request) {
		for (String attributeName : this.resolvedAttributeNames) {
			this.attributeStore.cleanupAttribute(request, attributeName);
		}
	}

	/**
	 * Store attributes in the backend session.
	 * <p>Only attributes that have previously been successfully resolved via calls to
	 * {@link #isHandlerSessionAttribute(String, Class)} are stored. All other attributes 
	 * from the input map are ignored.
	 * @param request the current request
	 * @param attributes the attribute pairs to consider for storing
	 */
	public void storeHandlerSessionAttributes(WebRequest request, Map<String, Object> attributes) {
		for (String name : attributes.keySet()) {
			Object value = attributes.get(name);
			Class<?> attrType = (value != null) ? value.getClass() : null;
			
			if (isHandlerSessionAttribute(name, attrType)) {
				this.attributeStore.storeAttribute(request, name, value);
			}
		}
	}

}