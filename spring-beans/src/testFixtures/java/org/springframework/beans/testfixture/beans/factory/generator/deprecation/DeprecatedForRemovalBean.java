/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.deprecation;

/**
 * A sample bean that's fully deprecated for removal.
 *
 * @author Stephane Nicoll
 */
@Deprecated(forRemoval = true)
public class DeprecatedForRemovalBean {

	// This isn't flag deprecated on purpose
	public static class Nested {}
}
