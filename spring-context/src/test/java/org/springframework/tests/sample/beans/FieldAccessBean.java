/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.tests.sample.beans;

import org.springframework.beans.testfixture.beans.TestBean;

/**
 * @author Juergen Hoeller
 * @since 07.03.2006
 */
public class FieldAccessBean {

	public String name;

	protected int age;

	private TestBean spouse;

	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}

	public TestBean getSpouse() {
		return spouse;
	}

}
