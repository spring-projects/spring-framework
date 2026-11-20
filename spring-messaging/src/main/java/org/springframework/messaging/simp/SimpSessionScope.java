/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * A {@link Scope} implementation exposing the attributes of a SiMP session
 * (for example, WebSocket session).
 *
 * <p>Relies on a thread-bound {@link SimpAttributes} instance exported by
 * {@link org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class SimpSessionScope implements Scope {

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		SimpAttributes simpAttributes = SimpAttributesContextHolder.currentAttributes();
		Object scopedObject = simpAttributes.getAttribute(name);
		if (scopedObject != null) {
			return scopedObject;
		}
		synchronized (simpAttributes.getSessionMutex()) {
			scopedObject = simpAttributes.getAttribute(name);
			if (scopedObject == null) {
				scopedObject = objectFactory.getObject();
				simpAttributes.setAttribute(name, scopedObject);
			}
			return scopedObject;
		}
	}

	@Override
	public @Nullable Object remove(String name) {
		SimpAttributes simpAttributes = SimpAttributesContextHolder.currentAttributes();
		synchronized (simpAttributes.getSessionMutex()) {
			Object value = simpAttributes.getAttribute(name);
			if (value != null) {
				simpAttributes.removeAttribute(name);
				return value;
			}
			else {
				return null;
			}
		}
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		SimpAttributesContextHolder.currentAttributes().registerDestructionCallback(name, callback);
	}

	@Override
	public String getConversationId() {
		return SimpAttributesContextHolder.currentAttributes().getSessionId();
	}

}
