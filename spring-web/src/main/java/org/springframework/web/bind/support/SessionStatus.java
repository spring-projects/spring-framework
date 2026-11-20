/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

/**
 * Simple interface that can be injected into handler methods, allowing them to
 * signal that their session processing is complete. The handler invoker may
 * then follow up with appropriate cleanup, for example, of session attributes which
 * have been implicitly created during this handler's processing (according to
 * the
 * {@link org.springframework.web.bind.annotation.SessionAttributes @SessionAttributes}
 * annotation).
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see org.springframework.web.bind.annotation.SessionAttributes
 */
public interface SessionStatus {

	/**
	 * Mark the current handler's session processing as complete, allowing for
	 * cleanup of session attributes.
	 */
	void setComplete();

	/**
	 * Return whether the current handler's session processing has been marked
	 * as complete.
	 */
	boolean isComplete();

}
