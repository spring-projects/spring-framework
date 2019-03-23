/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Comparator capable of sorting exceptions based on their depth from the thrown exception type.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 3.0.3
 */
public class ExceptionDepthComparator implements Comparator<Class<? extends Throwable>> {

	private final Class<? extends Throwable> targetException;


	/**
	 * Create a new ExceptionDepthComparator for the given exception.
	 * @param exception the target exception to compare to when sorting by depth
	 */
	public ExceptionDepthComparator(Throwable exception) {
		Assert.notNull(exception, "Target exception must not be null");
		this.targetException = exception.getClass();
	}

	/**
	 * Create a new ExceptionDepthComparator for the given exception type.
	 * @param exceptionType the target exception type to compare to when sorting by depth
	 */
	public ExceptionDepthComparator(Class<? extends Throwable> exceptionType) {
		Assert.notNull(exceptionType, "Target exception type must not be null");
		this.targetException = exceptionType;
	}


	@Override
	public int compare(Class<? extends Throwable> o1, Class<? extends Throwable> o2) {
		int depth1 = getDepth(o1, this.targetException, 0);
		int depth2 = getDepth(o2, this.targetException, 0);
		return (depth1 - depth2);
	}

	private int getDepth(Class<?> declaredException, Class<?> exceptionToMatch, int depth) {
		if (exceptionToMatch.equals(declaredException)) {
			// Found it!
			return depth;
		}
		// If we've gone as far as we can go and haven't found it...
		if (exceptionToMatch == Throwable.class) {
			return Integer.MAX_VALUE;
		}
		return getDepth(declaredException, exceptionToMatch.getSuperclass(), depth + 1);
	}


	/**
	 * Obtain the closest match from the given exception types for the given target exception.
	 * @param exceptionTypes the collection of exception types
	 * @param targetException the target exception to find a match for
	 * @return the closest matching exception type from the given collection
	 */
	public static Class<? extends Throwable> findClosestMatch(
			Collection<Class<? extends Throwable>> exceptionTypes, Throwable targetException) {

		Assert.notEmpty(exceptionTypes, "Exception types must not be empty");
		if (exceptionTypes.size() == 1) {
			return exceptionTypes.iterator().next();
		}
		List<Class<? extends Throwable>> handledExceptions =
				new ArrayList<Class<? extends Throwable>>(exceptionTypes);
		Collections.sort(handledExceptions, new ExceptionDepthComparator(targetException));
		return handledExceptions.get(0);
	}

}
