/*
 * Copyright 2002-2018 the original author or authors.
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
