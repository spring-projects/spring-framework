/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessagingException;

/**
 * Thrown by a {@link DestinationResolver} when it cannot resolve a destination.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DestinationResolutionException extends MessagingException {

	public DestinationResolutionException(String description) {
		super(description);
	}

	public DestinationResolutionException(@Nullable String description, @Nullable Throwable cause) {
		super(description, cause);
	}

}
