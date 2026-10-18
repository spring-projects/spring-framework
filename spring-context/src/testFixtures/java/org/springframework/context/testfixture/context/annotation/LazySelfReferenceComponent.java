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

import org.springframework.context.annotation.Lazy;

/**
 * Component that references itself using a lazy-resolution proxy.
 *
 * @author Pankraz76
 */
public class LazySelfReferenceComponent {

	private final LazySelfReferenceComponent self;

	public LazySelfReferenceComponent(@Lazy LazySelfReferenceComponent self) {
		this.self = self;
	}

	public LazySelfReferenceComponent getSelf() {
		return this.self;
	}

	public String greet() {
		return this.self.greeting();
	}

	public String greeting() {
		return "Hello";
	}

}
