/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.testresources;

public class Person {

	private String privateName;

	private int age;

	Company company;


	public Person(int age) {
		this.age = age;
	}

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

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Company getCompany() {
		return company;
	}

	public void getInvalid() {
		// no-op
	}

}
