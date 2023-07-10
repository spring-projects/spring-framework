/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.util.Collections;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HttpExchangeAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
public class TestExchangeAdapter implements HttpExchangeAdapter {

	@Nullable
	private String invokedMethodName;

	@Nullable
	private HttpRequestValues requestValues;

	@Nullable
	private ParameterizedTypeReference<?> bodyType;


	public String getInvokedMethodName() {
		assertThat(this.invokedMethodName).isNotNull();
		return this.invokedMethodName;
	}

	public HttpRequestValues getRequestValues() {
		assertThat(this.requestValues).isNotNull();
		return this.requestValues;
	}

	@Nullable
	public ParameterizedTypeReference<?> getBodyType() {
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

	protected  <T> void saveInput(
			String methodName, HttpRequestValues values, @Nullable ParameterizedTypeReference<T> bodyType) {

		this.invokedMethodName = methodName;
		this.requestValues = values;
		this.bodyType = bodyType;
	}

}
