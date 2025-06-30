/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.support;

/**
 * @author Alef Arendsen
 */
public class Assembler implements TestIF {

	@SuppressWarnings("unused")
	private Service service;

	private Logic l;

	@SuppressWarnings("unused")
	private String name;

	public void setService(Service service) {
		this.service = service;
	}

	public void setLogic(Logic l) {
		this.l = l;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	public void test() {
	}

	public void output() {
		l.output();
	}

}
