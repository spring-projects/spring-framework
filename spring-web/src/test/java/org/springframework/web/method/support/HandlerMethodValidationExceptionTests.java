/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.method.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.testfixture.method.ResolvableMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HandlerMethodValidationException}.
 *
 * @author Rossen Stoyanchev
 */
class HandlerMethodValidationExceptionTests {

	private static final Person person = new Person("Faustino1234");

	private static final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


	private final HandlerMethod handlerMethod = handlerMethod(new ValidController(),
			controller -> controller.handle(person, person, person, person, "", "", "", "", "", ""));

	private final TestVisitor visitor = new TestVisitor();


	@Test
	void traverse() {

		HandlerMethodValidationException ex =
				new HandlerMethodValidationException(createMethodValidationResult(this.handlerMethod),
						new MvcParamPredicate(ModelAttribute.class),
						new MvcParamPredicate(RequestParam.class));

		ex.visitResults(this.visitor);

		assertThat(this.visitor.getOutput()).isEqualTo("""
				@ModelAttribute: modelAttribute1, @ModelAttribute: modelAttribute2, \
				@RequestBody: requestBody, @RequestPart: requestPart, \
				@RequestParam: requestParam1, @RequestParam: requestParam2, \
				@RequestHeader: header, @PathVariable: pathVariable, \
				@CookieValue: cookie, @MatrixVariable: matrixVariable""");
	}

	@Test
	void traverseRemaining() {

		HandlerMethodValidationException ex =
				new HandlerMethodValidationException(createMethodValidationResult(this.handlerMethod));

		ex.visitResults(this.visitor);

		assertThat(this.visitor.getOutput()).isEqualTo("""
				Other: modelAttribute1, @ModelAttribute: modelAttribute2, \
				@RequestBody: requestBody, @RequestPart: requestPart, \
				Other: requestParam1, @RequestParam: requestParam2, \
				@RequestHeader: header, @PathVariable: pathVariable, \
				@CookieValue: cookie, @MatrixVariable: matrixVariable""");
	}

	@SuppressWarnings("unchecked")
	private static <T> HandlerMethod handlerMethod(T controller, Consumer<T> mockCallConsumer) {
		Method method = ResolvableMethod.on((Class<T>) controller.getClass()).mockCall(mockCallConsumer).method();
		HandlerMethod hm = new HandlerMethod(controller, method);
		for (MethodParameter parameter : hm.getMethodParameters()) {
			parameter.initParameterNameDiscovery(parameterNameDiscoverer);
		}
		return hm;
	}

	private static MethodValidationResult createMethodValidationResult(HandlerMethod handlerMethod) {
		return MethodValidationResult.create(
				handlerMethod.getBean(), handlerMethod.getMethod(),
				Arrays.stream(handlerMethod.getMethodParameters())
						.map(param -> {
							if (param.hasParameterAnnotation(Valid.class)) {
								Errors errors = new BeanPropertyBindingResult(person, param.getParameterName());
								errors.rejectValue("name", "Size.person.name");
								return new ParameterErrors(param, person, errors, null, null, null);
							}
							else {
								MessageSourceResolvable error = new DefaultMessageSourceResolvable("Size");
								return new ParameterValidationResult(
										param, "123", List.of(error), null, null, null, (e, t) -> null);
							}
						})
						.toList());
	}



	@SuppressWarnings("unused")
	private record Person(@Size(min = 1, max = 10) String name) {

		@Override
		public String name() {
			return this.name;
		}
	}


	@SuppressWarnings({"unused", "SameParameterValue", "UnusedReturnValue"})
	@RestController
	static class ValidController {

		void handle(
				@Valid Person modelAttribute1,
				@Valid @ModelAttribute Person modelAttribute2,
				@Valid @RequestBody Person requestBody,
				@Valid @RequestPart Person requestPart,
				@Size(min = 5) String requestParam1,
				@Size(min = 5) @RequestParam String requestParam2,
				@Size(min = 5) @RequestHeader String header,
				@Size(min = 5) @PathVariable String pathVariable,
				@Size(min = 5) @CookieValue String cookie,
				@Size(min = 5) @MatrixVariable String matrixVariable) {
		}

	}


	private record MvcParamPredicate(Class<? extends Annotation> type) implements Predicate<MethodParameter> {

		@Override
		public boolean test(MethodParameter param) {
			return (param.hasParameterAnnotation(this.type) ||
					(isDefaultParameter(param) && !hasMvcAnnotation(param)));
		}

		private boolean isDefaultParameter(MethodParameter param) {
			boolean simpleType = BeanUtils.isSimpleValueType(param.getParameterType());
			return ((this.type.equals(RequestParam.class) && simpleType) ||
					(this.type.equals(ModelAttribute.class) && !simpleType));
		}

		private boolean hasMvcAnnotation(MethodParameter param) {
			return Arrays.stream(param.getParameterAnnotations())
					.map(Annotation::annotationType)
					.anyMatch(type -> type.getPackage().equals(RequestParam.class.getPackage()));
		}
	}


	private static class TestVisitor implements HandlerMethodValidationException.Visitor {

		private final StringJoiner joiner = new StringJoiner(", ");

		public String getOutput() {
			return this.joiner.toString();
		}

		@Override
		public void cookieValue(CookieValue cookieValue, ParameterValidationResult result) {
			handle(cookieValue, result);
		}

		@Override
		public void matrixVariable(MatrixVariable matrixVariable, ParameterValidationResult result) {
			handle(matrixVariable, result);
		}

		@Override
		public void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors) {
			handle("@ModelAttribute", errors);
		}

		@Override
		public void pathVariable(PathVariable pathVariable, ParameterValidationResult result) {
			handle(pathVariable, result);
		}

		@Override
		public void requestBody(RequestBody requestBody, ParameterErrors errors) {
			handle(requestBody, errors);
		}

		@Override
		public void requestHeader(RequestHeader requestHeader, ParameterValidationResult result) {
			handle(requestHeader, result);
		}

		@Override
		public void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result) {
			handle("@RequestParam", result);
		}

		@Override
		public void requestPart(RequestPart requestPart, ParameterErrors errors) {
			handle(requestPart, errors);
		}

		@Override
		public void other(ParameterValidationResult result) {
			handle("Other", result);
		}

		private void handle(Annotation annotation, ParameterValidationResult result) {
			handle("@" + annotation.annotationType().getSimpleName(), result);
		}

		private void handle(String tag, ParameterValidationResult result) {
			this.joiner.add(tag + ": " + result.getMethodParameter().getParameterName());
		}
	}

}
