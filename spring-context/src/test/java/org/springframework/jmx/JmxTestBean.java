/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
