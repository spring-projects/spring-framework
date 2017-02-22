package org.springframework.http.client;

import java.util.Collections;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Brian Clozel
 */
public class AbstractMockWebServerTestCase {

	private MockWebServer server;

	protected int port;

	protected String baseUrl;

	protected static final MediaType textContentType =
			new MediaType("text", "plain", Collections.singletonMap("charset", "UTF-8"));

	@Before
	public void setUp() throws Exception {
		this.server = new MockWebServer();
		this.server.setDispatcher(new TestDispatcher());
		this.server.start();
		this.port = this.server.getPort();
		this.baseUrl = "http://localhost:" + this.port;
	}

	@After
	public void tearDown() throws Exception {
		this.server.shutdown();
	}

	protected class TestDispatcher extends Dispatcher {
		@Override
		public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
			try {
				if (request.getPath().equals("/echo")) {
					MockResponse response = new MockResponse()
							.setHeaders(request.getHeaders())
							.setHeader("Content-Length", request.getBody().size())
							.setResponseCode(200)
							.setBody(request.getBody());
					request.getBody().flush();
					return response;
				}
				else if(request.getPath().equals("/status/ok")) {
					return new MockResponse();
				}
				else if(request.getPath().equals("/status/notfound")) {
					return new MockResponse().setResponseCode(404);
				}
				else if(request.getPath().startsWith("/params")) {
					assertThat(request.getPath(), Matchers.containsString("param1=value"));
					assertThat(request.getPath(), Matchers.containsString("param2=value1&param2=value2"));
					return new MockResponse();
				}
				else if(request.getPath().equals("/methods/post")) {
					assertThat(request.getMethod(), Matchers.is("POST"));
					String transferEncoding = request.getHeader("Transfer-Encoding");
					if(StringUtils.hasLength(transferEncoding)) {
						assertThat(transferEncoding, Matchers.is("chunked"));
					}
					else {
						long contentLength = Long.parseLong(request.getHeader("Content-Length"));
						assertThat("Invalid content-length",
								request.getBody().size(), Matchers.is(contentLength));
					}
					return new MockResponse().setResponseCode(200);
				}
				else if(request.getPath().startsWith("/methods/")) {
					String expectedMethod = request.getPath().replace("/methods/","").toUpperCase();
					assertThat(request.getMethod(), Matchers.is(expectedMethod));
					return new MockResponse();
				}
				return new MockResponse().setResponseCode(404);
			}
			catch (Throwable exc) {
				return new MockResponse().setResponseCode(500).setBody(exc.toString());
			}
		}
	}
}
