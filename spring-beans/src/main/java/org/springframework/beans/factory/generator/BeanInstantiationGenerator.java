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

import java.lang.reflect.Executable;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.RuntimeHints;

/**
 * Generate code that instantiate a particular bean.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface BeanInstantiationGenerator {

	/**
	 * Return the {@link Executable} that is used to create the bean instance
	 * for further metadata processing.
	 * @return the executable that is used to create the bean instance
	 */
	Executable getInstanceCreator();

	/**
	 * Return the necessary code to instantiate a bean.
	 * @param runtimeHints the runtime hints instance to use
	 * @return a code contribution that provides an initialized bean instance
	 */
	CodeContribution generateBeanInstantiation(RuntimeHints runtimeHints);

}

