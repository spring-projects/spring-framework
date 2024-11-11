/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.assertj;

import org.springframework.lang.Nullable;
import org.springframework.test.http.HttpMessageContentConverter;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The default {@link MvcTestResult} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
final class DefaultMvcTestResult implements MvcTestResult {

	@Nullable
	private final MvcResult mvcResult;

	@Nullable
	private final Exception unresolvedException;

	@Nullable
	private final HttpMessageContentConverter contentConverter;


	DefaultMvcTestResult(@Nullable MvcResult mvcResult, @Nullable Exception unresolvedException,
			@Nullable HttpMessageContentConverter contentConverter) {

		this.mvcResult = mvcResult;
		this.unresolvedException = unresolvedException;
		this.contentConverter = contentConverter;
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
	@Nullable
	public Exception getUnresolvedException() {
		return this.unresolvedException;
	}

	@Nullable
	public Exception getResolvedException() {
		return getMvcResult().getResolvedException();
	}


	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
	 * instead.
	 */
	@Override
	public MvcTestResultAssert assertThat() {
		return new MvcTestResultAssert(this, this.contentConverter);
	}

}
