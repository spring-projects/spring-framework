/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NamedThreadLocal;
import org.springframework.messaging.Message;

/**
 * Holder class to expose SiMP attributes associated with a session (for example, WebSocket)
 * in the form of a thread-bound {@link SimpAttributes} object.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class SimpAttributesContextHolder {

	private static final ThreadLocal<SimpAttributes> attributesHolder =
			new NamedThreadLocal<>("SiMP session attributes");


	/**
	 * Reset the SimpAttributes for the current thread.
	 */
	public static void resetAttributes() {
		attributesHolder.remove();
	}

	/**
	 * Bind the given SimpAttributes to the current thread.
	 * @param attributes the RequestAttributes to expose
	 */
	public static void setAttributes(@Nullable SimpAttributes attributes) {
		if (attributes != null) {
			attributesHolder.set(attributes);
		}
		else {
			resetAttributes();
		}
	}

	/**
	 * Extract the SiMP session attributes from the given message, wrap them in
	 * a {@link SimpAttributes} instance and bind it to the current thread.
	 * @param message the message to extract session attributes from
	 */
	public static void setAttributesFromMessage(Message<?> message) {
		setAttributes(SimpAttributes.fromMessage(message));
	}

	/**
	 * Return the SimpAttributes currently bound to the thread.
	 * @return the attributes or {@code null} if not bound
	 */
	public static @Nullable SimpAttributes getAttributes() {
		return attributesHolder.get();
	}

	/**
	 * Return the SimpAttributes currently bound to the thread or raise an
	 * {@link java.lang.IllegalStateException} if none are bound.
	 * @return the attributes, never {@code null}
	 * @throws java.lang.IllegalStateException if attributes are not bound
	 */
	public static SimpAttributes currentAttributes() throws IllegalStateException {
		SimpAttributes attributes = getAttributes();
		if (attributes == null) {
			throw new IllegalStateException("No thread-bound SimpAttributes found. " +
					"Your code is probably not processing a client message and executing in " +
					"message-handling methods invoked by the SimpAnnotationMethodMessageHandler?");
		}
		return attributes;
	}

}
