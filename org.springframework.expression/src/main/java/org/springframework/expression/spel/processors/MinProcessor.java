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

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.ExpressionState;

/**
 * The Min Processor returns the minimum element in the input collection, the minimum is determined using the comparator
 * accessible in the evaluation context.
 * 
 * @author Andy Clement
 * 
 */
public class MinProcessor implements DataProcessor {

	public Object process(Collection<?> input, Object[] arguments, ExpressionState state) throws EvaluationException {
		Object minimum = null;
		TypeComparator elementComparator = state.getTypeComparator();
		for (Object element : input) {
			if (minimum == null) {
				minimum = element;
			} else {
				if (elementComparator.compare(element, minimum) < 0)
					minimum = element;
			}
		}
		return minimum;
	}

}
