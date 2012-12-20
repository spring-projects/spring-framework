package org.springframework.expression.spel.testresources;

///CLOVER:OFF
public class Person {
	private String privateName;
	Company company;

	public Person(String name) {
		this.privateName = name;
	}

	public Person(String name, Company company) {
		this.privateName = name;
		this.company = company;
	}

	public String getName() {
		return privateName;
	}

	public void setName(String n) {
		this.privateName = n;
	}

	public Company getCompany() {
		return company;
	}
}
