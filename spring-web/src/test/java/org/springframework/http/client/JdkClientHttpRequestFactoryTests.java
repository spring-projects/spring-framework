/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marten Deinum
 */
public class JdkClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Nullable
	private static String originalPropertyValue;

	@BeforeAll
	public static void setProperty() {
		originalPropertyValue = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
		System.setProperty("jdk.httpclient.allowRestrictedHeaders", "expect");
	}

	@AfterAll
	public static void restoreProperty() {
		if (originalPropertyValue != null) {
			System.setProperty("jdk.httpclient.allowRestrictedHeaders", originalPropertyValue);
		}
		else {
			System.clearProperty("jdk.httpclient.allowRestrictedHeaders");
		}
	}

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new JdkClientHttpRequestFactory();
	}

	@Override
	@Test
	public void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@Test
	public void customizeDisallowedHeaders() throws IOException {
			ClientHttpRequest request = factory.createRequest(URI.create(this.baseUrl + "/status/299"), HttpMethod.PUT);
			request.getHeaders().set("Expect", "299");

			try (ClientHttpResponse response = request.execute()) {
				assertThat(response.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatusCode.valueOf(299));
			}
	}

}
