package com.atlwj.demo.ioc.annotation.resource.entity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class Person implements Serializable {

	private static final long serialVersionUID = -1593440478830232276L;
	@Value("lucy")
	private String username;
	@Value("250")
	private Integer age;

	public Person() {
	}

	public Person(String username, Integer age) {
		this.username = username;
		this.age = age;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "Person{" +
				"username='" + username + '\'' +
				", age=" + age +
				'}';
	}
}
