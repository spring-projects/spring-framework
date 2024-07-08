package org.springframework.ioc.model;


public class User {
	private String name;
	private String age;

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
	public User() {
		System.out.println("无参构造函数");
	}

}
