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

package org.springframework.web.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Mono
import java.security.Principal

/**
 * Mock object based tests for [ServerWebExchange] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class ServerWebExchangeExtensionsTest {

	@Test
	fun `awaitFormData extension`() {
		val exchange = mockk<ServerWebExchange>()
		val multiMap = mockk<MultiValueMap<String, String>>()
		every { exchange.formData } returns Mono.just(multiMap)
		runBlocking {
			assertThat(exchange.awaitFormData()).isEqualTo(multiMap)
		}
	}

	@Test
	fun `awaitMultipartData extension`() {
		val exchange = mockk<ServerWebExchange>()
		val multiMap = mockk<MultiValueMap<String, Part>>()
		every { exchange.multipartData } returns Mono.just(multiMap)
		runBlocking {
			assertThat(exchange.awaitMultipartData()).isEqualTo(multiMap)
		}
	}

	@Test
	fun `awaitPrincipal extension`() {
		val exchange = mockk<ServerWebExchange>()
		val principal = mockk<Principal>()
		every { exchange.getPrincipal<Principal>() } returns Mono.just(principal)
		runBlocking {
			assertThat(exchange.awaitPrincipal<Principal>()).isEqualTo(principal)
		}
		verify { exchange.getPrincipal<Principal>() }
	}

	@Test
	fun `awaitSession extension`() {
		val exchange = mockk<ServerWebExchange>()
		val session = mockk<WebSession>()
		every { exchange.session } returns Mono.just(session)
		runBlocking {
			assertThat(exchange.awaitSession()).isEqualTo(session)
		}
	}

	@Test
	fun `principal builder extension`() {
		val builder = mockk<ServerWebExchange.Builder>()
		val principal = mockk<Principal>()
		every { builder.principal(any<Mono<Principal>>()) } returns builder
		runBlocking {
			assertThat(builder.principal { principal }).isEqualTo(builder)
		}
		verify { builder.principal(any<Mono<Principal>>()) }
	}

}
