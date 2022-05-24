package com.ysj.autowireTest;

public class C {
	private String name;

	public C() {
		System.out.println("C初始化了");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
