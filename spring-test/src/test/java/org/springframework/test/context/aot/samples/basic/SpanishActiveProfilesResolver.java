/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.basic;

import org.springframework.test.context.ActiveProfilesResolver;

/**
 * @author Sam Brannen
 * @since 6.0
 */
public class SpanishActiveProfilesResolver implements ActiveProfilesResolver {

	@Override
	public String[] resolve(Class<?> testClass) {
		return new String[] { "spanish" };
	}

}
