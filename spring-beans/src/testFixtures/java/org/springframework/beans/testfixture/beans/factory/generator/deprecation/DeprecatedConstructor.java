/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.deprecation;

import org.springframework.core.env.Environment;

/**
 * A sample bean whose factory method (constructor) is deprecated.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedConstructor {

	@Deprecated
	public DeprecatedConstructor(Environment environment) {
	}

}
