package com.gaok;

import java.io.Serializable;

/**
 * @author : gaokang
 * @date : 2020/5/2
 */
public class User implements Serializable {
	private static final long serialVersionUID = -4361831167768085422L;

	private String name;

	private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
