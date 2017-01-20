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

package org.springframework.test.web.client;

import org.springframework.util.Assert;

/**
 * A simple type representing a range for an expected count.
 *
 * <p>Examples:
 * <pre>
 * import static org.springframework.test.web.client.ExpectedCount.*
 *
 * once()
 * manyTimes()
 * times(5)
 * min(2)
 * max(4)
 * between(2, 4)
 * never()
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class ExpectedCount {

	private final int minCount;

	private final int maxCount;


	/**
	 * Private constructor.
	 * See static factory methods in this class.
	 */
	private ExpectedCount(int minCount, int maxCount) {
		Assert.isTrue(minCount >= 0, "minCount >= 0 is required");
		Assert.isTrue(maxCount >= minCount, "maxCount >= minCount is required");
		this.minCount = minCount;
		this.maxCount = maxCount;
	}


	/**
	 * Return the {@code min} boundary of the expected count range.
	 */
	public int getMinCount() {
		return this.minCount;
	}

	/**
	 * Return the {@code max} boundary of the expected count range.
	 */
	public int getMaxCount() {
		return this.maxCount;
	}


	/**
	 * Exactly once.
	 */
	public static ExpectedCount once() {
		return new ExpectedCount(1, 1);
	}

	/**
	 * Many times (range of 1..Integer.MAX_VALUE).
	 */
	public static ExpectedCount manyTimes() {
		return new ExpectedCount(1, Integer.MAX_VALUE);
	}

	/**
	 * Exactly N times.
	 */
	public static ExpectedCount times(int count) {
		Assert.isTrue(count >= 1, "'count' must be >= 1");
		return new ExpectedCount(count, count);
	}

	/**
	 * At least {@code min} number of times.
	 */
	public static ExpectedCount min(int min) {
		Assert.isTrue(min >= 1, "'min' must be >= 1");
		return new ExpectedCount(min, Integer.MAX_VALUE);
	}

	/**
	 * At most {@code max} number of times.
	 */
	public static ExpectedCount max(int max) {
		Assert.isTrue(max >= 1, "'max' must be >= 1");
		return new ExpectedCount(1, max);
	}

	/**
	 * No calls expected at all, i.e. min=0 and max=0.
	 * @since 4.3.6
	 */
	public static ExpectedCount never() {
		return new ExpectedCount(0, 0);
	}

	/**
	 * Between {@code min} and {@code max} number of times.
	 */
	public static ExpectedCount between(int min, int max) {
		return new ExpectedCount(min, max);
	}

}
