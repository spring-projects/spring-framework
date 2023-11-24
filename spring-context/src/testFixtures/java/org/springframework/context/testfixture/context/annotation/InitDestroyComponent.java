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

package org.springframework.context.testfixture.context.annotation;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class InitDestroyComponent {

	public final List<String> events = new ArrayList<>();

	@PostConstruct
	public void init() {
		this.events.add("init");
	}

	public void customInit() {
		this.events.add("customInit");
	}

	@PreDestroy
	public void destroy() {
		this.events.add("destroy");
	}

	public void customDestroy() {
		this.events.add("customDestroy");
	}

}
