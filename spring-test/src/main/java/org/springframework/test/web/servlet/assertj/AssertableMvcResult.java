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
import org.springframework.test.web.servlet.MvcResult;

/**
 * A {@link MvcResult} that additionally supports AssertJ style assertions.
 *
 * <p>Can be in two distinct states:
 * <ol>
 * <li>The request processed successfully, and {@link #getUnresolvedException()}
 * is therefore {@code null}.</li>
 * <li>The request failed unexpectedly with {@link #getUnresolvedException()}
 * providing more information about the error. Any attempt to access a
 * member of the result fails with an exception.</li>
 * </ol>
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.2
 * @see AssertableMockMvc
 */
public interface AssertableMvcResult extends MvcResult, AssertProvider<MvcResultAssert> {

	/**
	 * Return the exception that was thrown unexpectedly while processing the
	 * request, if any.
	 */
	@Nullable
	Exception getUnresolvedException();

}
