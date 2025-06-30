/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet

import org.springframework.test.web.servlet.result.*
import java.io.OutputStream
import java.io.Writer

/**
 * Provide a [MockMvcResultHandlers] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
class MockMvcResultHandlersDsl(private val actions: ResultActions) {

	/**
	 * @see MockMvcResultHandlers.print
	 */
	fun print() {
		actions.andDo(MockMvcResultHandlers.print())
	}

	/**
	 * @see MockMvcResultHandlers.print
	 */
	fun print(stream: OutputStream) {
		actions.andDo(MockMvcResultHandlers.print(stream))
	}

	/**
	 * @see MockMvcResultHandlers.print
	 */
	fun print(writer: Writer) {
		actions.andDo(MockMvcResultHandlers.print(writer))
	}

	/**
	 * @see MockMvcResultHandlers.log
	 */
	fun log() {
		actions.andDo(MockMvcResultHandlers.log())
	}

	/**
	 * @see ResultActions.andDo
	 */
	fun handle(resultHandler: ResultHandler) {
		actions.andDo(resultHandler)
	}
}
