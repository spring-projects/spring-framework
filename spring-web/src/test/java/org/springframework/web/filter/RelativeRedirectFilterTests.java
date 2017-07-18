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

package org.springframework.web.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockFilterChain;
import org.springframework.mock.web.test.MockHttpServletRequest;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Rob Winch
 * @since 4.3.10
 */
@RunWith(MockitoJUnitRunner.class)
public class RelativeRedirectFilterTests {
	@Mock
	HttpServletResponse response;

	RelativeRedirectFilter filter = new RelativeRedirectFilter();

	@Test(expected = IllegalArgumentException.class)
	public void sendRedirectHttpStatusWhenNullThenIllegalArgumentException() {
		this.filter.setSendRedirectHttpStatus(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void sendRedirectHttpStatusWhenNot3xxThenIllegalArgumentException() {
		this.filter.setSendRedirectHttpStatus(HttpStatus.OK);
	}

	@Test
	public void doFilterSendRedirectWhenDefaultsThenLocationAnd302() throws Exception {
		String location = "/foo";

		sendRedirect(location);

		InOrder inOrder = Mockito.inOrder(this.response);
		inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
		inOrder.verify(this.response).setStatus(HttpStatus.FOUND.value());
	}

	@Test
	public void doFilterSendRedirectWhenCustomSendRedirectHttpStatusThenLocationAnd301() throws Exception {
		String location = "/foo";
		HttpStatus status = HttpStatus.MOVED_PERMANENTLY;
		this.filter.setSendRedirectHttpStatus(status);
		sendRedirect(location);

		InOrder inOrder = Mockito.inOrder(this.response);
		inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
		inOrder.verify(this.response).setStatus(status.value());
	}

	private void sendRedirect(String location) throws Exception {
		MockFilterChain chain = new MockFilterChain();

		filter.doFilterInternal(new MockHttpServletRequest(), response, chain);

		HttpServletResponse wrappedResponse = (HttpServletResponse) chain.getResponse();
		wrappedResponse.sendRedirect(location);
	}
}