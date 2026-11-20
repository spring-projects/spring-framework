/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
