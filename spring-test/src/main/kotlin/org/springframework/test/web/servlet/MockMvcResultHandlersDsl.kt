/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
