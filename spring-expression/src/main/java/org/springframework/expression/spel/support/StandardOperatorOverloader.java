/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.expression.spel.support;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;

/**
 * Standard implementation of {@link OperatorOverloader}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardOperatorOverloader implements OperatorOverloader {

	static final StandardOperatorOverloader INSTANCE = new StandardOperatorOverloader();

	@Override
	public boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException {

		return false;
	}

	@Override
	public Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException {

		throw new EvaluationException("No operation overloaded by default");
	}

}
