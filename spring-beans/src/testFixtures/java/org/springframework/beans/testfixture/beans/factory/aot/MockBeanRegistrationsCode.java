/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

import org.springframework.aot.generate.GeneratedClass;
import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationsCode;
import org.springframework.javapoet.ClassName;

/**
 * Mock {@link BeanRegistrationsCode} implementation.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class MockBeanRegistrationsCode implements BeanRegistrationsCode {

	private final GeneratedClass generatedClass;

	private final DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();


	public MockBeanRegistrationsCode(GenerationContext generationContext) {
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

}
