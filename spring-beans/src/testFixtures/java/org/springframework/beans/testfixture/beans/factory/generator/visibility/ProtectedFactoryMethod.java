/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.visibility;

public class ProtectedFactoryMethod {

	String testBean(Integer number) {
		return "test-" + number;
	}

}
