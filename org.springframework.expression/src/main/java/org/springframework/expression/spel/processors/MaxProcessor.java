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
 * The Max Processor returns the maximum element in the input collection, the maximum is determined using the comparator
 * accessible in the evaluation context.
 * 
 * @author Andy Clement
 * 
 */
public class MaxProcessor implements DataProcessor {

	public Object process(Collection<?> input, Object[] arguments, ExpressionState state) throws EvaluationException {
		Object max = null;
		TypeComparator comparator = state.getTypeComparator();
		for (Object element : input) {
			if (max == null) {
				max = element;
			} else {
				if (comparator.compare(element, max) > 0)
					max = element;
			}
		}
		return max;
	}

}
