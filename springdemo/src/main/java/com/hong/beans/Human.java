package com.hong.beans;

/**
 * @author wanghong
 * @date 2022/8/3
 * @apiNote
 */
public class Human {
	private String name;
	private String age;

	public Human(String name, String age) {
		this.name = name;
		this.age = age;
	}

	public Human() {
	}

	@Override
	public String toString() {
		return "Human{" +
				"name='" + name + '\'' +
				", age='" + age + '\'' +
				'}';
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAge() {
		return age;
	}

	public void setAge(String age) {
		this.age = age;
	}
}
