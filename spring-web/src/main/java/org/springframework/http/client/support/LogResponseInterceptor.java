package org.springframework.http.client.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * For debug log level, log for all http status
 * For other log level, only log it when error happens
 *
 * @author hongxue.zou
 * @see RestTemplate#setInterceptors(List)
 */
public class LogResponseInterceptor implements ClientHttpRequestInterceptor {

	private final Log logger;

	public LogResponseInterceptor() {
		this.logger = LogFactory.getLog(LogResponseInterceptor.class);
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		ClientHttpResponse response = execution.execute(request, body);
		if (logger.isDebugEnabled() || response.getStatusCode().isError()) {
			logResponse(response);
		}
		return response;
	}

	private void logResponse(ClientHttpResponse response) throws IOException {
		StringBuilder error = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
			String line = bufferedReader.readLine();
			while (line != null) {
				error.append(line);
				error.append('\n');
				line = bufferedReader.readLine();
			}
			logger.warn("The response status is " + response.getStatusCode() + ", the response error is " + error);
		}
	}
}
