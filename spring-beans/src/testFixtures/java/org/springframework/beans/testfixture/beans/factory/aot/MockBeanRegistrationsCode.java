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

package org.springframework.beans.testfixture.beans.factory.aot;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.beans.factory.aot.BeanRegistrationsCode;
import org.springframework.javapoet.ClassName;

/**
 * Mock {@link BeanRegistrationsCode} implementation.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class MockBeanRegistrationsCode implements BeanRegistrationsCode {

	private final ClassName className;

	private final GeneratedMethods generatedMethods = new GeneratedMethods();

	public MockBeanRegistrationsCode(ClassName className) {
		this.className = className;
	}

	public MockBeanRegistrationsCode() {
		this(ClassName.get("com.example", "Test"));
	}

	@Override
	public ClassName getClassName() {
		return this.className;
	}

	@Override
	public GeneratedMethods getMethodGenerator() {
		return this.generatedMethods;
	}

}
