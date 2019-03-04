package org.springframework.test.web.servlet

/**
 * Provide a [ResultActions] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
class ResultActionsDsl(private val actions: ResultActions) {

	/**
	 * Provide access to [MockMvcResultMatchersDsl] Kotlin DSL.
	 * @see MockMvcResultMatchersDsl.match
	 */
	fun andExpect(dsl: MockMvcResultMatchersDsl.() -> Unit): ResultActionsDsl {
		MockMvcResultMatchersDsl(actions).dsl()
		return this
	}

	/**
	 * Provide access to [MockMvcResultHandlersDsl] Kotlin DSL.
	 * @see MockMvcResultHandlersDsl.handle
	 */
	fun andDo(dsl: MockMvcResultHandlersDsl.() -> Unit): ResultActionsDsl {
		MockMvcResultHandlersDsl(actions).dsl()
		return this
	}

	/**
	 * @see ResultActions.andReturn
	 */
	fun andReturn() = actions.andReturn()
}
