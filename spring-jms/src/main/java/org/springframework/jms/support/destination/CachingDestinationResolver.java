/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.destination;

/**
 * Extension of the DestinationResolver interface,
 * exposing methods for clearing the cache.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface CachingDestinationResolver extends DestinationResolver {

	/**
	 * Remove the destination with the given name from the cache
	 * (if cached by this resolver in the first place).
	 * <p>To be called if access to the specified destination failed,
	 * assuming that the JMS Destination object might have become invalid.
	 * @param destinationName the name of the destination
	 */
	void removeFromCache(String destinationName);

	/**
	 * Clear the entire destination cache.
	 * <p>To be called in case of general JMS provider failure.
	 */
	void clearCache();

}
