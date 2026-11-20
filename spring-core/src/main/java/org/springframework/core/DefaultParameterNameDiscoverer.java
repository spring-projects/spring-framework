/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of the {@link ParameterNameDiscoverer} strategy interface,
 * delegating to Java's standard reflection mechanism.
 *
 * <p>If a Kotlin reflection implementation is present,
 * {@link KotlinReflectionParameterNameDiscoverer} is added first in the list and
 * used for Kotlin classes and interfaces.
 *
 * <p>Further discoverers may be added through {@link #addDiscoverer(ParameterNameDiscoverer)}.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 4.0
 * @see StandardReflectionParameterNameDiscoverer
 * @see KotlinReflectionParameterNameDiscoverer
 */
public class DefaultParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {

	private static final boolean KOTLIN_REFLECT_PRESENT = KotlinDetector.isKotlinReflectPresent();

	private static volatile @Nullable DefaultParameterNameDiscoverer sharedInstance;


	public DefaultParameterNameDiscoverer() {
		if (KOTLIN_REFLECT_PRESENT) {
			addDiscoverer(new KotlinReflectionParameterNameDiscoverer());
		}

		// Recommended approach on Java: compilation with -parameters.
		addDiscoverer(new StandardReflectionParameterNameDiscoverer());
	}


	/**
	 * Return a shared default {@code ParameterNameDiscoverer} instance,
	 * lazily building it once needed.
	 * @return the shared {@code ParameterNameDiscoverer} instance
	 * @since 7.0.3
	 */
	public static ParameterNameDiscoverer getSharedInstance() {
		DefaultParameterNameDiscoverer pnd = sharedInstance;
		if (pnd == null) {
			synchronized (DefaultParameterNameDiscoverer.class) {
				pnd = sharedInstance;
				if (pnd == null) {
					pnd = new DefaultParameterNameDiscoverer();
					sharedInstance = pnd;
				}
			}
		}
		return pnd;
	}

}
