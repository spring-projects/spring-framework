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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.ExpressionState;

/**
 * The Sort Processor will sort an input collection, comparing elements using the comparator accessible in the
 * evaluation context.
 * 
 * @author Andy Clement
 * 
 */
@SuppressWarnings("unchecked")
public class SortProcessor implements DataProcessor {

	public Object process(Collection<?> input, Object[] arguments, ExpressionState state) throws EvaluationException {
		List<Object> sortedCollection = new ArrayList<Object>();
		sortedCollection.addAll(input);
		LocalComparator comparator = new LocalComparator(state.getTypeComparator());
		Collections.sort(sortedCollection, comparator);
		if (comparator.exceptionOccurred != null)
			throw comparator.exceptionOccurred;
		return sortedCollection;
	}

	private static class LocalComparator implements java.util.Comparator {
		TypeComparator comparator;
		EvaluationException exceptionOccurred;

		public LocalComparator(TypeComparator comparator) {
			this.comparator = comparator;
		}

		public int compare(Object o1, Object o2) {
			try {
				return comparator.compare(o1, o2);
			} catch (EvaluationException e) {
				exceptionOccurred = e;
				return 0;
			}
		}

	}

}
