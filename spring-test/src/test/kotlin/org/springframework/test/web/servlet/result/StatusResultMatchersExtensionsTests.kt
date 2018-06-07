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

package org.springframework.test.web.servlet.result

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StatusResultMatchersExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var matchers: StatusResultMatchers

	@Test
	fun `StatusResultMatchers#is with Matcher parameter is called as expected when using isEqualTo extension`() {
		val matcher = mock<Matcher<Int>>()
		matchers.isEqualTo(matcher)
		verify(matchers, times(1)).`is`(matcher)
	}

	@Test
	fun `StatusResultMatchers#is with int parameter is called as expected when using isEqualTo extension`() {
		matchers.isEqualTo(200)
		verify(matchers, times(1)).`is`(200)
	}

}
