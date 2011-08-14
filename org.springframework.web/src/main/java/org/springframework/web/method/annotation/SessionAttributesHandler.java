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
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * Manages handler-specific session attributes declared via @{@link SessionAttributes}. 
 * Actual storage is performed through an instance of {@link SessionAttributeStore}.
 * 
 * <p>A typical scenario begins with a controller adding attributes to the 
 * {@link org.springframework.ui.Model Model}. At the end of the request, model 
 * attributes are checked to see if any of them match the names and types declared 
 * via @{@link SessionAttributes}. Matching model attributes are "promoted" to 
 * the session and remain there until the controller calls 
 * {@link SessionStatus#setComplete()} to indicate the session attributes are
 * no longer needed and can be removed. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<String>();

	private final Set<Class<?>> attributeTypes = new HashSet<Class<?>>();

	private final Set<String> resolvedAttributeNames = Collections.synchronizedSet(new HashSet<String>(4));

	private final SessionAttributeStore sessionAttributeStore;

	/**
	 * Creates a {@link SessionAttributesHandler} instance for the specified handler type 
	 * Inspects the given handler type for the presence of an @{@link SessionAttributes} 
	 * and stores that information to identify model attribute that need to be stored, 
	 * retrieved, or removed from the session.
	 * @param handlerType the handler type to inspect for a {@link SessionAttributes} annotation
	 * @param sessionAttributeStore used for session access
	 */
	public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore may not be null.");
		this.sessionAttributeStore = sessionAttributeStore;
		
		SessionAttributes annotation = AnnotationUtils.findAnnotation(handlerType, SessionAttributes.class);
		if (annotation != null) {
			this.attributeNames.addAll(Arrays.asList(annotation.value())); 
			this.attributeTypes.addAll(Arrays.<Class<?>>asList(annotation.types()));
		}		
	}

	/**
	 * Whether the controller represented by this handler has declared session 
	 * attribute names or types of interest via @{@link SessionAttributes}. 
	 */
	public boolean hasSessionAttributes() {
		return ((this.attributeNames.size() > 0) || (this.attributeTypes.size() > 0)); 
	}
	
	/**
	 * Whether the controller represented by this instance has declared a specific 
	 * attribute as a session attribute via @{@link SessionAttributes}. 
	 * 
	 * <p>Attributes successfully resolved through this method are "remembered" and
	 * used by calls to {@link #retrieveAttributes(WebRequest)} and 
	 * {@link #cleanupAttributes(WebRequest)}. In other words unless attributes 
	 * have been resolved and stored before, retrieval and cleanup have no impact.
	 * 
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
	 * Stores a subset of the given attributes in the session. Attributes not 
	 * declared as session attributes via @{@link SessionAttributes} are ignored. 
	 * @param request the current request
	 * @param attributes candidate attributes for session storage
	 */
	public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
		for (String name : attributes.keySet()) {
			Object value = attributes.get(name);
			Class<?> attrType = (value != null) ? value.getClass() : null;
			
			if (isHandlerSessionAttribute(name, attrType)) {
				this.sessionAttributeStore.storeAttribute(request, name, value);
			}
		}
	}
	
	/**
	 * Retrieves "remembered" (i.e. previously stored) session attributes 
	 * for the controller represented by this handler.
	 * @param request the current request
	 * @return a map with handler session attributes; possibly empty.
	 */
	public Map<String, Object> retrieveAttributes(WebRequest request) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		for (String name : this.resolvedAttributeNames) {
			Object value = this.sessionAttributeStore.retrieveAttribute(request, name);
			if (value != null) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	/**
	 * Cleans "remembered" (i.e. previously stored) session attributes  
	 * for the controller represented by this handler.
	 * @param request the current request
	 */
	public void cleanupAttributes(WebRequest request) {
		for (String attributeName : this.resolvedAttributeNames) {
			this.sessionAttributeStore.cleanupAttribute(request, attributeName);
		}
	}

	/**
	 * A pass-through call to the underlying {@link SessionAttributeStore}.
	 * @param request the current request
	 * @param attributeName the name of the attribute of interest
	 * @return the attribute value or {@code null}
	 */
	Object retrieveAttribute(WebRequest request, String attributeName) {
		return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
	}
	
}