package com.atlwj.ioc;


import java.lang.reflect.Field;
import java.util.Optional;

class UserDemo {
	private String name;
	private String password;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
public class Demo{
	public static void main(String[] args) {
		UserDemo userDemo = new UserDemo();
		userDemo.setName(null);
		userDemo.setPassword(null);

	}
}