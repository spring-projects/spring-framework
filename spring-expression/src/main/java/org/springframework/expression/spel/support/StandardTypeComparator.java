/*
 * Copyright 2002-2017 the original author or authors.
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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.NumberUtils;

/**
 * A simple basic {@link TypeComparator} implementation.
 * It supports comparison of Numbers and types implementing Comparable.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class StandardTypeComparator implements TypeComparator {

	@Override
	public boolean canCompare(@Nullable Object left, @Nullable Object right) {
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

	@Override
	@SuppressWarnings("unchecked")
	public int compare(@Nullable Object left, @Nullable Object right) throws SpelEvaluationException {
		// If one is null, check if the other is
		if (left == null) {
			return (right == null ? 0 : -1);
		}
		else if (right == null) {
			return 1;  // left cannot be null at this point
		}

		// Basic number comparisons
		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return leftBigDecimal.compareTo(rightBigDecimal);
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return Float.compare(leftNumber.floatValue(), rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return leftBigInteger.compareTo(rightBigInteger);
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				// Don't call Long.compare here - only available on JDK 1.7+
				return compare(leftNumber.longValue(), rightNumber.longValue());
			}
			else if (leftNumber instanceof Integer || rightNumber instanceof Integer) {
				// Don't call Integer.compare here - only available on JDK 1.7+
				return compare(leftNumber.intValue(), rightNumber.intValue());
			}
			else if (leftNumber instanceof Short || rightNumber instanceof Short) {
				// Don't call Short.compare here - only available on JDK 1.7+
				return compare(leftNumber.shortValue(), rightNumber.shortValue());
			}
			else if (leftNumber instanceof Byte || rightNumber instanceof Byte) {
				// Don't call Short.compare here - only available on JDK 1.7+
				return compare(leftNumber.byteValue(), rightNumber.byteValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double multiplication
				return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
			}
		}

		try {
			if (left instanceof Comparable) {
				return ((Comparable<Object>) left).compareTo(right);
			}
		}
		catch (ClassCastException ex) {
			throw new SpelEvaluationException(ex, SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
		}

		throw new SpelEvaluationException(SpelMessage.NOT_COMPARABLE, left.getClass(), right.getClass());
	}


	private static int compare(long x, long y) {
		return (x < y ? -1 : (x > y ? 1 : 0));
	}

	private static int compare(int x, int y) {
		return (x < y ? -1 : (x > y ? 1 : 0));
	}

	private static int compare(short x, short y) {
		return x - y;
	}

	private static int compare(byte x, byte y) {
		return x - y;
	}

}
