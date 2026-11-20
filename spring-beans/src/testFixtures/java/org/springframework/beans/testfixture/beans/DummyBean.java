/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

/**
 * @author Costin Leau
 */
public class DummyBean {

	private Object value;
	private String name;
	private int age;
	private TestBean spouse;

	public DummyBean(Object value) {
		this.value = value;
	}

	public DummyBean(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public DummyBean(int ageRef, String nameRef) {
		this.name = nameRef;
		this.age = ageRef;
	}

	public DummyBean(String name, TestBean spouse) {
		this.name = name;
		this.spouse = spouse;
	}

	public DummyBean(String name, Object value, int age) {
		this.name = name;
		this.value = value;
		this.age = age;
	}

	public Object getValue() {
		return value;
	}

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
