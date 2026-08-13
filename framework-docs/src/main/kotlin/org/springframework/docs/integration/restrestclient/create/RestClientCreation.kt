/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.docs.integration.restrestclient.create

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInitializer
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.ApiVersionInserter
import org.springframework.web.client.RestClient

class RestClientCreation {

	fun createRestClient() {
		// tag::snippet[]
		val defaultClient = RestClient.create()

		val customClient = RestClient.builder()
			.requestFactory(HttpComponentsClientHttpRequestFactory())
			.configureMessageConverters { converters -> converters.addCustomConverter(MyCustomMessageConverter()) }
			.baseUrl("https://example.com")
			.defaultUriVariables(mapOf("variable" to "foo"))
			.defaultHeader("My-Header", "Foo")
			.defaultCookie("My-Cookie", "Bar")
			.defaultApiVersion("1.2")
			.apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
			.requestInterceptor(MyCustomInterceptor())
			.requestInitializer(MyCustomInitializer())
			.build()
		// end::snippet[]
	}

	private class MyCustomMessageConverter : StringHttpMessageConverter()

	private class MyCustomInterceptor : ClientHttpRequestInterceptor {

		override fun intercept(
			request: HttpRequest,
			body: ByteArray,
			execution: ClientHttpRequestExecution
		): ClientHttpResponse {
			return execution.execute(request, body)
		}
	}

	private class MyCustomInitializer : ClientHttpRequestInitializer {

		override fun initialize(request: ClientHttpRequest) {
			request.headers.add("My-Header", "My-Value")
		}
	}

}
