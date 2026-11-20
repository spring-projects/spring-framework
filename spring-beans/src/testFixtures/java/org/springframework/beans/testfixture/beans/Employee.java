/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

public class Employee extends TestBean {

	private String co;

	public Employee() {
	}

	public Employee(String name) {
		super(name);
	}

	public String getCompany() {
		return co;
	}

	public void setCompany(String co) {
		this.co = co;
	}

}
