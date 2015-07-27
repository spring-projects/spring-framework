/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.test.web.servlet.htmlunit.webdriver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Rob Winch
 * @since 4.2
 */
@RunWith(MockitoJUnitRunner.class)
public class WebConnectionHtmlUnitDriverTests {

	@Mock
	WebConnection connection;

	WebConnectionHtmlUnitDriver driver;

	@Before
	public void setup() throws Exception {
		driver = new WebConnectionHtmlUnitDriver();

		when(connection.getResponse(any(WebRequest.class))).thenThrow(new InternalError(""));
	}

	@Test
	public void getWebConnectionDefaultNotNull() {
		assertThat(driver.getWebConnection(), notNullValue());
	}

	@Test
	public void setWebConnection() {
		driver.setWebConnection(connection);

		assertThat(driver.getWebConnection(), equalTo(connection));
		try {
			driver.get("https://example.com");
			fail("Expected Exception");
		} catch (InternalError success) {}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setWebConnectionNull() {
		driver.setWebConnection(null);
	}

}