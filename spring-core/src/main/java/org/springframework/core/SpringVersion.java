/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import org.jspecify.annotations.Nullable;

/**
 * Class that exposes the Spring version. Fetches the
 * "Implementation-Version" manifest attribute from the jar file.
 *
 * <p>Note that some ClassLoaders do not expose the package metadata,
 * hence this class might not be able to determine the Spring version
 * in all environments. Consider using a reflection-based check instead &mdash;
 * for example, checking for the presence of a specific Spring method that you
 * intend to call.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public final class SpringVersion {

	private SpringVersion() {
	}


	/**
	 * Return the "major.minor.patch" version string of the present Spring codebase,
	 * or {@code null} if it cannot be determined.
	 * @see Package#getImplementationVersion()
	 */
	public static @Nullable String getVersion() {
		Package pkg = SpringVersion.class.getPackage();
		String version = (pkg != null ? pkg.getImplementationVersion() : null);
		if (version != null) {
			int idx = version.indexOf('.');  // after major
			if (idx != -1) {
				idx = version.indexOf('.', idx + 1);  // after minor
				if (idx != -1) {
					idx = version.indexOf('.', idx + 1);  // after patch
					if (idx != -1) {
						// Ignore anything beyond "major.minor.patch"
						version = version.substring(0, idx);
					}
				}
			}
		}
		return version;
	}

}
