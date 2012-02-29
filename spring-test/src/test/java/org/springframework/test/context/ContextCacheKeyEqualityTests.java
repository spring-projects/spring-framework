/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.*;

/**
 * Verify the equality of {@link ContextCacheKey} class instances.
 *
 * {@link ContextCacheKey} is used as a key of {@link ContextCache}, so equality of {@link ContextCacheKey} is
 * important.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
@RunWith(Parameterized.class)
public class ContextCacheKeyEqualityTests {

	private boolean expectEqual;

	private ContextCacheKey leftKey;

	private ContextCacheKey rightKey;

	public ContextCacheKeyEqualityTests(boolean expectEqual, ContextCacheKey leftKey, ContextCacheKey rightKey) {
		this.expectEqual = expectEqual;
		this.leftKey = leftKey;
		this.rightKey = rightKey;
	}

	@Parameters
	public static Collection<Object[]> getData() {
		MergedContextConfiguration foo =
				new MergedContextConfiguration(ContextCacheKeyEqualityTests.class, new String[]{}, new Class<?>[]{},
						new String[]{}, null);
		MergedContextConfiguration bar =
				new MergedContextConfiguration(ContextCacheKeyEqualityTests.class, new String[]{}, new Class<?>[]{},
						new String[]{}, null);
		MergedContextConfiguration baz =
				new MergedContextConfiguration(ContextCacheKeyEqualityTests.class, new String[]{"different"}, new Class<?>[]{},
						new String[]{}, null);

		assertThat(foo, equalTo(bar));
		assertThat(bar, equalTo(foo));
		assertThat(baz, not(equalTo(foo)));
		assertThat(baz, not(equalTo(bar)));

		// foo == bar != baz
		return Arrays.asList(new Object[][]{

				// expect to be equal, left cache key, right cache key
				{true, new ContextCacheKey(foo, null), new ContextCacheKey(foo, null)},
				{true, new ContextCacheKey(foo, null), new ContextCacheKey(bar, null)},
				{true, new ContextCacheKey(foo, bar), new ContextCacheKey(foo, bar)},
				{true, new ContextCacheKey(foo, bar), new ContextCacheKey(bar, foo)},
				{true, new ContextCacheKey(null, null), new ContextCacheKey(null, null)},

				{false, new ContextCacheKey(foo, null), new ContextCacheKey(baz, null)},
				{false, new ContextCacheKey(foo, bar), new ContextCacheKey(foo, baz)},});
	}

	@Test
	public void testEquality() {
		if (expectEqual) {
			assertThat(leftKey, equalTo(rightKey));
		}
		else {
			assertThat(leftKey, not(equalTo(rightKey)));
		}
	}
}
