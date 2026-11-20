/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

/**
 * An extension of {@link VersionPathStrategy} that adds a method
 * to determine the actual version of a {@link Resource}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see VersionResourceResolver
*/
public interface VersionStrategy extends VersionPathStrategy {

	/**
	 * Determine the version for the given resource.
	 * @param resource the resource to check
	 * @return the version (never {@code null})
	 */
	String getResourceVersion(Resource resource);

}
