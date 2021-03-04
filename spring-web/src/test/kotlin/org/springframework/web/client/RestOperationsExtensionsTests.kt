/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.util.ReflectionUtils
import java.net.URI
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.kotlinFunction

/**
 * Mock object based tests for [RestOperations] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class RestOperationsExtensionsTests {

	val template = mockk<RestOperations>()

	val foo = mockk<Foo>()

	val entity = mockk<ResponseEntity<Foo>>()

	@Test
	fun `getForObject with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val var1 = "var1"
		val var2 = "var2"
		every { template.getForObject(url, Foo::class.java, var1, var2) } returns foo
		assertThat(template.getForObject<Foo>(url, var1, var2)).isEqualTo(foo)
		verify { template.getForObject(url, Foo::class.java, var1, var2) }
	}

	@Test
	fun `getForObject with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		every { template.getForObject(url, Foo::class.java, vars) } returns foo
		assertThat(template.getForObject<Foo>(url, vars)).isEqualTo(foo)
		verify { template.getForObject(url, Foo::class.java, vars) }
	}

	@Test
	fun `getForObject with reified type parameters and URI`() {
		val url = URI("https://spring.io")
		every { template.getForObject(url, Foo::class.java) } returns foo
		assertThat(template.getForObject<Foo>(url)).isEqualTo(foo)
		verify { template.getForObject(url, Foo::class.java) }
	}

	@Test
	fun `getForEntity with reified type parameters, String and URI`() {
		val url = URI("https://spring.io")
		every { template.getForEntity(url, Foo::class.java) } returns entity
		assertThat(template.getForEntity<Foo>(url)).isEqualTo(entity)
		verify { template.getForEntity(url, Foo::class.java) }
	}

	@Test
	fun `getForEntity with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val var1 = "var1"
		val var2 = "var2"
		every { template.getForEntity(url, Foo::class.java, var1, var2) } returns entity
		assertThat(template.getForEntity<Foo>(url, var1, var2)).isEqualTo(entity)
		verify { template.getForEntity(url, Foo::class.java, var1, var2) }
	}

	@Test
	fun `getForEntity with reified type parameters and Map`() {
		val url = "https://spring.io"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		every { template.getForEntity(url, Foo::class.java, vars) } returns entity
		assertThat(template.getForEntity<Foo>(url, vars)).isEqualTo(entity)
		verify { template.getForEntity(url, Foo::class.java, vars) }
	}

	@Test
	fun `patchForObject with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		every { template.patchForObject(url, body, Foo::class.java, var1, var2) } returns foo
		assertThat(template.patchForObject<Foo>(url, body, var1, var2)).isEqualTo(foo)
		verify { template.patchForObject(url, body, Foo::class.java, var1, var2) }
	}

	@Test
	fun `patchForObject with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		every { template.patchForObject(url, body, Foo::class.java, vars) } returns foo
		assertThat(template.patchForObject<Foo>(url, body, vars)).isEqualTo(foo)
		verify { template.patchForObject(url, body, Foo::class.java, vars) }
	}

	@Test
	fun `patchForObject with reified type parameters and String`() {
		val url = "https://spring.io"
		val body: Any = "body"
		every { template.patchForObject(url, body, Foo::class.java) } returns foo
		assertThat(template.patchForObject<Foo>(url, body)).isEqualTo(foo)
		verify { template.patchForObject(url, body, Foo::class.java) }
	}

	@Test
	fun `patchForObject with reified type parameters`() {
		val url = "https://spring.io"
		every { template.patchForObject(url, null, Foo::class.java) } returns foo
		assertThat(template.patchForObject<Foo>(url)).isEqualTo(foo)
		verify { template.patchForObject(url, null, Foo::class.java) }
	}

	@Test
	fun `postForObject with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		every { template.postForObject(url, body, Foo::class.java, var1, var2) } returns foo
		assertThat(template.postForObject<Foo>(url, body, var1, var2)).isEqualTo(foo)
		verify { template.postForObject(url, body, Foo::class.java, var1, var2) }
	}

	@Test
	fun `postForObject with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		every { template.postForObject(url, body, Foo::class.java, vars) } returns foo
		assertThat(template.postForObject<Foo>(url, body, vars)).isEqualTo(foo)
		verify { template.postForObject(url, body, Foo::class.java, vars) }
	}

	@Test
	fun `postForObject with reified type parameters and String`() {
		val url = "https://spring.io"
		val body: Any = "body"
		every { template.postForObject(url, body, Foo::class.java) } returns foo
		assertThat(template.postForObject<Foo>(url, body)).isEqualTo(foo)
		verify { template.postForObject(url, body, Foo::class.java) }
	}

	@Test
	fun `postForObject with reified type parameters`() {
		val url = "https://spring.io"
		every { template.postForObject(url, null, Foo::class.java) } returns foo
		assertThat(template.postForObject<Foo>(url)).isEqualTo(foo)
		verify { template.postForObject(url, null, Foo::class.java) }
	}

	@Test
	fun `postForEntity with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		every { template.postForEntity(url, body, Foo::class.java, var1, var2) } returns entity
		assertThat(template.postForEntity<Foo>(url, body, var1, var2)).isEqualTo(entity)
		verify { template.postForEntity(url, body, Foo::class.java, var1, var2) }
	}

	@Test
	fun `postForEntity with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		every { template.postForEntity(url, body, Foo::class.java, vars) } returns entity
		assertThat(template.postForEntity<Foo>(url, body, vars)).isEqualTo(entity)
		verify { template.postForEntity(url, body, Foo::class.java, vars) }
	}

	@Test
	fun `postForEntity with reified type parameters and String`() {
		val url = "https://spring.io"
		val body: Any = "body"
		every { template.postForEntity(url, body, Foo::class.java) } returns entity
		assertThat(template.postForEntity<Foo>(url, body)).isEqualTo(entity)
		verify { template.postForEntity(url, body, Foo::class.java) }
	}

	@Test
	fun `postForEntity with reified type parameters`() {
		val url = "https://spring.io"
		every  { template.postForEntity(url, null, Foo::class.java) } returns entity
		assertThat(template.postForEntity<Foo>(url)).isEqualTo(entity)
		verify { template.postForEntity(url, null, Foo::class.java) }
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity and varargs`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val var1 = "var1"
		val var2 = "var2"
		val entityList = mockk<ResponseEntity<List<Foo>>>()
		val responseType = object : ParameterizedTypeReference<List<Foo>>() {}
		every { template.exchange(url, method, entity, responseType, var1, var2) } returns entityList
		assertThat(template.exchange<List<Foo>>(url, method, entity, var1, var2)).isEqualTo(entityList)
		verify { template.exchange(url, method, entity, responseType, var1, var2) }
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity and Map`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mockk<HttpEntity<Foo>>()
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		val entityList = mockk<ResponseEntity<List<Foo>>>()
		val responseType = object : ParameterizedTypeReference<List<Foo>>() {}
		every { template.exchange(url, method, entity, responseType, vars) } returns entityList
		assertThat(template.exchange<List<Foo>>(url, method, entity, vars)).isEqualTo(entityList)
		verify { template.exchange(url, method, entity, responseType, vars) }
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod and HttpEntity`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mockk<HttpEntity<Foo>>()
		val entityList = mockk<ResponseEntity<List<Foo>>>()
		val responseType = object : ParameterizedTypeReference<List<Foo>>() {}
		every { template.exchange(url, method, entity, responseType) } returns entityList
		assertThat(template.exchange<List<Foo>>(url, method, entity)).isEqualTo(entityList)
		verify { template.exchange(url, method, entity, responseType) }
	}

	@Test
	fun `exchange with reified type parameters, String and HttpMethod`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entityList = mockk<ResponseEntity<List<Foo>>>()
		val responseType = object : ParameterizedTypeReference<List<Foo>>() {}
		every { template.exchange(url, method, null, responseType) } returns entityList
		assertThat(template.exchange<List<Foo>>(url, method)).isEqualTo(entityList)
		verify { template.exchange(url, method, null, responseType) }
	}

	@Test
	fun `exchange with reified type parameters, String and HttpEntity`() {
		val entity = mockk<RequestEntity<Foo>>()
		val entityList = mockk<ResponseEntity<List<Foo>>>()
		val responseType = object : ParameterizedTypeReference<List<Foo>>() {}
		every { template.exchange(entity, responseType) } returns entityList
		assertThat(template.exchange<List<Foo>>(entity)).isEqualTo(entityList)
		verify { template.exchange(entity, responseType) }
	}

	@Test
	fun `RestOperations are available`() {
		val extensions = Class.forName("org.springframework.web.client.RestOperationsExtensionsKt")
		ReflectionUtils.doWithMethods(RestOperations::class.java) { method ->
			arrayOf(ParameterizedTypeReference::class, Class::class).forEach { kClass ->
				if (method.parameterTypes.contains(kClass.java)) {
					val parameters = mutableListOf<Class<*>>(RestOperations::class.java).apply { addAll(method.parameterTypes.filter { it !=  kClass.java }) }
					val f = extensions.getDeclaredMethod(method.name, *parameters.toTypedArray()).kotlinFunction!!
					assertThat(f.typeParameters.size).isEqualTo(1)
					assertThat(f.typeParameters[0].upperBounds).isEqualTo(listOf(Any::class.createType(nullable = true)))
				}
			}
		}
	}

	class Foo

}
