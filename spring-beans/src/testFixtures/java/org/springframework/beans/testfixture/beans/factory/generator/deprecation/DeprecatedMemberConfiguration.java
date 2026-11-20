/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.deprecation;

/**
 * A class with deprecated members to test various use cases.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedMemberConfiguration {

	@Deprecated
	public String deprecatedString() {
		return "deprecated";
	}

	@SuppressWarnings("deprecation")
	public String deprecatedParameter(DeprecatedBean bean) {
		return bean.toString();
	}

	@SuppressWarnings("deprecation")
	public DeprecatedBean deprecatedReturnType() {
		return new DeprecatedBean();
	}

	@SuppressWarnings("deprecation")
	DeprecatedBean deprecatedReturnTypeProtected() {
		return new DeprecatedBean();
	}

}
