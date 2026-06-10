package org.springframework.docs.web.webmvc.mvccontroller.mvcannvalidation;

import org.jspecify.annotations.Nullable;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

public class HandlerMethodValidationExceptionVisitor {

	static void main() {
		// tag::snippet[]
		HandlerMethodValidationException ex = /**/ new HandlerMethodValidationException(null);

		ex.visitResults(new HandlerMethodValidationException.Visitor() {

			@Override
			public void requestHeader(RequestHeader requestHeader, ParameterValidationResult result) {
				// ...
			}

			@Override
			public void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result) {
				// ...
			}

			@Override
			public void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors) {
				// ...
			}

			// @fold:on // ...
			@Override
			public void requestPart(RequestPart requestPart, ParameterErrors errors) {

			}

			@Override
			public void cookieValue(CookieValue cookieValue, ParameterValidationResult result) {

			}

			@Override
			public void matrixVariable(MatrixVariable matrixVariable, ParameterValidationResult result) {

			}

			@Override
			public void pathVariable(PathVariable pathVariable, ParameterValidationResult result) {

			}

			@Override
			public void requestBody(RequestBody requestBody, ParameterErrors errors) {

			}
			// @fold:off

			@Override
			public void other(ParameterValidationResult result) {
				// ...
			}
		});
		// end::snippet[]
	}

}
