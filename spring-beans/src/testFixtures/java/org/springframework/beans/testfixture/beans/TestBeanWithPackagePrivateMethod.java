/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

@SuppressWarnings("unused")
public class TestBeanWithPackagePrivateMethod {

	private int age;

	void setAge(int age) {
		this.age = age;
	}

}
