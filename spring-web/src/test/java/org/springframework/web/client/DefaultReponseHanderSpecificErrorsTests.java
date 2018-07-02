package org.springframework.web.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.response.HttpBadGatewayException;
import org.springframework.web.client.response.HttpBadRequestException;
import org.springframework.web.client.response.HttpConflictException;
import org.springframework.web.client.response.HttpForbiddenException;
import org.springframework.web.client.response.HttpGatewayTimeoutException;
import org.springframework.web.client.response.HttpInternalServerErrorException;
import org.springframework.web.client.response.HttpMethodNotAllowedException;
import org.springframework.web.client.response.HttpNotAcceptableException;
import org.springframework.web.client.response.HttpNotFoundException;
import org.springframework.web.client.response.HttpNotImplementedException;
import org.springframework.web.client.response.HttpServiceUnavailableException;
import org.springframework.web.client.response.HttpTooManyRequestsException;
import org.springframework.web.client.response.HttpUnauthorizedException;
import org.springframework.web.client.response.HttpUnprocessableEntityException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@RunWith(Parameterized.class)
public class DefaultReponseHanderSpecificErrorsTests {

	@Parameters(name = "error: [{0}], exception: [{1}]")
	public static Object[][] errorCodes() {
		return new Object[][]{
				// 4xx
				{BAD_REQUEST, HttpBadRequestException.class},
				{UNAUTHORIZED, HttpUnauthorizedException.class},
				{FORBIDDEN, HttpForbiddenException.class},
				{NOT_FOUND, HttpNotFoundException.class},
				{METHOD_NOT_ALLOWED, HttpMethodNotAllowedException.class},
				{NOT_ACCEPTABLE, HttpNotAcceptableException.class},
				{CONFLICT, HttpConflictException.class},
				{TOO_MANY_REQUESTS, HttpTooManyRequestsException.class},
				{UNPROCESSABLE_ENTITY, HttpUnprocessableEntityException.class},
				{I_AM_A_TEAPOT, HttpClientErrorException.class},
				// 5xx
				{INTERNAL_SERVER_ERROR, HttpInternalServerErrorException.class},
				{NOT_IMPLEMENTED, HttpNotImplementedException.class},
				{BAD_GATEWAY, HttpBadGatewayException.class},
				{SERVICE_UNAVAILABLE, HttpServiceUnavailableException.class},
				{GATEWAY_TIMEOUT, HttpGatewayTimeoutException.class},
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
		} catch (HttpStatusCodeException ex) {
			assertEquals("Expected " + expectedExceptionClass.getSimpleName(), expectedExceptionClass, ex.getClass());
		}
	}

	@Test
	public void hasErrorTrue() throws Exception {
		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		assertTrue(handler.hasError(response));
	}

}
