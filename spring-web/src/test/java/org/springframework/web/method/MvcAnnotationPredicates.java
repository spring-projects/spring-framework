/*
 * Copyright 2002-2017 the original author or authors.
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
package org.springframework.web.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Predicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ValueConstants;

/**
 * Predicates for {@code @MVC} annotations.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 *
 * @see ResolvableMethod#annot(Predicate[])
 * @see ResolvableMethod.Builder#annot(Predicate[])
 */
public class MvcAnnotationPredicates {


	// Method parameter predicates

	public static ModelAttributePredicate modelAttribute() {
		return new ModelAttributePredicate();
	}

	public static RequestBodyPredicate requestBody() {
		return new RequestBodyPredicate();
	}

	public static RequestParamPredicate requestParam() {
		return new RequestParamPredicate();
	}

	public static RequestPartPredicate requestPart() {
		return new RequestPartPredicate();
	}

	public static RequestAttributePredicate requestAttribute() {
		return new RequestAttributePredicate();
	}

	public static MatrixVariablePredicate matrixAttribute() {
		return new MatrixVariablePredicate();
	}

	// Method predicates

	public static ModelAttributeMethodPredicate modelMethod() {
		return new ModelAttributeMethodPredicate();
	}

	public static ResponseStatusPredicate responseStatus() {
		return new ResponseStatusPredicate();
	}

	public static ResponseStatusPredicate responseStatus(HttpStatus code) {
		return new ResponseStatusPredicate(code);
	}

	public static RequestMappingPredicate requestMapping(String... path) {
		return new RequestMappingPredicate(path);
	}

	public static RequestMappingPredicate getMapping(String... path) {
		return new RequestMappingPredicate(path).method(RequestMethod.GET);
	}

	public static RequestMappingPredicate postMapping(String... path) {
		return new RequestMappingPredicate(path).method(RequestMethod.POST);
	}

	public static RequestMappingPredicate putMapping(String... path) {
		return new RequestMappingPredicate(path).method(RequestMethod.PUT);
	}

	public static RequestMappingPredicate deleteMapping(String... path) {
		return new RequestMappingPredicate(path).method(RequestMethod.DELETE);
	}

	public static RequestMappingPredicate optionsMapping(String... path) {
		return new RequestMappingPredicate(path).method(RequestMethod.OPTIONS);
	}

	public static RequestMappingPredicate headMapping(String... path) {
		return new RequestMappingPredicate(path).method(RequestMethod.HEAD);
	}



	public static class ModelAttributePredicate implements Predicate<MethodParameter> {

		private String name;

		private boolean binding = true;


		public ModelAttributePredicate name(String name) {
			this.name = name;
			return this;
		}

		public ModelAttributePredicate noName() {
			this.name = "";
			return this;
		}

		public ModelAttributePredicate noBinding() {
			this.binding = false;
			return this;
		}


		@Override
		public boolean test(MethodParameter parameter) {
			ModelAttribute annotation = parameter.getParameterAnnotation(ModelAttribute.class);
			return annotation != null &&
					(this.name == null || annotation.name().equals(this.name)) &&
					annotation.binding() == this.binding;
		}
	}

	public static class RequestBodyPredicate implements Predicate<MethodParameter> {

		private boolean required = true;


		public RequestBodyPredicate notRequired() {
			this.required = false;
			return this;
		}


		@Override
		public boolean test(MethodParameter parameter) {
			RequestBody annotation = parameter.getParameterAnnotation(RequestBody.class);
			return annotation != null && annotation.required() == this.required;
		}
	}

	public static class RequestParamPredicate implements Predicate<MethodParameter> {

		private String name;

		private boolean required = true;

		private String defaultValue = ValueConstants.DEFAULT_NONE;



		public RequestParamPredicate name(String name) {
			this.name = name;
			return this;
		}

		public RequestParamPredicate noName() {
			this.name = "";
			return this;
		}

		public RequestParamPredicate notRequired() {
			this.required = false;
			return this;
		}

		public RequestParamPredicate notRequired(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}


		@Override
		public boolean test(MethodParameter parameter) {
			RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
			return annotation != null &&
					(this.name == null || annotation.name().equals(this.name)) &&
					annotation.required() == this.required &&
					annotation.defaultValue().equals(this.defaultValue);
		}
	}


	public static class RequestPartPredicate implements Predicate<MethodParameter> {

		private String name;

		private boolean required = true;


		public RequestPartPredicate name(String name) {
			this.name = name;
			return this;
		}

		public RequestPartPredicate noName() {
			this.name = "";
			return this;
		}

		public RequestPartPredicate notRequired() {
			this.required = false;
			return this;
		}


		@Override
		public boolean test(MethodParameter parameter) {
			RequestPart annotation = parameter.getParameterAnnotation(RequestPart.class);
			return annotation != null &&
					(this.name == null || annotation.name().equals(this.name)) &&
					annotation.required() == this.required;
		}
	}

	public static class ModelAttributeMethodPredicate implements Predicate<Method> {

		private String name;


		public ModelAttributeMethodPredicate name(String name) {
			this.name = name;
			return this;
		}

		public ModelAttributeMethodPredicate noName() {
			this.name = "";
			return this;
		}

		@Override
		public boolean test(Method method) {
			ModelAttribute annot = AnnotatedElementUtils.findMergedAnnotation(method, ModelAttribute.class);
			return annot != null && (this.name == null || annot.name().equals(this.name));
		}
	}

	public static class RequestAttributePredicate implements Predicate<MethodParameter> {

		private String name;

		private boolean required = true;


		public RequestAttributePredicate name(String name) {
			this.name = name;
			return this;
		}

		public RequestAttributePredicate noName() {
			this.name = "";
			return this;
		}

		public RequestAttributePredicate notRequired() {
			this.required = false;
			return this;
		}


		@Override
		public boolean test(MethodParameter parameter) {
			RequestAttribute annotation = parameter.getParameterAnnotation(RequestAttribute.class);
			return annotation != null &&
					(this.name == null || annotation.name().equals(this.name)) &&
					annotation.required() == this.required;
		}
	}

	public static class ResponseStatusPredicate implements Predicate<Method> {

		private HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;


		private ResponseStatusPredicate() {
		}

		private ResponseStatusPredicate(HttpStatus code) {
			this.code = code;
		}

		@Override
		public boolean test(Method method) {
			ResponseStatus annot = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
			return annot != null && annot.code().equals(this.code);
		}
	}

	public static class RequestMappingPredicate implements Predicate<Method> {

		private String[] path;

		private RequestMethod[] method = {};

		private String[] params;


		private RequestMappingPredicate(String... path) {
			this.path = path;
		}


		public RequestMappingPredicate method(RequestMethod... methods) {
			this.method = methods;
			return this;
		}

		public RequestMappingPredicate params(String... params) {
			this.params = params;
			return this;
		}

		@Override
		public boolean test(Method method) {
			RequestMapping annot = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			return annot != null &&
					Arrays.equals(this.path, annot.path()) &&
					Arrays.equals(this.method, annot.method()) &&
					(this.params == null || Arrays.equals(this.params, annot.params()));
		}
	}

	public static class MatrixVariablePredicate implements Predicate<MethodParameter> {

		private String name;

		private String pathVar;


		public MatrixVariablePredicate name(String name) {
			this.name = name;
			return this;
		}

		public MatrixVariablePredicate noName() {
			this.name = "";
			return this;
		}

		public MatrixVariablePredicate pathVar(String name) {
			this.pathVar = name;
			return this;
		}

		public MatrixVariablePredicate noPathVar() {
			this.pathVar = ValueConstants.DEFAULT_NONE;
			return this;
		}

		@Override
		public boolean test(MethodParameter parameter) {
			MatrixVariable annotation = parameter.getParameterAnnotation(MatrixVariable.class);
			return annotation != null &&
					(this.name == null || this.name.equalsIgnoreCase(annotation.name())) &&
					(this.pathVar == null || this.pathVar.equalsIgnoreCase(annotation.pathVar()));
		}
	}

}
