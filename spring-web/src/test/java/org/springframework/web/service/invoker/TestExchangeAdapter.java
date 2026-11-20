/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.invoker;

import java.util.Collections;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HttpExchangeAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
public class TestExchangeAdapter implements HttpExchangeAdapter {

	private @Nullable String invokedMethodName;

	private @Nullable HttpRequestValues requestValues;

	private @Nullable ParameterizedTypeReference<?> bodyType;


	public String getInvokedMethodName() {
		assertThat(this.invokedMethodName).isNotNull();
		return this.invokedMethodName;
	}

	public HttpRequestValues getRequestValues() {
		assertThat(this.requestValues).isNotNull();
		return this.requestValues;
	}

	public @Nullable ParameterizedTypeReference<?> getBodyType() {
		return this.bodyType;
	}

	@Override
	public void exchange(HttpRequestValues requestValues) {
		saveInput("exchange", requestValues, null);
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		saveInput("exchangeForHeaders", requestValues, null);
		return new HttpHeaders();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		saveInput("exchangeForBody", requestValues, bodyType);
		return bodyType.getType().getTypeName().contains("List") ?
				(T) Collections.singletonList(getInvokedMethodName()) : (T) getInvokedMethodName();
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		saveInput("exchangeForBodilessEntity", requestValues, null);
		return ResponseEntity.ok().build();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> ResponseEntity<T> exchangeForEntity(
			HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

		saveInput("exchangeForEntity", requestValues, bodyType);
		return (ResponseEntity<T>) ResponseEntity.ok(getInvokedMethodName());
	}

	@Override
	public boolean supportsRequestAttributes() {
		return true;
	}

	protected <T> void saveInput(
			String methodName, HttpRequestValues values, @Nullable ParameterizedTypeReference<T> bodyType) {

		this.invokedMethodName = methodName;
		this.requestValues = values;
		this.bodyType = bodyType;
	}

}
