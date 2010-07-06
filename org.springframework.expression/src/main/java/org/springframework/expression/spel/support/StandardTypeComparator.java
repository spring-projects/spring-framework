/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel.support;

import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * A simple basic TypeComparator implementation. It supports comparison of numbers and types implementing Comparable.
 * 
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StandardTypeComparator implements TypeComparator {

	@SuppressWarnings("unchecked")
	public int compare(Object left, Object right) throws SpelEvaluationException {
		// If one is null, check if the other is
		if (left == null) {
			return right == null ? 0 : 1;
		} else if (right == null) {
			return -1; // left cannot be null
		}

		// Basic number comparisons
		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;
			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				double d1 = leftNumber.doubleValue();
				double d2 = rightNumber.doubleValue();
				return Double.compare(d1,d2);
			} else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				float f1 = leftNumber.floatValue();
				float f2 = rightNumber.floatValue();
				return Float.compare(f1,f2);
			} else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				Long l1 = leftNumber.longValue();
				Long l2 = rightNumber.longValue();
				return l1.compareTo(l2);
			} else {
				Integer i1 = leftNumber.intValue();
				Integer i2 = rightNumber.intValue();
				return i1.compareTo(i2);
			}
		}

		try {
			if (left instanceof Comparable) {
				return ((Comparable) left).compareTo(right);
			}
		} catch (ClassCastException cce) {
			throw new SpelEvaluationException(cce, SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
		}
		
		throw new SpelEvaluationException(SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
	}

	public boolean canCompare(Object left, Object right) {
		if (left == null || right == null) {
			return true;
		}
		if (left instanceof Number && right instanceof Number) {
			return true;
		}
		if (left instanceof Comparable) {
			return true;
		}
		return false;
	}

}
