/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.test.tools;

import org.assertj.core.api.AbstractAssert;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertion methods for {@code DynamicFile} instances.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @param <A> the assertion type
 * @param <F> the file type
 */
public class DynamicFileAssert<A extends DynamicFileAssert<A, F>, F extends DynamicFile>
		extends AbstractAssert<A, F> {


	DynamicFileAssert(F actual, Class<?> selfType) {
		super(actual, selfType);
	}


	/**
	 * Verify that the actual content is equal to the given one.
	 * @param content the expected content of the file
	 * @return {@code this}, to facilitate method chaining
	 */
	public A hasContent(@Nullable CharSequence content) {
		assertThat(this.actual.getContent()).isEqualTo(
				content != null ? content.toString() : null);
		return this.myself;
	}

	/**
	 * Verify that the actual content contains all the given values.
	 * @param values the values to look for
	 * @return {@code this}, to facilitate method chaining
	 */
	public A contains(CharSequence... values) {
		assertThat(this.actual.getContent()).contains(values);
		return this.myself;
	}

	/**
	 * Verify that the actual content does not contain any of the given values.
	 * @param values the values to look for
	 * @return {@code this}, to facilitate method chaining
	 */
	public A doesNotContain(CharSequence... values) {
		assertThat(this.actual.getContent()).doesNotContain(values);
		return this.myself;
	}

}
