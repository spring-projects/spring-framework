package org.springframework.web.client

import com.nhaarman.mockito_kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import java.net.URI

/**
 * Mock object based tests for [RestOperations] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class RestOperationsExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var template: RestOperations

	@Test
	fun `getForObject with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val var1 = "var1"
		val var2 = "var2"
		template.getForObject<Foo>(url, var1, var2)
		verify(template, times(1)).getForObject(url, Foo::class.java, var1, var2)
	}

	@Test
	fun `getForObject with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.getForObject<Foo>(url, vars)
		verify(template, times(1)).getForObject(url, Foo::class.java, vars)
	}

	@Test
	fun `getForObject with reified type parameters and URI`() {
		val url = URI("https://spring.io")
		template.getForObject<Foo>(url)
		verify(template, times(1)).getForObject(url, Foo::class.java)
	}

	@Test
	fun `getForEntity with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val var1 = "var1"
		val var2 = "var2"
		template.getForEntity<Foo>(url, var1, var2)
		verify(template, times(1)).getForEntity(url, Foo::class.java, var1, var2)
	}

	@Test
	fun `postForObject with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		template.postForObject<Foo>(url, body, var1, var2)
		verify(template, times(1)).postForObject(url, body, Foo::class.java, var1, var2)
	}

	@Test
	fun `postForObject with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.postForObject<Foo>(url, body, vars)
		verify(template, times(1)).postForObject(url, body, Foo::class.java, vars)
	}

	@Test
	fun `postForObject with reified type parameters`() {
		val url = "https://spring.io"
		val body: Any = "body"
		template.postForObject<Foo>(url, body)
		verify(template, times(1)).postForObject(url, body, Foo::class.java)
	}

	@Test
	fun `postForEntity with reified type parameters, String and varargs`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val var1 = "var1"
		val var2 = "var2"
		template.postForEntity<Foo>(url, body, var1, var2)
		verify(template, times(1)).postForEntity(url, body, Foo::class.java, var1, var2)
	}

	@Test
	fun `postForEntity with reified type parameters, String and Map`() {
		val url = "https://spring.io"
		val body: Any = "body"
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.postForEntity<Foo>(url, body, vars)
		verify(template, times(1)).postForEntity(url, body, Foo::class.java, vars)
	}

	@Test
	fun `postForEntity with reified type parameters`() {
		val url = "https://spring.io"
		val body: Any = "body"
		template.postForEntity<Foo>(url, body)
		verify(template, times(1)).postForEntity(url, body, Foo::class.java)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity and varargs`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mock<HttpEntity<Foo>>()
		val var1 = "var1"
		val var2 = "var2"
		template.exchange<Foo>(url, method, entity, var1, var2)
		verify(template, times(1)).exchange(url, method, entity, Foo::class.java, var1, var2)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity and Map`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mock<HttpEntity<Foo>>()
		val vars = mapOf(Pair("key1", "value1"), Pair("key2", "value2"))
		template.exchange<Foo>(url, method, entity, vars)
		verify(template, times(1)).exchange(url, method, entity, Foo::class.java, vars)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpMethod, HttpEntity`() {
		val url = "https://spring.io"
		val method = HttpMethod.GET
		val entity = mock<HttpEntity<Foo>>()
		template.exchange<Foo>(url, method, entity)
		verify(template, times(1)).exchange(url, method, entity, Foo::class.java)
	}

	@Test
	fun `exchange with reified type parameters, String, HttpEntity`() {
		val entity = mock<RequestEntity<Foo>>()
		template.exchange<Foo>(entity)
		verify(template, times(1)).exchange(entity, Foo::class.java)
	}

	class Foo

}
