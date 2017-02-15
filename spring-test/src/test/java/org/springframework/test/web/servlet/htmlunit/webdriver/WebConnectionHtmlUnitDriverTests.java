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

package org.springframework.test.web.servlet.htmlunit.webdriver;

import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebConnectionHtmlUnitDriver}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(MockitoJUnitRunner.class)
public class WebConnectionHtmlUnitDriverTests {

	private final WebConnectionHtmlUnitDriver driver = new WebConnectionHtmlUnitDriver();

	@Mock
	private WebConnection connection;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() throws Exception {
		when(this.connection.getResponse(any(WebRequest.class))).thenThrow(new InternalError(""));
	}

	@Test
	public void getWebConnectionDefaultNotNull() {
		assertThat(this.driver.getWebConnection(), notNullValue());
	}

	@Test
	public void setWebConnectionToNull() {
		this.exception.expect(IllegalArgumentException.class);
		this.driver.setWebConnection(null);
	}

	@Test
	public void setWebConnection() {
		this.driver.setWebConnection(this.connection);
		assertThat(this.driver.getWebConnection(), equalTo(this.connection));

		this.exception.expect(InternalError.class);
		this.driver.get("https://example.com");
	}

}