/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.assertj;

import org.jspecify.annotations.Nullable;

import org.springframework.test.json.JsonConverterDelegate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The default {@link MvcTestResult} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
final class DefaultMvcTestResult implements MvcTestResult {

	private final @Nullable MvcResult mvcResult;

	private final @Nullable Exception unresolvedException;

	private final @Nullable JsonConverterDelegate converterDelegate;


	DefaultMvcTestResult(@Nullable MvcResult mvcResult, @Nullable Exception unresolvedException,
			@Nullable JsonConverterDelegate converterDelegate) {

		this.mvcResult = mvcResult;
		this.unresolvedException = unresolvedException;
		this.converterDelegate = converterDelegate;
	}


	@Override
	public MvcResult getMvcResult() {
		if (this.mvcResult == null) {
			throw new IllegalStateException(
					"Request failed with unresolved exception " + this.unresolvedException);
		}
		return this.mvcResult;
	}

	@Override
	public @Nullable Exception getUnresolvedException() {
		return this.unresolvedException;
	}

	public @Nullable Exception getResolvedException() {
		return getMvcResult().getResolvedException();
	}


	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
	 * instead.
	 */
	@Override
	public MvcTestResultAssert assertThat() {
		return new MvcTestResultAssert(this, this.converterDelegate);
	}

}
