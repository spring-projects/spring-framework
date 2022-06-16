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
 * A default {@link CodeContribution} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class DefaultCodeContribution implements CodeContribution {

	private final MultiStatement statements;

	private final RuntimeHints runtimeHints;

	private final ProtectedAccess protectedAccess;


	protected DefaultCodeContribution(MultiStatement statements, RuntimeHints runtimeHints,
			ProtectedAccess protectedAccess) {

		this.statements = statements;
		this.runtimeHints = runtimeHints;
		this.protectedAccess = protectedAccess;
	}

	/**
	 * Create an instance with the {@link RuntimeHints} instance to use.
	 * @param runtimeHints the runtime hints instance to use
	 */
	public DefaultCodeContribution(RuntimeHints runtimeHints) {
		this(new MultiStatement(), runtimeHints, new ProtectedAccess());
	}

	@Override
	public MultiStatement statements() {
		return this.statements;
	}

	@Override
	public RuntimeHints runtimeHints() {
		return this.runtimeHints;
	}

	@Override
	public ProtectedAccess protectedAccess() {
		return this.protectedAccess;
	}

}
