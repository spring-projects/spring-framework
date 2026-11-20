/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.advice;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Abstract superclass for counting advice, etc.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
@SuppressWarnings("serial")
public class MethodCounter implements Serializable {

	/** Method name --> count, does not understand overloading */
	private Map<String, Integer> map = new HashMap<>();

	private int allCount;

	protected void count(Method m) {
		count(m.getName());
	}

	protected void count(String methodName) {
		map.merge(methodName, 1, (n, m) -> n + 1);
		++allCount;
	}

	public int getCalls(String methodName) {
		return map.getOrDefault(methodName, 0);
	}

	public int getCalls() {
		return allCount;
	}

	/**
	 * A bit simplistic: just wants the same class.
	 * Doesn't worry about counts.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (other != null && getClass() == other.getClass());
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
