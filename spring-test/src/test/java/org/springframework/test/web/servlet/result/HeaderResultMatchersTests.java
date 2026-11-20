/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.StubMvcResult;

/**
 * Tests for {@link HeaderResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
class HeaderResultMatchersTests {

	private final HeaderResultMatchers matchers = new HeaderResultMatchers();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final MvcResult mvcResult =
			new StubMvcResult(new MockHttpServletRequest(), null, null, null, null, null, this.response);


	@Test // SPR-17330
	void matchDateFormattedWithHttpHeaders() throws Exception {

		long epochMilli = ZonedDateTime.of(2018, 10, 5, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
		HttpHeaders headers = new HttpHeaders();
		headers.setDate("myDate", epochMilli);
		this.response.setHeader("d", headers.getFirst("myDate"));

		this.matchers.dateValue("d", epochMilli).match(this.mvcResult);
	}

}
