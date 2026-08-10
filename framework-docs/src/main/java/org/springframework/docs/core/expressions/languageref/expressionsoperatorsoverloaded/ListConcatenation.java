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

package org.springframework.docs.core.expressions.languageref.expressionsoperatorsoverloaded;

import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;

import java.util.ArrayList;
import java.util.List;

public class ListConcatenation implements OperatorOverloader {

	@Override
	public boolean overridesOperation(Operation operation, Object left, Object right) {
		return (operation == Operation.ADD && left instanceof List && right instanceof List);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object operate(Operation operation, Object left, Object right) {
		if (operation == Operation.ADD &&
				left instanceof List list1 && right instanceof List list2) {

			List result = new ArrayList(list1);
			result.addAll(list2);
			return result;
		}
		throw new UnsupportedOperationException(
				"No overload for operation %s and operands [%s] and [%s]"
						.formatted(operation, left, right));
	}

}
