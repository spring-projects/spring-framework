package com.lxcecho.reflect;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class Car {
	// 属性

	private String name;

	private int age;

	private String color;

	/**
	 * 无参数构造
	 */
	public Car() {
	}

	/**
	 * 有参数构造
	 * @param name
	 * @param age
	 * @param color
	 */
	public Car(String name, int age, String color) {
		this.name = name;
		this.age = age;
		this.color = color;
	}

	/**
	 * 普通方法
	 */
	private void run() {
		System.out.println("私有方法-run.....");
	}

	// get和set方法

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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Override
	public String toString() {
		return "Car{" +
				"name='" + name + '\'' +
				", age=" + age +
				", color='" + color + '\'' +
				'}';
	}
}
