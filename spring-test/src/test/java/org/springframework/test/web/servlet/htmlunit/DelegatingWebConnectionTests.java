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

package org.springframework.test.web.servlet.htmlunit;

import java.net.URL;
import java.util.Collections;

import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection.DelegateWebConnection;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit and integration tests for {@link DelegatingWebConnection}.
 *
 * @author Rob Winch
 * @since 4.2
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingWebConnectionTests {

	private DelegatingWebConnection webConnection;

	private WebRequest request;

	private WebResponse expectedResponse;


	@Mock
	private WebRequestMatcher matcher1;

	@Mock
	private WebRequestMatcher matcher2;

	@Mock
	private WebConnection defaultConnection;

	@Mock
	private WebConnection connection1;

	@Mock
	private WebConnection connection2;


	@Before
	public void setup() throws Exception {
		request = new WebRequest(new URL("http://localhost/"));
		WebResponseData data = new WebResponseData("".getBytes("UTF-8"), 200, "", Collections.emptyList());
		expectedResponse = new WebResponse(data, request, 100L);
		webConnection = new DelegatingWebConnection(defaultConnection,
				new DelegateWebConnection(matcher1, connection1), new DelegateWebConnection(matcher2, connection2));
	}


	@Test
	public void getResponseDefault() throws Exception {
		when(defaultConnection.getResponse(request)).thenReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response, sameInstance(expectedResponse));
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1, connection2);
		verify(defaultConnection).getResponse(request);
	}

	@Test
	public void getResponseAllMatches() throws Exception {
		when(matcher1.matches(request)).thenReturn(true);
		when(connection1.getResponse(request)).thenReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response, sameInstance(expectedResponse));
		verify(matcher1).matches(request);
		verifyNoMoreInteractions(matcher2, connection2, defaultConnection);
		verify(connection1).getResponse(request);
	}

	@Test
	public void getResponseSecondMatches() throws Exception {
		when(matcher2.matches(request)).thenReturn(true);
		when(connection2.getResponse(request)).thenReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response, sameInstance(expectedResponse));
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1, defaultConnection);
		verify(connection2).getResponse(request);
	}

	@Test
	public void verifyExampleInClassLevelJavadoc() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		WebClient webClient = new WebClient();

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup().build();
		MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc, webClient);

		WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
		WebConnection httpConnection = new HttpWebConnection(webClient);
		webClient.setWebConnection(
				new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection)));

		Page page = webClient.getPage("https://code.jquery.com/jquery-1.11.0.min.js");
		assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
		assertThat(page.getWebResponse().getContentAsString(), not(isEmptyString()));
	}


	@Controller
	static class TestController {
	}

}
