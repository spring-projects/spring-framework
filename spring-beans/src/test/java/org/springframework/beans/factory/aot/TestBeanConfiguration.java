/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.springframework.beans.testfixture.beans.TestBean;

/**
 * Test {@code @Configuration} style class to create {@link TestBean}.
 *
 * @author Phillip Webb
 */
public class TestBeanConfiguration {

	public TestBean testBean() {
		return new TestBean();
	}

}
