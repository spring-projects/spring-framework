/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.interceptor;

import org.springframework.beans.testfixture.beans.TestBean;

/**
 * Test for CGLIB proxying that implements no interfaces
 * and has one dependency.
 *
 * @author Rod Johnson
 */
class ImplementsNoInterfaces {

	private TestBean testBean;

	public void setDependency(TestBean testBean) {
		this.testBean = testBean;
	}

	public String getName() {
		return testBean.getName();
	}

	public void setName(String name) {
		testBean.setName(name);
	}

}
