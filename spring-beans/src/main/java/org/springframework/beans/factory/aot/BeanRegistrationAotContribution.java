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

package org.springframework.beans.factory.aot;

import java.util.function.UnaryOperator;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.util.Assert;

/**
 * AOT contribution from a {@link BeanRegistrationAotProcessor} used to register
 * a single bean definition.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see BeanRegistrationAotProcessor
 */
@FunctionalInterface
public interface BeanRegistrationAotContribution {

	/**
	 * Customize the {@link BeanRegistrationCodeFragments} that will be used to
	 * generate the bean registration code. Custom code fragments can be used if
	 * default code generation isn't suitable.
	 * @param generationContext the generation context
	 * @param codeFragments the existing code fragments
	 * @return the code fragments to use, may be the original instance or a
	 * wrapper
	 */
	default BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
			GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {

		return codeFragments;
	}

	/**
	 * Apply this contribution to the given {@link BeanRegistrationCode}.
	 * @param generationContext the generation context
	 * @param beanRegistrationCode the generated registration
	 */
	void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode);

	/**
	 * Create a {@link BeanRegistrationAotContribution} that customizes
	 * the {@link BeanRegistrationCodeFragments}. Typically used in
	 * conjunction with an extension of {@link BeanRegistrationCodeFragmentsDecorator}
	 * that overrides a specific callback.
	 * @param defaultCodeFragments the default code fragments
	 * @return a new {@link BeanRegistrationAotContribution} instance
	 * @see BeanRegistrationCodeFragmentsDecorator
	 */
	static BeanRegistrationAotContribution withCustomCodeFragments(
			UnaryOperator<BeanRegistrationCodeFragments> defaultCodeFragments) {

		Assert.notNull(defaultCodeFragments, "'defaultCodeFragments' must not be null");

		return new BeanRegistrationAotContribution() {
			@Override
			public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(
					GenerationContext generationContext, BeanRegistrationCodeFragments codeFragments) {
				return defaultCodeFragments.apply(codeFragments);
			}
			@Override
			public void applyTo(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode) {
			}
		};
	}

}
