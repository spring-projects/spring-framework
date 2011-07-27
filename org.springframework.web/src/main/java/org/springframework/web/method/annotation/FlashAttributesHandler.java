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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.FlashAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Manages flash attributes declared via @{@link FlashAttributes}. 
 * 
 * TODO ...
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class FlashAttributesHandler {
	
	public static final String FLASH_ATTRIBUTES_SESSION_KEY = FlashAttributesHandler.class.getName() + ".attributes";

	private final Set<String> attributeNames = new HashSet<String>();

	private final Set<Class<?>> attributeTypes = new HashSet<Class<?>>();

	/**
	 * TODO ...
	 */
	public FlashAttributesHandler(Class<?> handlerType) {
		FlashAttributes annotation = AnnotationUtils.findAnnotation(handlerType, FlashAttributes.class);
		if (annotation != null) {
			this.attributeNames.addAll(Arrays.asList(annotation.value())); 
			this.attributeTypes.addAll(Arrays.<Class<?>>asList(annotation.types()));
		}		
	}

	/**
	 * Whether the controller represented by this handler has declared flash 
	 * attribute names or types via @{@link FlashAttributes}. 
	 */
	public boolean hasFlashAttributes() {
		return ((this.attributeNames.size() > 0) || (this.attributeTypes.size() > 0)); 
	}

	/**
	 * TODO ...
	 */
	public boolean isFlashAttribute(String attributeName, Class<?> attributeType) {
		return (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType));
	}
	
	/**
	 * TODO ...
	 */
	public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
		Map<String, Object> filtered = filterAttributes(attributes);
		if (!filtered.isEmpty()) {
			request.setAttribute(FLASH_ATTRIBUTES_SESSION_KEY, filtered, WebRequest.SCOPE_SESSION);
		}
	}

	private Map<String, Object> filterAttributes(Map<String, ?> attributes) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (String name : attributes.keySet()) {
			Object value = attributes.get(name);
			Class<?> type = (value != null) ? value.getClass() : null;
			if (isFlashAttribute(name, type)) {
				result.put(name, value);
			}
		}
		return result;
	}

	/**
	 * TODO ...
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> retrieveAttributes(WebRequest request) {
		return (Map<String, Object>) request.getAttribute(FLASH_ATTRIBUTES_SESSION_KEY, WebRequest.SCOPE_SESSION);
	}

	/**
	 * TODO ...
	 */
	public void cleanupAttributes(WebRequest request) {
		request.removeAttribute(FLASH_ATTRIBUTES_SESSION_KEY, WebRequest.SCOPE_SESSION);
	}

}