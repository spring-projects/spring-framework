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

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class AutowiredComponent {

	private Environment environment;

	private Integer counter;

	@Autowired
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	@Autowired
	public void setCounter(Integer counter) {
		this.counter = counter;
	}

	public Integer getCounter() {
		return this.counter;
	}

	public Integer getCounter(Integer ignored) {
		return this.counter;
	}

}
