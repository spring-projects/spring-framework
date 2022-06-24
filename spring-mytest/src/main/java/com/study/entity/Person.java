package com.study.entity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhutongtong
 * @date 2022/6/22 16:23
 */
@Configuration
public class Person {
	private Integer id = 2;
	private String name = "name";

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
