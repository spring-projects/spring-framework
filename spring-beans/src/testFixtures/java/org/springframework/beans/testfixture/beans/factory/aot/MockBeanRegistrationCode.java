/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
