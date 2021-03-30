package com.cn.mayf.service;

import org.springframework.stereotype.Component;

/**
 * @Author mayf
 * @Date 2021/3/9 12:53
 */
@Component
public class BeanService {
	String name;
	int age;

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

	@Override
	public String toString() {
		return "name:"+name+"\nage:"+age;
	}
}
