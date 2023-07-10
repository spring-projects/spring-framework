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

package org.springframework.web.client.support

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.lang.Nullable
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.service.annotation.PutExchange
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.testfixture.servlet.MockMultipartFile
import org.springframework.web.util.DefaultUriBuilderFactory
import java.net.URI
import java.util.*

/**
 * Kotlin integration tests for {@link HttpServiceProxyFactory HTTP Service proxy} using
 * {@link RestTemplate} and {@link MockWebServer}.
 *
 * @author Olga Maciaszek-Sharma
 */
class KotlinRestTemplateHttpServiceProxyTests {

    private lateinit var server: MockWebServer

    private lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        prepareResponse()
        testService = initTestService()
    }

    private fun initTestService(): TestService {
        val restTemplate = RestTemplate()
        restTemplate.uriTemplateHandler = DefaultUriBuilderFactory(server.url("/").toString())
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestTemplateAdapter.create(restTemplate))
                .build()
                .createClient(TestService::class.java)
    }

    @AfterEach
    fun shutDown() {
        server.shutdown()
    }

    @Test
    @Throws(InterruptedException::class)
    fun getRequest() {
        val response = testService.request

        val request = server.takeRequest()
        assertThat(response).isEqualTo("Hello Spring!")
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/test")
    }

    @Test
    @Throws(InterruptedException::class)
    fun getRequestWithPathVariable() {
        val response = testService.getRequestWithPathVariable("456")

        val request = server.takeRequest()
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo("Hello Spring!")
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.path).isEqualTo("/test/456")
    }

    @Test
    @Throws(InterruptedException::class)
    fun getRequestWithDynamicUri() {
        val dynamicUri = server.url("/greeting/123").uri()

        val response = testService.getRequestWithDynamicUri(dynamicUri, "456")

        val request = server.takeRequest()
        assertThat(response.orElse("empty")).isEqualTo("Hello Spring!")
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.requestUrl.uri()).isEqualTo(dynamicUri)
    }

    @Test
    @Throws(InterruptedException::class)
    fun postWithRequestHeader() {
        testService.postRequestWithHeader("testHeader", "testBody")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/test")
        assertThat(request.headers["testHeaderName"]).isEqualTo("testHeader")
        assertThat(request.body.readUtf8()).isEqualTo("testBody")
    }

    @Test
    @Throws(Exception::class)
    fun formData() {
        val map: MultiValueMap<String, String> = LinkedMultiValueMap()
        map.add("param1", "value 1")
        map.add("param2", "value 2")

        testService.postForm(map)

        val request = server.takeRequest()
        assertThat(request.headers["Content-Type"])
                .isEqualTo("application/x-www-form-urlencoded;charset=UTF-8")
        assertThat(request.body.readUtf8()).isEqualTo("param1=value+1&param2=value+2")
    }

    // gh-30342
    @Test
    @Throws(InterruptedException::class)
    fun multipart() {
        val fileName = "testFileName"
        val originalFileName = "originalTestFileName"
        val file: MultipartFile = MockMultipartFile(fileName, originalFileName, MediaType.APPLICATION_JSON_VALUE,
                "test".toByteArray())

        testService.postMultipart(file, "test2")

        val request = server.takeRequest()
        assertThat(request.headers["Content-Type"]).startsWith("multipart/form-data;boundary=")
        assertThat(request.body.readUtf8()).containsSubsequence(
                "Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
                "Content-Type: application/json", "Content-Length: 4", "test",
                "Content-Disposition: form-data; name=\"anotherPart\"", "Content-Type: text/plain;charset=UTF-8",
                "Content-Length: 5", "test2")
    }

    @Test
    @Throws(InterruptedException::class)
    fun putRequestWithCookies() {
        testService.putRequestWithCookies("test1", "test2")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("PUT")
        assertThat(request.getHeader("Cookie"))
                .isEqualTo("firstCookie=test1; secondCookie=test2")
    }

    @Test
    @Throws(InterruptedException::class)
    fun putRequestWithSameNameCookies() {
        testService.putRequestWithSameNameCookies("test1", "test2")

        val request = server.takeRequest()
        assertThat(request.method).isEqualTo("PUT")
        assertThat(request.getHeader("Cookie"))
                .isEqualTo("testCookie=test1; testCookie=test2")
    }

    private fun prepareResponse() {
        val response = MockResponse()
        response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!")
        server.enqueue(response)
    }


    private interface TestService {

        @get:GetExchange("/test")
        val request: String

        @GetExchange("/test/{id}")
        fun getRequestWithPathVariable(@PathVariable id: String): ResponseEntity<String>

        @GetExchange("/test/{id}")
        fun getRequestWithDynamicUri(@Nullable uri: URI, @PathVariable id: String): Optional<String>

        @PostExchange("/test")
        fun postRequestWithHeader(@RequestHeader("testHeaderName") testHeader: String,
                                  @RequestBody requestBody: String)

        @PostExchange(contentType = "application/x-www-form-urlencoded")
        fun postForm(@RequestParam params: MultiValueMap<String, String>)

        @PostExchange
        fun postMultipart(file: MultipartFile, @RequestPart anotherPart: String)

        @PutExchange
        fun putRequestWithCookies(@CookieValue firstCookie: String,
                                  @CookieValue secondCookie: String)

        @PutExchange
        fun putRequestWithSameNameCookies(@CookieValue("testCookie") firstCookie: String,
                                          @CookieValue("testCookie") secondCookie: String)
    }

}