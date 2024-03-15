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

import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * The default {@link AssertableMvcResult} implementation.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
final class DefaultAssertableMvcResult implements AssertableMvcResult {

	@Nullable
	private final MvcResult target;

	@Nullable
	private final Exception unresolvedException;

	@Nullable
	private final GenericHttpMessageConverter<Object> jsonMessageConverter;

	DefaultAssertableMvcResult(@Nullable MvcResult target, @Nullable Exception unresolvedException, @Nullable GenericHttpMessageConverter<Object> jsonMessageConverter) {
		this.target = target;
		this.unresolvedException = unresolvedException;
		this.jsonMessageConverter = jsonMessageConverter;
	}

	/**
	 * Return the exception that was thrown unexpectedly while processing the
	 * request, if any.
	 */
	@Nullable
	public Exception getUnresolvedException() {
		return this.unresolvedException;
	}

	@Override
	public MockHttpServletRequest getRequest() {
		return getTarget().getRequest();
	}

	@Override
	public MockHttpServletResponse getResponse() {
		return getTarget().getResponse();
	}

	@Override
	public Object getHandler() {
		return getTarget().getHandler();
	}

	@Override
	public HandlerInterceptor[] getInterceptors() {
		return getTarget().getInterceptors();
	}

	@Override
	public ModelAndView getModelAndView() {
		return getTarget().getModelAndView();
	}

	@Override
	public Exception getResolvedException() {
		return getTarget().getResolvedException();
	}

	@Override
	public FlashMap getFlashMap() {
		return getTarget().getFlashMap();
	}

	@Override
	public Object getAsyncResult() {
		return getTarget().getAsyncResult();
	}

	@Override
	public Object getAsyncResult(long timeToWait) {
		return getTarget().getAsyncResult(timeToWait);
	}


	private MvcResult getTarget() {
		if (this.target == null) {
			throw new IllegalStateException(
					"Request has failed with unresolved exception " + this.unresolvedException);
		}
		return this.target;
	}

	/**
	 * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
	 * instead.
	 */
	@Override
	public MvcResultAssert assertThat() {
		return new MvcResultAssert(this, this.jsonMessageConverter);
	}

}
