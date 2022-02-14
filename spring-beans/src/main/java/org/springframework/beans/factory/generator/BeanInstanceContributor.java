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

package org.springframework.beans.factory.generator;

import org.springframework.aot.generator.CodeContribution;

/**
 * Strategy interface to be implemented by components that participates in a
 * bean instance setup so that the generated code provides an equivalent
 * setup.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@FunctionalInterface
public interface BeanInstanceContributor {

	/**
	 * A {@link BeanInstanceContributor} that does not contribute anything
	 * to the {@link CodeContribution}.
	 */
	BeanInstanceContributor NO_OP = contribution -> { };

	/**
	 * Contribute to the specified {@link CodeContribution}.
	 * <p>Implementation of this interface can assume the following variables
	 * to be accessible:
	 * <ul>
	 * <li>{@code beanFactory}: the general {@code DefaultListableBeanFactory}</li>
	 * <li>{@code instanceContext}: the {@code BeanInstanceContext} callback</li>
	 * <li>{@code bean}: the variable that refers to the bean instance</li>
	 * </ul>
	 * @param contribution the {@link CodeContribution} to use
	 */
	void contribute(CodeContribution contribution);

}
