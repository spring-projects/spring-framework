/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.testdata;

/**
 * @author Andy Clement
 * @since 4.1.2
 */
public class PersonInOtherPackage {

	private int age;

	public PersonInOtherPackage(int age) {
		this.age = age;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

}
