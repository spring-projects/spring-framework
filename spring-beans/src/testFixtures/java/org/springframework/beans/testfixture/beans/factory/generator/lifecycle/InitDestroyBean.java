/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.lifecycle;

public class InitDestroyBean {

	@Init
	public void initMethod() {
	}

	public void customInitMethod() {
	}

	@Destroy
	public void destroyMethod() {
	}

	public void customDestroyMethod() {
	}

}
