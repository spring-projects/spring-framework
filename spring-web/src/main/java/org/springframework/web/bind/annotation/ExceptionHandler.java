/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for handling exceptions in specific handler classes and/or
 * handler methods.
 *
 * <p>Handler methods which are annotated with this annotation are allowed to
 * have very flexible signatures. They may have parameters of the following
 * types, in arbitrary order:
 * <ul>
 * <li>An exception argument: declared as a general Exception or as a more
 * specific exception. This also serves as a mapping hint if the annotation
 * itself does not narrow the exception types through its {@link #value()}.
 * You may refer to a top-level exception being propagated or to a nested
 * cause within a wrapper exception. As of 5.3, any cause level is being
 * exposed, whereas previously only an immediate cause was considered.
 * <li>Request and/or response objects (typically from the Servlet API).
 * You may choose any specific request/response type, e.g.
 * {@link javax.servlet.ServletRequest} / {@link javax.servlet.http.HttpServletRequest}.
 * <li>Session object: typically {@link javax.servlet.http.HttpSession}.
 * An argument of this type will enforce the presence of a corresponding session.
 * As a consequence, such an argument will never be {@code null}.
 * <i>Note that session access may not be thread-safe, in particular in a
 * Servlet environment: Consider switching the
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession
 * "synchronizeOnSession"} flag to "true" if multiple requests are allowed to
 * access a session concurrently.</i>
 * <li>{@link org.springframework.web.context.request.WebRequest} or
 * {@link org.springframework.web.context.request.NativeWebRequest}.
 * Allows for generic request parameter access as well as request/session
 * attribute access, without ties to the native Servlet API.
 * <li>{@link java.util.Locale} for the current request locale
 * (determined by the most specific locale resolver available,
 * i.e. the configured {@link org.springframework.web.servlet.LocaleResolver}
 * in a Servlet environment).
 * <li>{@link java.io.InputStream} / {@link java.io.Reader} for access
 * to the request's content. This will be the raw InputStream/Reader as
 * exposed by the Servlet API.
 * <li>{@link java.io.OutputStream} / {@link java.io.Writer} for generating
 * the response's content. This will be the raw OutputStream/Writer as
 * exposed by the Servlet API.
 * <li>{@link org.springframework.ui.Model} as an alternative to returning
 * a model map from the handler method. Note that the provided model is not
 * pre-populated with regular model attributes and therefore always empty,
 * as a convenience for preparing the model for an exception-specific view.
 * </ul>
 *
 * <p>The following return types are supported for handler methods:
 * <ul>
 * <li>A {@code ModelAndView} object (from Servlet MVC).
 * <li>A {@link org.springframework.ui.Model} object, with the view name implicitly
 * determined through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}.
 * <li>A {@link java.util.Map} object for exposing a model,
 * with the view name implicitly determined through a
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}.
 * <li>A {@link org.springframework.web.servlet.View} object.
 * <li>A {@link String} value which is interpreted as view name.
 * <li>{@link ResponseBody @ResponseBody} annotated methods (Servlet-only)
 * to set the response content. The return value will be converted to the
 * response stream using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}.
 * <li>An {@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;} or
 * {@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;} object
 * (Servlet-only) to set response headers and content. The ResponseEntity body
 * will be converted and written to the response stream using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message converters}.
 * <li>{@code void} if the method handles the response itself (by
 * writing the response content directly, declaring an argument of type
 * {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 * for that purpose) or if the view name is supposed to be implicitly determined
 * through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * (not declaring a response argument in the handler method signature).
 * </ul>
 *
 * <p>You may combine the {@code ExceptionHandler} annotation with
 * {@link ResponseStatus @ResponseStatus} for a specific HTTP error status.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see ControllerAdvice
 * @see org.springframework.web.context.request.WebRequest
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {

	/**
	 * Exceptions handled by the annotated method. If empty, will default to any
	 * exceptions listed in the method argument list.
	 */
	Class<? extends Throwable>[] value() default {};

}
