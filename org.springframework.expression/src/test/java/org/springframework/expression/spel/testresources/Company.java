/**
 * 
 */
package org.springframework.expression.spel.testresources;

public class Company {
	String address;
	
	public Company(String string) {
		this.address = string;
	}

	public String getAddress() {
		return address;
	}
}