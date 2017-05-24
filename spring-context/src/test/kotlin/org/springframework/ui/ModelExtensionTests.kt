/*
 * Copyright 2002-2017 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.ui.ModelExtension.set


class ModelExtensionTests {

	@Test
	fun setAttribute() {
		val model:Model = ConcurrentModel()
		model["foo"] = "bing"
		assertTrue(model.containsAttribute("foo"))
		assertEquals("bing", model.asMap()["foo"])
	}
}
