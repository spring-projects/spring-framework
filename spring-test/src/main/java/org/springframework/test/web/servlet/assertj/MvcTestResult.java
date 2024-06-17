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

import org.assertj.core.api.AssertProvider;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Provides the result of an executed request using {@link MockMvcTester} that
 * is meant to be used with {@link org.assertj.core.api.Assertions#assertThat(AssertProvider)
 * assertThat}.
 *
 * <p>Can be in one of two distinct states:
 * <ol>
 * <li>The request processed successfully, even if it failed with an exception
 * that has been resolved. The {@linkplain #getMvcResult() result} is available,
 * and {@link #getUnresolvedException()} will return {@code null}.</li>
 * <li>The request failed unexpectedly. {@link #getUnresolvedException()}
 * provides more information about the error, and any attempt to access the
 * {@linkplain #getMvcResult() result} will fail with an exception.</li>
 * </ol>
 *
 * <p>If the request was asynchronous, it is fully resolved at this point and
 * regular assertions can be applied without having to wait for the completion
 * of the response.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 * @see MockMvcTester
 */
public interface MvcTestResult extends AssertProvider<MvcTestResultAssert> {

	/**
	 * Return the {@linkplain MvcResult result} of the processing. If
	 * the processing has failed with an unresolved exception, the
	 * result is not available, see {@link #getUnresolvedException()}.
	 * @return the {@link MvcResult}
	 * @throws IllegalStateException if the processing has failed with
	 * an unresolved exception
	 */
	MvcResult getMvcResult();

	/**
	 * Return the performed {@linkplain  MockHttpServletRequest request}.
	 */
	default MockHttpServletRequest getRequest() {
		return getMvcResult().getRequest();
	}

	/**
	 * Return the resulting {@linkplain  MockHttpServletResponse response}.
	 */
	default MockHttpServletResponse getResponse() {
		return getMvcResult().getResponse();
	}

	/**
	 * Return the exception that was thrown unexpectedly while processing the
	 * request, if any.
	 */
	@Nullable
	Exception getUnresolvedException();

}
