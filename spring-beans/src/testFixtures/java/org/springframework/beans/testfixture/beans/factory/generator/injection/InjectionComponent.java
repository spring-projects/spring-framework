/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.testfixture.beans.factory.generator.injection;

import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("unused")
public class InjectionComponent {

	private final String bean;

	private Integer counter;

	public InjectionComponent(String bean) {
		this.bean = bean;
	}

	@Autowired(required = false)
	public void setCounter(Integer counter) {
		this.counter = counter;
	}

}
