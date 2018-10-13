/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.env

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

/**
 * Mock object based tests for PropertyResolver Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
@Suppress("UNUSED_VARIABLE")
class PropertyResolverExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var propertyResolver: PropertyResolver

	@Test
	fun `get operator`() {
		val name = propertyResolver["name"] ?: "foo"
		Mockito.verify(propertyResolver, Mockito.times(1)).getProperty("name")
	}

	@Test
	fun `getProperty extension`() {
		whenever(propertyResolver.getProperty(any(), eq(String::class.java))).thenReturn("foo")
		val name = propertyResolver.getProperty<String>("name") ?: "foo"
		Mockito.verify(propertyResolver, Mockito.times(1)).getProperty("name", String::class.java)
	}

	@Test
	fun `getRequiredProperty extension`() {
		whenever(propertyResolver.getRequiredProperty(any(), eq(String::class.java))).thenReturn("foo")
		val name = propertyResolver.getRequiredProperty<String>("name")
		Mockito.verify(propertyResolver, Mockito.times(1)).getRequiredProperty("name", String::class.java)
	}

}
