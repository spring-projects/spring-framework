package org.springframework.web.client.reactive;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.web.client.reactive.ResponseExtractors.*;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.TestSubscriber;

import org.springframework.core.codec.StringDecoder;
import org.springframework.core.codec.StringEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;

/**
 * Unit tests for {@link DefaultResponseErrorHandler}.
 *
 * @author Brian Clozel
 */
public class DefaultResponseErrorHandlerTests {

	private DefaultResponseErrorHandler errorHandler;

	private ClientHttpResponse response;

	private List<HttpMessageConverter<?>> messageConverters;

	@Before
	public void setUp() throws Exception {
		this.errorHandler = new DefaultResponseErrorHandler();
		this.response = mock(ClientHttpResponse.class);
		this.messageConverters = Collections
				.singletonList(new CodecHttpMessageConverter<>(new StringEncoder(), new StringDecoder()));
	}

	@Test
	public void noError() throws Exception {
		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		this.errorHandler.handleError(this.response, this.messageConverters);
	}

	@Test
	public void clientError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer();
		buffer.write(new String("Page Not Found").getBytes("UTF-8"));
		given(this.response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(this.response.getHeaders()).willReturn(headers);
		given(this.response.getBody()).willReturn(Flux.just(buffer));
		try {
			this.errorHandler.handleError(this.response, this.messageConverters);
			fail("expected HttpClientErrorException");
		}
		catch (WebClientErrorException exc) {
			assertThat(exc.getMessage(), is("404 Not Found"));
			assertThat(exc.getStatus(), is(HttpStatus.NOT_FOUND));
			TestSubscriber.subscribe(exc.getResponseBody(as(String.class)))
					.awaitAndAssertNextValues("Page Not Found")
					.assertComplete();
		}
	}

	@Test
	public void serverError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		DataBuffer buffer = new DefaultDataBufferFactory().allocateBuffer();
		buffer.write(new String("Internal Server Error").getBytes("UTF-8"));
		given(this.response.getStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		given(this.response.getHeaders()).willReturn(headers);
		given(this.response.getBody()).willReturn(Flux.just(buffer));
		try {
			this.errorHandler.handleError(this.response, this.messageConverters);
			fail("expected HttpServerErrorException");
		}
		catch (WebServerErrorException exc) {
			assertThat(exc.getMessage(), is("500 Internal Server Error"));
			assertThat(exc.getStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR));
			TestSubscriber.subscribe(exc.getResponseBody(as(String.class)))
					.awaitAndAssertNextValues("Internal Server Error")
					.assertComplete();
		}
	}

}
