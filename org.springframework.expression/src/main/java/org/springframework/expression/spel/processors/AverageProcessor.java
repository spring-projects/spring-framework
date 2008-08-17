/*
 * Copyright 2004-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.expression.spel.processors;

import java.util.Collection;

import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

// TODO does it return an element consistent with input? (an int if input is ints, even though the average may be
// X.Y?) yes, for now
/**
 * The AverageProcessor operates upon an input collection and computes the average value of the elements within it. It
 * will currently only operate upon Numbers and its return value type is an Integer if the input values were integers,
 * otherwise it is a double.
 */
public class AverageProcessor implements DataProcessor {

	public Object process(Collection<?> input, Object[] arguments, ExpressionState state) throws SpelException {
		// TypeUtilities typeUtilities = state.getTypeUtilities();
		boolean allIntegerObjects = true;
		int total = 0;
		int numberOfElements = 0;
		for (Object element : input) {
			if (element != null) {
				if (element instanceof Number) {
					allIntegerObjects = allIntegerObjects
							&& (element.getClass() == Integer.class || element.getClass() == Integer.TYPE);
					total = total + ((Number) element).intValue();
					numberOfElements++;
				} else {
					throw new SpelException(SpelMessages.TYPE_NOT_SUPPORTED_BY_PROCESSOR, "average", element.getClass());
				}
			}
		}
		int result = total / numberOfElements;
		// if (allIntegerObjects) {
		// return new Integer(((Number) result).intValue());
		// } else {
		return result;
		// }
	}
}
