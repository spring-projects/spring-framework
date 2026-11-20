/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
	suspend fun `awaitFormData extension`() {
		val exchange = mockk<ServerWebExchange>()
		val multiMap = mockk<MultiValueMap<String, String>>()
		every { exchange.formData } returns Mono.just(multiMap)
		assertThat(exchange.awaitFormData()).isEqualTo(multiMap)
	}

	@Test
	suspend fun `awaitMultipartData extension`() {
		val exchange = mockk<ServerWebExchange>()
		val multiMap = mockk<MultiValueMap<String, Part>>()
		every { exchange.multipartData } returns Mono.just(multiMap)
		assertThat(exchange.awaitMultipartData()).isEqualTo(multiMap)
	}

	@Test
	suspend fun `awaitPrincipal extension`() {
		val exchange = mockk<ServerWebExchange>()
		val principal = mockk<Principal>()
		every { exchange.getPrincipal<Principal>() } returns Mono.just(principal)
		assertThat(exchange.awaitPrincipal<Principal>()).isEqualTo(principal)
		verify { exchange.getPrincipal<Principal>() }
	}

	@Test
	suspend fun `awaitSession extension`() {
		val exchange = mockk<ServerWebExchange>()
		val session = mockk<WebSession>()
		every { exchange.session } returns Mono.just(session)
		assertThat(exchange.awaitSession()).isEqualTo(session)
	}

	@Test
	fun `principal builder extension`() {
		val builder = mockk<ServerWebExchange.Builder>()
		val principal = mockk<Principal>()
		every { builder.principal(any<Mono<Principal>>()) } returns builder
		assertThat(builder.principal { principal }).isEqualTo(builder)
		verify { builder.principal(any<Mono<Principal>>()) }
	}

}
