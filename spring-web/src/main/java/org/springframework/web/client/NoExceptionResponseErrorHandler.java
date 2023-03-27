package org.springframework.web.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Swallow exception instead of throw it
 *
 * @author hongxue.zou
 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
 */
public class NoExceptionResponseErrorHandler extends DefaultResponseErrorHandler {

	private final Log logger;

	public NoExceptionResponseErrorHandler() {
		this.logger = LogFactory.getLog(NoExceptionResponseErrorHandler.class);
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("The http status of response is " + response.getStatusCode());
		}
	}
}
