package org.springframework.docs.web.webmvc.mvccontroller.mvcannvalidation

import org.springframework.validation.method.ParameterErrors
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.HandlerMethodValidationException

class HandlerMethodValidationExceptionVisitor {

	fun main() {
		// tag::snippet[]
		val ex: HandlerMethodValidationException =  /**/ HandlerMethodValidationException(null)

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
}