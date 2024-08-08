/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.WebSession;

/**
 * Package-private class to assist {@link ModelInitializer} with managing model
 * attributes in the {@link WebSession} based on model attribute names and types
 * declared va {@link SessionAttributes @SessionAttributes}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<>();

	private final Set<Class<?>> attributeTypes = new HashSet<>();

	private final Set<String> knownAttributeNames = ConcurrentHashMap.newKeySet(4);


	/**
	 * Create a new session attributes handler. Session attribute names and types
	 * are extracted from the {@code @SessionAttributes} annotation, if present,
	 * on the given type.
	 * @param handlerType the controller type
	 */
	public SessionAttributesHandler(Class<?> handlerType) {
		SessionAttributes ann = AnnotatedElementUtils.findMergedAnnotation(handlerType, SessionAttributes.class);
		if (ann != null) {
			Collections.addAll(this.attributeNames, ann.names());
			Collections.addAll(this.attributeTypes, ann.types());
		}
		this.knownAttributeNames.addAll(this.attributeNames);
	}


	/**
	 * Whether the controller represented by this instance has declared any
	 * session attributes through an {@link SessionAttributes} annotation.
	 */
	public boolean hasSessionAttributes() {
		return (!this.attributeNames.isEmpty() || !this.attributeTypes.isEmpty());
	}

	/**
	 * Whether the attribute name or type match the names and types specified
	 * via {@code @SessionAttributes} on the underlying controller.
	 * <p>Attributes successfully resolved through this method are "remembered"
	 * and subsequently used in {@link #retrieveAttributes(WebSession)}
	 * and also {@link #cleanupAttributes(WebSession)}.
	 * @param attributeName the attribute name to check
	 * @param attributeType the type for the attribute
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		Assert.notNull(attributeName, "Attribute name must not be null");
		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			this.knownAttributeNames.add(attributeName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Retrieve "known" attributes from the session, i.e. attributes listed
	 * by name in {@code @SessionAttributes} or attributes previously stored
	 * in the model that matched by type.
	 * @param session the current session
	 * @return a map with handler session attributes, possibly empty
	 */
	public Map<String, Object> retrieveAttributes(WebSession session) {
		Map<String, Object> attributes = new HashMap<>();
		this.knownAttributeNames.forEach(name -> {
			Object value = session.getAttribute(name);
			if (value != null) {
				attributes.put(name, value);
			}
		});
		return attributes;
	}

	/**
	 * Store a subset of the given attributes in the session. Attributes not
	 * declared as session attributes via {@code @SessionAttributes} are ignored.
	 * @param session the current session
	 * @param attributes candidate attributes for session storage
	 */
	public void storeAttributes(WebSession session, Map<String, ?> attributes) {
		attributes.forEach((name, value) -> {
			if (value != null && isHandlerSessionAttribute(name, value.getClass())) {
				session.getAttributes().put(name, value);
			}
		});
	}

	/**
	 * Remove "known" attributes from the session, i.e. attributes listed
	 * by name in {@code @SessionAttributes} or attributes previously stored
	 * in the model that matched by type.
	 * @param session the current session
	 */
	public void cleanupAttributes(WebSession session) {
		this.knownAttributeNames.forEach(name -> session.getAttributes().remove(name));
	}

}
