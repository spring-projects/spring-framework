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

package org.springframework.docs.web.webflux.controller.mvcannvalidation

import org.springframework.context.MessageSourceResolvable
import org.springframework.validation.method.MethodValidationResult
import org.springframework.validation.method.ParameterErrors
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.HandlerMethodValidationException
import java.lang.reflect.Method

class HandlerMethodValidationExceptionVisitor {

	fun main() {
		// tag::snippet[]
		val ex: HandlerMethodValidationException =  /**/ HandlerMethodValidationException(EmptyMethodValidationResult())

		ex.visitResults(object : HandlerMethodValidationException.Visitor {

			override fun requestHeader(requestHeader: RequestHeader, result: ParameterValidationResult) {
				// ...
			}

			override fun requestParam(requestParam: RequestParam?, result: ParameterValidationResult) {
				// ...
			}

			override fun modelAttribute(modelAttribute: ModelAttribute?, errors: ParameterErrors) {
				// ...
			}

			// @fold:on // ...
			override fun requestPart(requestPart: RequestPart, errors: ParameterErrors) {
			}

			override fun cookieValue(cookieValue: CookieValue, result: ParameterValidationResult) {
			}

			override fun matrixVariable(matrixVariable: MatrixVariable, result: ParameterValidationResult) {
			}

			override fun pathVariable(pathVariable: PathVariable, result: ParameterValidationResult) {
			}

			override fun requestBody(requestBody: RequestBody, errors: ParameterErrors) {
			}
			// @fold:off

			override fun other(result: ParameterValidationResult) {
				// ...
			}
		})
		// end::snippet[]
	}

	internal class EmptyMethodValidationResult : MethodValidationResult {
		override fun getTarget(): Any {
			TODO()
		}

		override fun getMethod(): Method {
			TODO()
		}

		override fun isForReturnValue(): Boolean {
			TODO()
		}

		override fun getParameterValidationResults(): List<ParameterValidationResult> {
			TODO()
		}

		override fun getCrossParameterValidationResults(): List<MessageSourceResolvable> {
			TODO()
		}

		override fun toString(): String {
			TODO()
		}
	}
}