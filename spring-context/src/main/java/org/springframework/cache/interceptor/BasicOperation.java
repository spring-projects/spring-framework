/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.util.Set;

/**
 * The base interface that all cache operations must implement.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface BasicOperation {

	/**
	 * Return the cache name(s) associated with the operation.
	 */
	Set<String> getCacheNames();

}
