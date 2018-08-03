package org.springframework.web.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.*;

@RunWith(Parameterized.class)
public class DefaultReponseHanderSpecificErrorsTests {

	@Parameters(name = "error: [{0}], exception: [{1}]")
	public static Object[][] errorCodes() {
		return new Object[][]{
				// 4xx
				{BAD_REQUEST, HttpClientErrorException.BadRequest.class},
				{UNAUTHORIZED, HttpClientErrorException.Unauthorized.class},
				{FORBIDDEN, HttpClientErrorException.Forbidden.class},
				{NOT_FOUND, HttpClientErrorException.NotFound.class},
				{METHOD_NOT_ALLOWED, HttpClientErrorException.MethodNotAllowed.class},
				{NOT_ACCEPTABLE, HttpClientErrorException.NotAcceptable.class},
				{CONFLICT, HttpClientErrorException.Conflict.class},
				{TOO_MANY_REQUESTS, HttpClientErrorException.TooManyRequests.class},
				{UNPROCESSABLE_ENTITY, HttpClientErrorException.UnprocessableEntity.class},
				{I_AM_A_TEAPOT, HttpClientErrorException.class},
				// 5xx
				{INTERNAL_SERVER_ERROR, HttpServerErrorException.InternalServerError.class},
				{NOT_IMPLEMENTED, HttpServerErrorException.NotImplemented.class},
				{BAD_GATEWAY, HttpServerErrorException.BadGateway.class},
				{SERVICE_UNAVAILABLE, HttpServerErrorException.ServiceUnavailable.class},
				{GATEWAY_TIMEOUT, HttpServerErrorException.GatewayTimeout.class},
				{HTTP_VERSION_NOT_SUPPORTED, HttpServerErrorException.class}
		};
	}

	@Parameterized.Parameter
	public HttpStatus httpStatus;

	@Parameterized.Parameter(1)
	public Class expectedExceptionClass;

	private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);

	@Test
	public void handleErrorIOException() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(httpStatus.value());
		given(response.getHeaders()).willReturn(headers);

		try {
			handler.handleError(response);
			fail("expected " + expectedExceptionClass.getSimpleName());
		}
		catch (HttpStatusCodeException ex) {
			assertEquals("Expected " + expectedExceptionClass.getSimpleName(), expectedExceptionClass, ex.getClass());
		}
	}

	@Test
	public void hasErrorTrue() throws Exception {
		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		assertTrue(handler.hasError(response));
	}

}
