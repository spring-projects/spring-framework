/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.deprecation;

/**
 * A class with deprecated members for removal to test various use cases.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedForRemovalMemberConfiguration {

	@Deprecated(forRemoval = true)
	public String deprecatedString() {
		return "deprecated";
	}

	@SuppressWarnings("removal")
	public String deprecatedParameter(DeprecatedForRemovalBean bean) {
		return bean.toString();
	}

	@SuppressWarnings("removal")
	public DeprecatedForRemovalBean deprecatedReturnType() {
		return new DeprecatedForRemovalBean();
	}

	@SuppressWarnings("removal")
	DeprecatedForRemovalBean deprecatedReturnTypeProtected() {
		return new DeprecatedForRemovalBean();
	}

}
