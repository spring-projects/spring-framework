/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
