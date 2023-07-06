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
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
public class TestHttpExchangeAdapter implements HttpExchangeAdapter, TestAdapter {

	@Nullable
	private String invokedMethodName;

	@Nullable
	private HttpRequestValues requestValues;

	@Nullable
	private ParameterizedTypeReference<?> bodyType;

	public String getInvokedMethodReference() {
		assertThat(this.invokedMethodName).isNotNull();
		return this.invokedMethodName;
	}

	public HttpRequestValues getRequestValues() {
		assertThat(this.requestValues).isNotNull();
		return this.requestValues;
	}

	@Override
	@Nullable
	public ParameterizedTypeReference<?> getBodyType() {
		return this.bodyType;
	}

	@Override
	public Void exchange(HttpRequestValues requestValues) {
		saveInput("void", requestValues, null);
		return null;
	}

	@Override
	public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
		saveInput("headers", requestValues, null);
		return new HttpHeaders();
	}

	@Override
	public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
		saveInput("body", requestValues, bodyType);
		return bodyType.getType().getTypeName().contains("List")
				? (T) Collections.singletonList(getInvokedMethodReference()) : (T) getInvokedMethodReference();
	}

	@Override
	public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
		saveInput("bodilessEntity", requestValues, null);
		return ResponseEntity.ok().build();
	}

	@Override
	public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues,
			ParameterizedTypeReference<T> bodyType) {
		saveInput("entity", requestValues, bodyType);
		return (ResponseEntity<T>) ResponseEntity.ok(this.getInvokedMethodReference());
	}

	@Override
	public boolean supportsRequestAttributes() {
		return false;
	}

	private <T> void saveInput(String methodName, HttpRequestValues requestValues,
			@Nullable ParameterizedTypeReference<T> bodyType) {

		this.invokedMethodName = methodName;
		this.requestValues = requestValues;
		this.bodyType = bodyType;
	}

}
