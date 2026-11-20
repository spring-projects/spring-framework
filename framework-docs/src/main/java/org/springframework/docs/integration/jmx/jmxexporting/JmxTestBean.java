/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jmx.jmxexporting;

// tag::snippet[]
public class JmxTestBean implements IJmxTestBean {

	private String name;
	private int age;

	@Override
	public int getAge() {
		return age;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int add(int x, int y) {
		return x + y;
	}

	@Override
	public void dontExposeMe() {
		throw new RuntimeException();
	}
}
// end::snippet[]