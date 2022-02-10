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

package org.springframework.aot.generator;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.javapoet.support.MultiStatement;

/**
 * A code contribution that gathers the code, the {@linkplain RuntimeHints
 * runtime hints}, and the {@linkplain ProtectedElement protected elements}
 * that are necessary to execute it.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface CodeContribution {

	/**
	 * Return the {@linkplain MultiStatement statements} that can be used to
	 * append code.
	 * @return the statements instance to use to contribute code
	 */
	MultiStatement statements();

	/**
	 * Return the {@linkplain RuntimeHints hints} to use to register
	 * potential optimizations for contributed code.
	 * @return the runtime hints
	 */
	RuntimeHints runtimeHints();

	/**
	 * Return the {@linkplain ProtectedAccess protected access} to use to
	 * analyze any privileged access, if necessary.
	 * @return the protected access
	 */
	ProtectedAccess protectedAccess();

}
