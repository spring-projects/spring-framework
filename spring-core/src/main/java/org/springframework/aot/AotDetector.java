/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot;

import org.springframework.core.NativeDetector;
import org.springframework.core.NativeDetector.Context;
import org.springframework.core.SpringProperties;

/**
 * Utility for determining if AOT-processed optimizations must be used rather
 * than the regular runtime. Strictly for internal use within the framework.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 6.0
 */
public abstract class AotDetector {

	/**
	 * System property that indicates the application should run with AOT
	 * generated artifacts. If such optimizations are not available, it is
	 * recommended to throw an exception rather than fall back to the regular
	 * runtime behavior.
	 */
	public static final String AOT_ENABLED = "spring.aot.enabled";

	private static final boolean inNativeImage = NativeDetector.inNativeImage(Context.RUN, Context.BUILD);


	/**
	 * Determine whether AOT optimizations must be considered at runtime. This
	 * is mandatory in a native image but can be triggered on the JVM using
	 * the {@value #AOT_ENABLED} Spring property.
	 * @return whether AOT optimizations must be considered
	 */
	public static boolean useGeneratedArtifacts() {
		return (inNativeImage || SpringProperties.getFlag(AOT_ENABLED));
	}

}
