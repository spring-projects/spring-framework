/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx;

import java.io.IOException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class JmxTestBean implements IJmxTestBean {

	private String name;

	private String nickName;

	private int age;

	private boolean isSuperman;


	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public int getAge() {
		return age;
	}

	@Override
	public long myOperation() {
		return 1L;
	}

	@Override
	public void setName(String name) throws Exception {
		if ("Juergen".equals(name)) {
			throw new IllegalArgumentException("Juergen");
		}
		if ("Juergen Class".equals(name)) {
			throw new ClassNotFoundException("Juergen");
		}
		if ("Juergen IO".equals(name)) {
			throw new IOException("Juergen");
		}
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getNickName() {
		return this.nickName;
	}

	public void setSuperman(boolean superman) {
		this.isSuperman = superman;
	}

	public boolean isSuperman() {
		return isSuperman;
	}

	@Override
	public int add(int x, int y) {
		return x + y;
	}

	@Override
	public void dontExposeMe() {
		throw new RuntimeException();
	}

	protected void someProtectedMethod() {
	}

	@SuppressWarnings("unused")
	private void somePrivateMethod() {
	}

	protected void getSomething() {
	}

	@SuppressWarnings("unused")
	private void getSomethingElse() {
	}

}
