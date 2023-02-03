package org.springframework.test.web.servlet

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

/**
 * Provide a [ResultActions] Kotlin DSL in order to be able to write idiomatic Kotlin code.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
class ResultActionsDsl internal constructor (private val actions: ResultActions, private val mockMvc: MockMvc) {

	/**
	 * Provide access to [MockMvcResultMatchersDsl] Kotlin DSL.
	 * @see MockMvcResultMatchersDsl.match
	 */
	fun andExpect(dsl: MockMvcResultMatchersDsl.() -> Unit): ResultActionsDsl {
		MockMvcResultMatchersDsl(actions).dsl()
		return this
	}


	/**
	 * Provide access to [MockMvcResultMatchersDsl] Kotlin DSL.
	 * @since 6.0.4
	 * @see MockMvcResultMatchersDsl.matchAll
	 */
	fun andExpectAll(dsl: MockMvcResultMatchersDsl.() -> Unit): ResultActionsDsl {
		val softMatchers = mutableListOf<ResultMatcher>()
		val softActions = object : ResultActions {
			override fun andExpect(matcher: ResultMatcher): ResultActions {
				softMatchers.add(matcher)
				return this
			}

			override fun andDo(handler: ResultHandler): ResultActions {
				throw UnsupportedOperationException("andDo should not be part of andExpectAll DSL calls")
			}

			override fun andReturn(): MvcResult {
				throw UnsupportedOperationException("andReturn should not be part of andExpectAll DSL calls")
			}

		}
		// the use of softActions as the matchers DSL actions parameter will store ResultMatchers in list
		MockMvcResultMatchersDsl(softActions).dsl()
		actions.andExpectAll(*softMatchers.toTypedArray())
		return this;
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
	 * Enable asynchronous dispatching.
	 * @see MockMvcRequestBuilders.asyncDispatch
	 * @since 5.2.2
	 */
	fun asyncDispatch(): ResultActionsDsl {
		return andExpect {
			request { asyncStarted() }
		}.andReturn().let {
			ResultActionsDsl(mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(it)), mockMvc)
		}
	}

	/**
	 * @see ResultActions.andReturn
	 */
	fun andReturn() = actions.andReturn()
}
