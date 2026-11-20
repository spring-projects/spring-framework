/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

/**
 * Strategy for resolving a String destination name to an actual destination
 * of type {@code <D>}.
 *
 * @author Mark Fisher
 * @since 4.0
 * @param <D> the destination type
 */
@FunctionalInterface
public interface DestinationResolver<D> {

	/**
	 * Resolve the given destination name.
	 * @param name the destination name to resolve
	 * @return the resolved destination (never {@code null})
	 * @throws DestinationResolutionException if the specified destination
	 * wasn't found or wasn't resolvable for any other reason
	 */
	D resolveDestination(String name) throws DestinationResolutionException;

}
