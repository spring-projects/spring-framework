/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.test.web.servlet.result;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.StubMvcResult;

/**
 * Unit tests for {@link HeaderResultMatchers}.
 * @author Rossen Stoyanchev
 */
public class HeaderResultMatchersTests {

	private final HeaderResultMatchers matchers = new HeaderResultMatchers();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final MvcResult mvcResult =
			new StubMvcResult(new MockHttpServletRequest(), null, null, null, null, null, this.response);


	@Test // SPR-17330
	public void matchDateFormattedWithHttpHeaders() throws Exception {

		long epochMilli = ZonedDateTime.of(2018, 10, 5, 0, 0, 0, 0, ZoneId.of("GMT")).toInstant().toEpochMilli();
		HttpHeaders headers = new HttpHeaders();
		headers.setDate("myDate", epochMilli);
		this.response.setHeader("d", headers.getFirst("myDate"));

		this.matchers.dateValue("d", epochMilli).match(this.mvcResult);
	}

}
