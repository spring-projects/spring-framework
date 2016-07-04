/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.Callable;

import org.springframework.core.annotation.AliasFor;

/**
 * Annotation for mapping web requests onto specific handler classes and/or
 * handler methods.
 *
 * <p>Handler methods which are annotated with this annotation are allowed to
 * have very flexible signatures. They may have parameters of the following
 * types, in arbitrary order (except for validation results, which need to
 * follow right after the corresponding command object, if desired):
 * <ul>
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
 * <li>{@link org.springframework.http.HttpMethod} for the HTTP request method</li>
 * <li>{@link PathVariable @PathVariable} annotated parameters (Servlet-only)
 * for access to URI template values (i.e. /hotels/{hotel}). Variable values will be
 * converted to the declared method argument type. By default, the URI template
 * will match against the regular expression {@code [^\.]*} (i.e. any character
 * other than period), but this can be changed by specifying another regular
 * expression, like so: /hotels/{hotel:\d+}.
 * Additionally, {@code @PathVariable} can be used on a
 * {@link java.util.Map Map&lt;String, String&gt;} to gain access to all
 * URI template variables.
 * <li>{@link MatrixVariable @MatrixVariable} annotated parameters (Servlet-only)
 * for access to name-value pairs located in URI path segments. Matrix variables
 * must be represented with a URI template variable. For example /hotels/{hotel}
 * where the incoming URL may be "/hotels/42;q=1".
 * Additionally, {@code @MatrixVariable} can be used on a
 * {@link java.util.Map Map&lt;String, String&gt;} to gain access to all
 * matrix variables in the URL or to those in a specific path variable.
 * <li>{@link RequestParam @RequestParam} annotated parameters for access to
 * specific Servlet request parameters. Parameter values will be
 * converted to the declared method argument type. Additionally,
 * {@code @RequestParam} can be used on a {@link java.util.Map Map&lt;String, String&gt;} or
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * method parameter to gain access to all request parameters.
 * <li>{@link RequestHeader @RequestHeader} annotated parameters for access to
 * specific Servlet request HTTP headers. Parameter values will be
 * converted to the declared method argument type. Additionally,
 * {@code @RequestHeader} can be used on a {@link java.util.Map Map&lt;String, String&gt;},
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}, or
 * {@link org.springframework.http.HttpHeaders HttpHeaders} method parameter to
 * gain access to all request headers.
 * <li>{@link RequestBody @RequestBody} annotated parameters (Servlet-only)
 * for access to the Servlet request HTTP contents. The request stream will be
 * converted to the declared method argument type using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message
 * converters}. Such parameters may optionally be annotated with {@code @Valid}
 * and also support access to validation results through an
 * {@link org.springframework.validation.Errors} argument.
 * Instead a {@link org.springframework.web.bind.MethodArgumentNotValidException}
 * exception is raised.
 * <li>{@link RequestPart @RequestPart} annotated parameters
 * (Servlet-only, {@literal @MVC 3.1-only})
 * for access to the content
 * of a part of "multipart/form-data" request. The request part stream will be
 * converted to the declared method argument type using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message
 * converters}. Such parameters may optionally be annotated with {@code @Valid}
 * and support access to validation results through a
 * {@link org.springframework.validation.Errors} argument.
 * Instead a {@link org.springframework.web.bind.MethodArgumentNotValidException}
 * exception is raised.
 * <li>{@link SessionAttribute @SessionAttribute} annotated parameters for access
 * to existing, permanent session attributes (e.g. user authentication object)
 * as opposed to model attributes temporarily stored in the session as part of
 * a controller workflow via {@link SessionAttributes}.
 * <li>{@link RequestAttribute @RequestAttribute} annotated parameters for access
 * to request attributes.
 * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;} parameters
 * (Servlet-only) for access to the Servlet request HTTP headers and contents.
 * The request stream will be converted to the entity body using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message
 * converters}.
 * <li>{@link java.util.Map} / {@link org.springframework.ui.Model} /
 * {@link org.springframework.ui.ModelMap} for enriching the implicit model
 * that will be exposed to the web view.
 * <li>{@link org.springframework.web.servlet.mvc.support.RedirectAttributes}
 * (Servlet-only, {@literal @MVC 3.1-only}) to specify the exact set of attributes
 * to use in case of a redirect and also to add flash attributes (attributes
 * stored temporarily on the server-side to make them available to the request
 * after the redirect). {@code RedirectAttributes} is used instead of the
 * implicit model if the method returns a "redirect:" prefixed view name or
 * {@code RedirectView}.
 * <li>Command/form objects to bind parameters to: as bean properties or fields,
 * with customizable type conversion, depending on {@link InitBinder} methods
 * and/or the HandlerAdapter configuration - see the "webBindingInitializer"
 * property on RequestMappingHandlerMethodAdapter.
 * Such command objects along with their validation results will be exposed
 * as model attributes, by default using the non-qualified command class name
 * in property notation (e.g. "orderAddress" for type "mypackage.OrderAddress").
 * Specify a parameter-level {@link ModelAttribute @ModelAttribute} annotation for
 * declaring a specific model attribute name.
 * <li>{@link org.springframework.validation.Errors} /
 * {@link org.springframework.validation.BindingResult} validation results
 * for a preceding command/form object (the immediate preceding argument).
 * <li>{@link org.springframework.web.bind.support.SessionStatus} status handle
 * for marking form processing as complete (triggering the cleanup of session
 * attributes that have been indicated by the {@link SessionAttributes @SessionAttributes}
 * annotation at the handler type level).
 * <li>{@link org.springframework.web.util.UriComponentsBuilder}
 * (Servlet-only, {@literal @MVC 3.1-only})
 * for preparing a URL relative to the current request's host, port, scheme,
 * context path, and the literal part of the servlet mapping.
 * </ul>
 *
 * <p><strong>Note:</strong> Java 8's {@code java.util.Optional} is supported
 * as a method parameter type with annotations that provide a {@code required}
 * attribute (e.g. {@code @RequestParam}, {@code @RequestHeader}, etc.). The use
 * of {@code java.util.Optional} in those cases is equivalent to having
 * {@code required=false}.
 *
 * <p>The following return types are supported for handler methods:
 * <ul>
 * <li>A {@code ModelAndView} object (from Servlet MVC),
 * with the model implicitly enriched with command objects and the results
 * of {@link ModelAttribute @ModelAttribute} annotated reference data accessor methods.
 * <li>A {@link org.springframework.ui.Model Model} object, with the view name implicitly
 * determined through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * and the model implicitly enriched with command objects and the results
 * of {@link ModelAttribute @ModelAttribute} annotated reference data accessor methods.
 * <li>A {@link java.util.Map} object for exposing a model,
 * with the view name implicitly determined through a
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * and the model implicitly enriched with command objects and the results
 * of {@link ModelAttribute @ModelAttribute} annotated reference data accessor methods.
 * <li>A {@link org.springframework.web.servlet.View} object, with the
 * model implicitly determined through command objects and
 * {@link ModelAttribute @ModelAttribute} annotated reference data accessor methods.
 * The handler method may also programmatically enrich the model by
 * declaring a {@link org.springframework.ui.Model} argument (see above).
 * <li>A {@link String} value which is interpreted as view name,
 * with the model implicitly determined through command objects and
 * {@link ModelAttribute @ModelAttribute} annotated reference data accessor methods.
 * The handler method may also programmatically enrich the model by
 * declaring a {@link org.springframework.ui.ModelMap} argument
 * (see above).
 * <li>{@link ResponseBody @ResponseBody} annotated methods (Servlet-only)
 * for access to the Servlet response HTTP contents. The return value will
 * be converted to the response stream using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message
 * converters}.
 * <li>An {@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;} or
 * {@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;} object
 * (Servlet-only) to access to the Servlet response HTTP headers and contents.
 * The entity body will be converted to the response stream using
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message
 * converters}.
 * <li>An {@link org.springframework.http.HttpHeaders HttpHeaders} object to
 * return a response with no body.</li>
 * <li>A {@link Callable} which is used by Spring MVC to obtain the return
 * value asynchronously in a separate thread transparently managed by Spring MVC
 * on behalf of the application.
 * <li>A {@link org.springframework.web.context.request.async.DeferredResult}
 * which the application uses to produce a return value in a separate
 * thread of its own choosing, as an alternative to returning a Callable.
 * <li>A {@link org.springframework.util.concurrent.ListenableFuture}
 * which the application uses to produce a return value in a separate
 * thread of its own choosing, as an alternative to returning a Callable.
 * <li>A {@link java.util.concurrent.CompletionStage} (implemented by
 * {@link java.util.concurrent.CompletableFuture} for example)
 * which the application uses to produce a return value in a separate
 * thread of its own choosing, as an alternative to returning a Callable.
 * <li>A {@link org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter}
 * can be used to write multiple objects to the response asynchronously;
 * also supported as the body within {@code ResponseEntity}.</li>
 * <li>An {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter}
 * can be used to write Server-Sent Events to the response asynchronously;
 * also supported as the body within {@code ResponseEntity}.</li>
 * <li>A {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody}
 * can be used to write to the response asynchronously;
 * also supported as the body within {@code ResponseEntity}.</li>
 * <li>{@code void} if the method handles the response itself (by
 * writing the response content directly, declaring an argument of type
 * {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 * for that purpose) or if the view name is supposed to be implicitly determined
 * through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * (not declaring a response argument in the handler method signature).
 * <li>Any other return type will be considered as single model attribute
 * to be exposed to the view, using the attribute name specified through
 * {@link ModelAttribute @ModelAttribute} at the method level (or the default attribute
 * name based on the return type's class name otherwise). The model will be
 * implicitly enriched with command objects and the results of
 * {@link ModelAttribute @ModelAttribute} annotated reference data accessor methods.
 * </ul>
 *
 * <p><b>NOTE:</b> {@code @RequestMapping} will only be processed if an
 * an appropriate {@code HandlerMapping}-{@code HandlerAdapter} pair
 * is configured. If you are defining custom {@code HandlerMappings} or
 * {@code HandlerAdapters}, then you need to add
 * {@code DefaultAnnotationHandlerMapping} and
 * {@code AnnotationMethodHandlerAdapter} to your configuration.</code>.
 *
 * <p><b>NOTE:</b> Spring 3.1 introduced a new set of support classes for
 * {@code @RequestMapping} methods in Servlet environments called
 * {@code RequestMappingHandlerMapping} and
 * {@code RequestMappingHandlerAdapter}. They are recommended for use and
 * even required to take advantage of new features in Spring MVC 3.1 (search
 * {@literal "@MVC 3.1-only"} in this source file) and going forward.
 * The new support classes are enabled by default from the MVC namespace and
 * with use of the MVC Java config ({@code @EnableWebMvc}) but must be
 * configured explicitly if using neither.
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @RequestMapping} and {@code @SessionAttributes} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see PatchMapping
 * @see RequestParam
 * @see RequestAttribute
 * @see PathVariable
 * @see ModelAttribute
 * @see SessionAttribute
 * @see SessionAttributes
 * @see InitBinder
 * @see org.springframework.web.context.request.WebRequest
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * Assign a name to this mapping.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used on both levels, a combined name is derived by concatenation
	 * with "#" as separator.
	 * @see org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder
	 * @see org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 */
	String name() default "";

	/**
	 * The primary mapping expressed by this annotation.
	 * <p>This is an alias for {@link #path}. For example
	 * {@code @RequestMapping("/foo")} is equivalent to
	 * {@code @RequestMapping(path="/foo")}.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 */
	@AliasFor("path")
	String[] value() default {};

	/**
	 * In a Servlet environment only: the path mapping URIs (e.g. "/myPath.do").
	 * Ant-style path patterns are also supported (e.g. "/myPath/*.do").
	 * At the method level, relative paths (e.g. "edit.do") are supported within
	 * the primary mapping expressed at the type level. Path mapping URIs may
	 * contain placeholders (e.g. "/${connect}")
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 * @see org.springframework.web.bind.annotation.ValueConstants#DEFAULT_NONE
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] path() default {};

	/**
	 * The HTTP request methods to map to, narrowing the primary mapping:
	 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this HTTP method restriction (i.e. the type-level restriction
	 * gets checked before the handler method is even resolved).
	 */
	RequestMethod[] method() default {};

	/**
	 * The parameters of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "myParam=myValue" style
	 * expressions, with a request only mapped if each such parameter is found
	 * to have the given value. Expressions can be negated by using the "!=" operator,
	 * as in "myParam!=myValue". "myParam" style expressions are also supported,
	 * with such parameters having to be present in the request (allowed to have
	 * any value). Finally, "!myParam" style expressions indicate that the
	 * specified parameter is <i>not</i> supposed to be present in the request.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this parameter restriction (i.e. the type-level restriction
	 * gets checked before the handler method is even resolved).
	 * <p>Parameter mappings are considered as restrictions that are enforced at
	 * the type level. The primary path mapping (i.e. the specified URI value)
	 * still has to uniquely identify the target handler, with parameter mappings
	 * simply expressing preconditions for invoking the handler.
	 */
	String[] params() default {};

	/**
	 * The headers of the mapped request, narrowing the primary mapping.
	 * <p>Same format for any environment: a sequence of "My-Header=myValue" style
	 * expressions, with a request only mapped if each such header is found
	 * to have the given value. Expressions can be negated by using the "!=" operator,
	 * as in "My-Header!=myValue". "My-Header" style expressions are also supported,
	 * with such headers having to be present in the request (allowed to have
	 * any value). Finally, "!My-Header" style expressions indicate that the
	 * specified header is <i>not</i> supposed to be present in the request.
	 * <p>Also supports media type wildcards (*), for headers such as Accept
	 * and Content-Type. For instance,
	 * <pre class="code">
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * will match requests with a Content-Type of "text/html", "text/plain", etc.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this header restriction (i.e. the type-level restriction
	 * gets checked before the handler method is even resolved).
	 * @see org.springframework.http.MediaType
	 */
	String[] headers() default {};

	/**
	 * The consumable media types of the mapped request, narrowing the primary mapping.
	 * <p>The format is a single media type or a sequence of media types,
	 * with a request only mapped if the {@code Content-Type} matches one of these media types.
	 * Examples:
	 * <pre class="code">
	 * consumes = "text/plain"
	 * consumes = {"text/plain", "application/*"}
	 * </pre>
	 * Expressions can be negated by using the "!" operator, as in "!text/plain", which matches
	 * all requests with a {@code Content-Type} other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings override
	 * this consumes restriction.
	 * @see org.springframework.http.MediaType
	 * @see javax.servlet.http.HttpServletRequest#getContentType()
	 */
	String[] consumes() default {};

	/**
	 * The producible media types of the mapped request, narrowing the primary mapping.
	 * <p>The format is a single media type or a sequence of media types,
	 * with a request only mapped if the {@code Accept} matches one of these media types.
	 * Examples:
	 * <pre class="code">
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * produces = "application/json; charset=UTF-8"
	 * </pre>
	 * <p>It affects the actual content type written, for example to produce a JSON response
	 * with UTF-8 encoding, {@code "application/json; charset=UTF-8"} should be used.
	 * <p>Expressions can be negated by using the "!" operator, as in "!text/plain", which matches
	 * all requests with a {@code Accept} other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings override
	 * this produces restriction.
	 * @see org.springframework.http.MediaType
	 */
	String[] produces() default {};

}
