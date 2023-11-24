/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.method.MethodValidationResult;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.BindErrorUtils;

/**
 * {@link ResponseStatusException} that is also {@link MethodValidationResult}.
 * Raised by {@link HandlerMethodValidator} in case of method validation errors
 * on a web controller method.
 *
 * <p>The {@link #getStatusCode()} is 400 for input validation errors, and 500
 * for validation errors on a return value.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
@SuppressWarnings("serial")
public class HandlerMethodValidationException extends ResponseStatusException implements MethodValidationResult {

	private final MethodValidationResult validationResult;

	private final Predicate<MethodParameter> modelAttribitePredicate;

	private final Predicate<MethodParameter> requestParamPredicate;


	public HandlerMethodValidationException(MethodValidationResult validationResult) {
		this(validationResult,
				param -> param.hasParameterAnnotation(ModelAttribute.class),
				param -> param.hasParameterAnnotation(RequestParam.class));
	}

	public HandlerMethodValidationException(MethodValidationResult validationResult,
			Predicate<MethodParameter> modelAttribitePredicate, Predicate<MethodParameter> requestParamPredicate) {

		super(initHttpStatus(validationResult), "Validation failure", null, null, null);
		this.validationResult = validationResult;
		this.modelAttribitePredicate = modelAttribitePredicate;
		this.requestParamPredicate = requestParamPredicate;
	}

	private static HttpStatus initHttpStatus(MethodValidationResult validationResult) {
		return (!validationResult.isForReturnValue() ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR);
	}


	@Override
	public Object[] getDetailMessageArguments(MessageSource messageSource, Locale locale) {
		return new Object[] { BindErrorUtils.resolveAndJoin(getAllErrors(), messageSource, locale) };
	}

	@Override
	public Object[] getDetailMessageArguments() {
		return new Object[] { BindErrorUtils.resolveAndJoin(getAllErrors()) };
	}

	@Override
	public Object getTarget() {
		return this.validationResult.getTarget();
	}

	@Override
	public Method getMethod() {
		return this.validationResult.getMethod();
	}

	@Override
	public boolean isForReturnValue() {
		return this.validationResult.isForReturnValue();
	}

	@Override
	public List<ParameterValidationResult> getAllValidationResults() {
		return this.validationResult.getAllValidationResults();
	}

	/**
	 * Provide a {@link Visitor Visitor} to handle {@link ParameterValidationResult}s
	 * through callback methods organized by controller method parameter type.
	 */
	public void visitResults(Visitor visitor) {
		for (ParameterValidationResult result : getAllValidationResults()) {
			MethodParameter param = result.getMethodParameter();
			CookieValue cookieValue = param.getParameterAnnotation(CookieValue.class);
			if (cookieValue != null) {
				visitor.cookieValue(cookieValue, result);
				continue;
			}
			MatrixVariable matrixVariable = param.getParameterAnnotation(MatrixVariable.class);
			if (matrixVariable != null) {
				visitor.matrixVariable(matrixVariable, result);
				continue;
			}
			if (this.modelAttribitePredicate.test(param)) {
				ModelAttribute modelAttribute = param.getParameterAnnotation(ModelAttribute.class);
				visitor.modelAttribute(modelAttribute, asErrors(result));
				continue;
			}
			PathVariable pathVariable = param.getParameterAnnotation(PathVariable.class);
			if (pathVariable != null) {
				visitor.pathVariable(pathVariable, result);
				continue;
			}
			RequestBody requestBody = param.getParameterAnnotation(RequestBody.class);
			if (requestBody != null) {
				visitor.requestBody(requestBody, asErrors(result));
				continue;
			}
			RequestHeader requestHeader = param.getParameterAnnotation(RequestHeader.class);
			if (requestHeader != null) {
				visitor.requestHeader(requestHeader, result);
				continue;
			}
			if (this.requestParamPredicate.test(param)) {
				RequestParam requestParam = param.getParameterAnnotation(RequestParam.class);
				visitor.requestParam(requestParam, result);
				continue;
			}
			RequestPart requestPart = param.getParameterAnnotation(RequestPart.class);
			if (requestPart != null) {
				visitor.requestPart(requestPart, asErrors(result));
				continue;
			}
			visitor.other(result);
		}
	}

	private static ParameterErrors asErrors(ParameterValidationResult result) {
		Assert.state(result instanceof ParameterErrors, "Expected ParameterErrors");
		return (ParameterErrors) result;
	}


	/**
	 * Contract to handle validation results with callbacks by controller method
	 * parameter type, with {@link #other} serving as the fallthrough.
	 */
	public interface Visitor {

		/**
		 * Handle results for {@code @CookieValue} method parameters.
		 * @param cookieValue the annotation declared on the parameter
		 * @param result the validation result
		 */
		void cookieValue(CookieValue cookieValue, ParameterValidationResult result);

		/**
		 * Handle results for {@code @MatrixVariable} method parameters.
		 * @param matrixVariable the annotation declared on the parameter
		 * @param result the validation result
		 */
		void matrixVariable(MatrixVariable matrixVariable, ParameterValidationResult result);

		/**
		 * Handle results for {@code @ModelAttribute} method parameters.
		 * @param modelAttribute the optional {@code ModelAttribute} annotation,
		 * possibly {@code null} if the method parameter is declared without it.
		 * @param errors the validation errors
		 */
		void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors);

		/**
		 * Handle results for {@code @PathVariable} method parameters.
		 * @param pathVariable the annotation declared on the parameter
		 * @param result the validation result
		 */
		void pathVariable(PathVariable pathVariable, ParameterValidationResult result);

		/**
		 * Handle results for {@code @RequestBody} method parameters.
		 * @param requestBody the annotation declared on the parameter
		 * @param errors the validation error
		 */
		void requestBody(RequestBody requestBody, ParameterErrors errors);

		/**
		 * Handle results for {@code @RequestHeader} method parameters.
		 * @param requestHeader the annotation declared on the parameter
		 * @param result the validation result
		 */
		void requestHeader(RequestHeader requestHeader, ParameterValidationResult result);

		/**
		 * Handle results for {@code @RequestParam} method parameters.
		 * @param requestParam the optional {@code RequestParam} annotation,
		 * possibly {@code null} if the method parameter is declared without it.
		 * @param result the validation result
		 */
		void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result);

		/**
		 * Handle results for {@code @RequestPart} method parameters.
		 * @param requestPart the annotation declared on the parameter
		 * @param errors the validation errors
		 */
		void requestPart(RequestPart requestPart, ParameterErrors errors);

		/**
		 * Handle other results that aren't any of the above.
		 * @param result the validation result
		 */
		void other(ParameterValidationResult result);

	}

}
