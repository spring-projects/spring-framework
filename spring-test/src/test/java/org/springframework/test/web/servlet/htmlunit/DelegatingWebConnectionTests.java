/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.net.URI;
import java.util.Collections;

import org.htmlunit.HttpWebConnection;
import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.WebConnection;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.WebResponseData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection.DelegateWebConnection;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * Unit and integration tests for {@link DelegatingWebConnection}.
 *
 * @author Rob Winch
 * @since 4.2
 */
@ExtendWith(MockitoExtension.class)
class DelegatingWebConnectionTests {

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


	@BeforeEach
	void setup() throws Exception {
		request = new WebRequest(URI.create("http://localhost/").toURL());
		WebResponseData data = new WebResponseData("".getBytes(UTF_8), 200, "", Collections.emptyList());
		expectedResponse = new WebResponse(data, request, 100L);
		webConnection = new DelegatingWebConnection(defaultConnection,
				new DelegateWebConnection(matcher1, connection1), new DelegateWebConnection(matcher2, connection2));
	}


	@Test
	void getResponseDefault() throws Exception {
		given(defaultConnection.getResponse(request)).willReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1, connection2);
		verify(defaultConnection).getResponse(request);
	}

	@Test
	void getResponseAllMatches() throws Exception {
		given(matcher1.matches(request)).willReturn(true);
		given(connection1.getResponse(request)).willReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verifyNoMoreInteractions(matcher2, connection2, defaultConnection);
		verify(connection1).getResponse(request);
	}

	@Test
	void getResponseSecondMatches() throws Exception {
		given(matcher2.matches(request)).willReturn(true);
		given(connection2.getResponse(request)).willReturn(expectedResponse);
		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1, defaultConnection);
		verify(connection2).getResponse(request);
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void verifyExampleInClassLevelJavadoc() throws Exception {
		WebClient webClient = new WebClient();

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup().build();
		MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc, webClient);

		WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
		WebConnection httpConnection = new HttpWebConnection(webClient);
		webClient.setWebConnection(
				new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection)));

		Page page = webClient.getPage("https://code.jquery.com/jquery-1.11.0.min.js");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
		assertThat(page.getWebResponse().getContentAsString()).isNotEmpty();
	}


	@Controller
	static class TestController {
	}

}
