/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.aot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.MethodReference;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.Assert;

/**
 * {@link BeanRegistrationCode} implementation with code generation support.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class BeanRegistrationCodeGenerator implements BeanRegistrationCode {

	private static final Predicate<String> REJECT_ALL_ATTRIBUTES_FILTER = attribute -> false;

	private final ClassName className;

	private final GeneratedMethods generatedMethods;

	private final List<MethodReference> instancePostProcessors = new ArrayList<>();

	private final RegisteredBean registeredBean;

	private final BeanRegistrationCodeFragments codeFragments;


	BeanRegistrationCodeGenerator(ClassName className, GeneratedMethods generatedMethods,
			RegisteredBean registeredBean, BeanRegistrationCodeFragments codeFragments) {

		this.className = className;
		this.generatedMethods = generatedMethods;
		this.registeredBean = registeredBean;
		this.codeFragments = codeFragments;
	}

	@Override
	public ClassName getClassName() {
		return this.className;
	}

	@Override
	public GeneratedMethods getMethods() {
		return this.generatedMethods;
	}

	@Override
	public void addInstancePostProcessor(MethodReference methodReference) {
		Assert.notNull(methodReference, "'methodReference' must not be null");
		this.instancePostProcessors.add(methodReference);
	}

	CodeBlock generateCode(GenerationContext generationContext) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add(this.codeFragments.generateNewBeanDefinitionCode(generationContext,
				this.registeredBean.getBeanType(), this));
		code.add(this.codeFragments.generateSetBeanDefinitionPropertiesCode(
				generationContext, this, this.registeredBean.getMergedBeanDefinition(),
				REJECT_ALL_ATTRIBUTES_FILTER));
		CodeBlock instanceSupplierCode = this.codeFragments.generateInstanceSupplierCode(
				generationContext, this, this.instancePostProcessors.isEmpty());
		code.add(this.codeFragments.generateSetBeanInstanceSupplierCode(generationContext,
				this, instanceSupplierCode, this.instancePostProcessors));
		code.add(this.codeFragments.generateReturnCode(generationContext, this));
		return code.build();
	}

}
