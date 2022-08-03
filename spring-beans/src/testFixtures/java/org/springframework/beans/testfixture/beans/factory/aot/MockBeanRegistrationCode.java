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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.javapoet.ClassName;

/**
 * Mock {@link BeanRegistrationCode} implementation.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class MockBeanRegistrationCode implements BeanRegistrationCode {

	private final GeneratedClass generatedClass;

	private final List<MethodReference> instancePostProcessors = new ArrayList<>();

	private final DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();


	public MockBeanRegistrationCode(GenerationContext generationContext) {
		this.generatedClass = generationContext.getGeneratedClasses().addForFeature("TestCode", this.typeBuilder);
	}


	public DeferredTypeBuilder getTypeBuilder() {
		return this.typeBuilder;
	}

	@Override
	public ClassName getClassName() {
		return this.generatedClass.getName();
	}

	@Override
	public GeneratedMethods getMethods() {
		return this.generatedClass.getMethods();
	}

	@Override
	public void addInstancePostProcessor(MethodReference methodReference) {
		this.instancePostProcessors.add(methodReference);
	}

	public List<MethodReference> getInstancePostProcessors() {
		return Collections.unmodifiableList(this.instancePostProcessors);
	}

}
