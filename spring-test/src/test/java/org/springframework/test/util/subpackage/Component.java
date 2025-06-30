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

package org.springframework.test.util.subpackage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple POJO representing a <em>component</em>; intended for use in
 * unit tests.
 *
 * @author Sam Brannen
 * @since 3.1
 */
public class Component {

	private Integer number;
	private String text;


	public Integer getNumber() {
		return this.number;
	}

	public String getText() {
		return this.text;
	}

	@Autowired
	protected void configure(Integer number, String text) {
		this.number = number;
		this.text = text;
	}

	@PostConstruct
	protected void init() {
		Assert.state(number != null, "number must not be null");
		Assert.state(StringUtils.hasText(text), "text must not be empty");
	}

	@PreDestroy
	protected void destroy() {
		this.number = null;
		this.text = null;
	}

	int subtract(int a, int b) {
		return a - b;
	}

	int add(int... args) {
		int sum = 0;
		for (int arg : args) {
			sum += arg;
		}
		return sum;
	}

	int multiply(Integer... args) {
		int product = 1;
		for (Integer arg : args) {
			product *= arg;
		}
		return product;
	}

}
