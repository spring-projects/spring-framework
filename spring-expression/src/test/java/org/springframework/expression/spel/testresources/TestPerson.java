/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.testresources;

public class TestPerson {

	private String name;

	private TestAddress address;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TestAddress getAddress() {
		return address;
	}

	public void setAddress(TestAddress address) {
		this.address = address;
	}

}
