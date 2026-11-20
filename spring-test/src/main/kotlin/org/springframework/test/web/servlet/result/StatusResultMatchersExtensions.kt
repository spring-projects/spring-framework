/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.result

import org.hamcrest.Matcher
import org.springframework.test.web.servlet.ResultMatcher

/**
 * Extension for [StatusResultMatchers.is] providing an `isEqualTo` alias since `is` is
 * a reserved keyword in Kotlin.
 *
 * @author Sebastien Deleuze
 * @since 5.0.7
 */
fun StatusResultMatchers.isEqualTo(matcher: Matcher<Int>): ResultMatcher = `is`(matcher)

/**
 * Extension for [StatusResultMatchers.is] providing an `isEqualTo` alias since `is` is
 * a reserved keyword in Kotlin.
 *
 * @author Sebastien Deleuze
 * @since 5.0.7
 */
fun StatusResultMatchers.isEqualTo(status: Int): ResultMatcher = `is`(status)
