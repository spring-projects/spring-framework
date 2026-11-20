/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.Nullable;

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
