package com.bat.spring.entity;

/**
 * @program: ESAT
 * @author: zhq
 * @description:
 * @create: 2021/9/2 16:13
 **/
public class User {
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

		@Override
		public String toString() {
			return "User{" +
					"name='" + name + '\'' +
					", age=" + age +
					'}';
		}
	}
