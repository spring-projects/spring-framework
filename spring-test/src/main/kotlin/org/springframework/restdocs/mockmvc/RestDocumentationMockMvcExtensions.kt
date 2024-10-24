package org.springframework.restdocs.mockmvc

import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMultipartHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.net.URI

private const val ATTRIBUTE_NAME_URL_TEMPLATE = "org.springframework.restdocs.urlTemplate"

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.get
 * @see org.springframework.test.web.servlet.get
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.get(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.get(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL.
 *
 * @see MockMvcRequestBuilders.get
 * @see org.springframework.test.web.servlet.get
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.get(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.get(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.post
 * @see org.springframework.test.web.servlet.post
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.post(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.post(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.post
 * @see org.springframework.test.web.servlet.post
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.post(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.post(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.put
 * @see org.springframework.test.web.servlet.put
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.put(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.put(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.put
 * @see org.springframework.test.web.servlet.put
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.put(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.put(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.patch
 * @see org.springframework.test.web.servlet.patch
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.patch(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.patch(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.patch
 * @see org.springframework.test.web.servlet.patch
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.patch(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.patch(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.delete
 * @see org.springframework.test.web.servlet.delete
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.delete(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.delete(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.delete
 * @see org.springframework.test.web.servlet.delete
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.delete(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.delete(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.options
 * @see org.springframework.test.web.servlet.options
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.options(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.options(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.options
 * @see org.springframework.test.web.servlet.options
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.options(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.options(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.head
 * @see org.springframework.test.web.servlet.head
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.head(urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.head(urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.head
 * @see org.springframework.test.web.servlet.head
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.head(uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.head(uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.request
 * @see org.springframework.test.web.servlet.request
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.request(method: HttpMethod, urlTemplate: String, vararg vars: Any?, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.request(method, urlTemplate, *vars)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.request
 * @see org.springframework.test.web.servlet.request
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.request(method: HttpMethod, uri: URI, dsl: MockHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.request(method, uri)
	return MockHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.multipart
 * @see org.springframework.test.web.servlet.multipart
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.multipart(urlTemplate: String, vararg vars: Any?, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(urlTemplate, *vars)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 * The url template will be captured and made available for documentation.
 *
 * @see MockMvcRequestBuilders.multipart
 * @see org.springframework.test.web.servlet.multipart
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.multipart(httpMethod: HttpMethod, urlTemplate: String, vararg vars: Any?, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(httpMethod, urlTemplate, *vars)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl)
		.apply { requestAttr(ATTRIBUTE_NAME_URL_TEMPLATE, urlTemplate) }
		.perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.multipart
 * @see org.springframework.test.web.servlet.multipart
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.multipart(uri: URI, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(uri)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}

/**
 * [MockMvc] extension providing access to [MockHttpServletRequestDsl] Kotlin DSL and support RestDocs.
 *
 * @see MockMvcRequestBuilders.multipart
 * @see org.springframework.test.web.servlet.multipart
 * @author He Jow Moon
 * @since 6.1.14
 */
fun MockMvc.multipart(httpMethod: HttpMethod, uri: URI, dsl: MockMultipartHttpServletRequestDsl.() -> Unit = {}): ResultActionsDsl {
	val requestBuilder = MockMvcRequestBuilders.multipart(httpMethod, uri)
	return MockMultipartHttpServletRequestDsl(requestBuilder).apply(dsl).perform(this)
}
