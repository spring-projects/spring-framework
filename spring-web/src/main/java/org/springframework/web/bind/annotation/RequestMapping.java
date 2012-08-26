/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * Annotation for mapping web requests onto specific handler classes and/or
 * handler methods. Provides a consistent style between Servlet and Portlet
 * environments, with the semantics adapting to the concrete environment.
 *
 * <p><b>NOTE:</b> The set of features supported for Servlets is a superset
 * of the set of features supported for Portlets. The places where this applies
 * are marked with the label "Servlet-only" in this source file. For Servlet
 * environments there are some further distinctions depending on whether an
 * application is configured with {@literal "@MVC 3.0"} or
 * {@literal "@MVC 3.1"} support classes. The places where this applies are
 * marked with {@literal "@MVC 3.1-only"} in this source file. For more
 * details see the note on the new support classes added in Spring MVC 3.1
 * further below.
 *
 * <p>Handler methods which are annotated with this annotation are allowed
 * to have very flexible signatures. They may have arguments of the following
 * types, in arbitrary order (except for validation results, which need to
 * follow right after the corresponding command object, if desired):
 * <ul>
 * <li>Request and/or response objects (Servlet API or Portlet API).
 * You may choose any specific request/response type, e.g.
 * {@link javax.servlet.ServletRequest} / {@link javax.servlet.http.HttpServletRequest}
 * or {@link javax.portlet.PortletRequest} / {@link javax.portlet.ActionRequest} /
 * {@link javax.portlet.RenderRequest}. Note that in the Portlet case,
 * an explicitly declared action/render argument is also used for mapping
 * specific request types onto a handler method (in case of no other
 * information given that differentiates between action and render requests).
 * <li>Session object (Servlet API or Portlet API): either
 * {@link javax.servlet.http.HttpSession} or {@link javax.portlet.PortletSession}.
 * An argument of this type will enforce the presence of a corresponding session.
 * As a consequence, such an argument will never be <code>null</code>.
 * <i>Note that session access may not be thread-safe, in particular in a
 * Servlet environment: Consider switching the
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession "synchronizeOnSession"}
 * flag to "true" if multiple requests are allowed to access a session concurrently.</i>
 * <li>{@link org.springframework.web.context.request.WebRequest} or
 * {@link org.springframework.web.context.request.NativeWebRequest}.
 * Allows for generic request parameter access as well as request/session
 * attribute access, without ties to the native Servlet/Portlet API.
 * <li>{@link java.util.Locale} for the current request locale
 * (determined by the most specific locale resolver available,
 * i.e. the configured {@link org.springframework.web.servlet.LocaleResolver}
 * in a Servlet environment and the portal locale in a Portlet environment).
 * <li>{@link java.io.InputStream} / {@link java.io.Reader} for access
 * to the request's content. This will be the raw InputStream/Reader as
 * exposed by the Servlet/Portlet API.
 * <li>{@link java.io.OutputStream} / {@link java.io.Writer} for generating
 * the response's content. This will be the raw OutputStream/Writer as
 * exposed by the Servlet/Portlet API.
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
 * specific Servlet/Portlet request parameters. Parameter values will be
 * converted to the declared method argument type. Additionally,
 * {@code @RequestParam} can be used on a {@link java.util.Map Map&lt;String, String&gt;} or
 * {@link org.springframework.util.MultiValueMap MultiValueMap&lt;String, String&gt;}
 * method parameter to gain access to all request parameters.
 * <li>{@link RequestHeader @RequestHeader} annotated parameters for access to
 * specific Servlet/Portlet request HTTP headers. Parameter values will be
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
 * Instead a {@link org.springframework.web.servlet.mvc.method.annotation.MethodArgumentNotValidException}
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
 * Instead a {@link org.springframework.web.servlet.mvc.method.annotation.MethodArgumentNotValidException}
 * exception is raised.
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
 * Specify a parameter-level {@link ModelAttribute} annotation for declaring
 * a specific model attribute name.
 * <li>{@link org.springframework.validation.Errors} /
 * {@link org.springframework.validation.BindingResult} validation results
 * for a preceding command/form object (the immediate preceding argument).
 * <li>{@link org.springframework.web.bind.support.SessionStatus} status handle
 * for marking form processing as complete (triggering the cleanup of session
 * attributes that have been indicated by the {@link SessionAttributes} annotation
 * at the handler type level).
 * <li>{@link org.springframework.web.util.UriComponentsBuilder}
 * (Servlet-only, {@literal @MVC 3.1-only})
 * for preparing a URL relative to the current request's host, port, scheme,
 * context path, and the literal part of the servlet mapping.
 * </ul>
 *
 * <p>The following return types are supported for handler methods:
 * <ul>
 * <li>A <code>ModelAndView</code> object (Servlet MVC or Portlet MVC),
 * with the model implicitly enriched with command objects and the results
 * of {@link ModelAttribute} annotated reference data accessor methods.
 * <li>A {@link org.springframework.ui.Model Model} object, with the view name
 * implicitly determined through a {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * and the model implicitly enriched with command objects and the results
 * of {@link ModelAttribute} annotated reference data accessor methods.
 * <li>A {@link java.util.Map} object for exposing a model,
 * with the view name implicitly determined through a
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * and the model implicitly enriched with command objects and the results
 * of {@link ModelAttribute} annotated reference data accessor methods.
 * <li>A {@link org.springframework.web.servlet.View} object, with the
 * model implicitly determined through command objects and
 * {@link ModelAttribute} annotated reference data accessor methods.
 * The handler method may also programmatically enrich the model by
 * declaring a {@link org.springframework.ui.Model} argument (see above).
 * <li>A {@link java.lang.String} value which is interpreted as view name,
 * with the model implicitly determined through command objects and
 * {@link ModelAttribute} annotated reference data accessor methods.
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
 * <li>A {@link Callable} which is used by Spring MVC to obtain the return
 * value asynchronously in a separate thread transparently managed by Spring MVC
 * on behalf of the application.
 * <li>A {@code org.springframework.web.context.request.async.DeferredResult}
 * which the application uses to produce a return value in a separate
 * thread of its own choosing, as an alternative to returning a Callable.
 * <li><code>void</code> if the method handles the response itself (by
 * writing the response content directly, declaring an argument of type
 * {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 * / {@link javax.portlet.RenderResponse} for that purpose)
 * or if the view name is supposed to be implicitly determined through a
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * (not declaring a response argument in the handler method signature;
 * only applicable in a Servlet environment).
 * <li>Any other return type will be considered as single model attribute
 * to be exposed to the view, using the attribute name specified through
 * {@link ModelAttribute} at the method level (or the default attribute name
 * based on the return type's class name otherwise). The model will be
 * implicitly enriched with command objects and the results of
 * {@link ModelAttribute} annotated reference data accessor methods.
 * </ul>
 *
 * <p><b>NOTE:</b> <code>@RequestMapping</code> will only be processed if an
 * an appropriate <code>HandlerMapping</code>-<code>HandlerAdapter</code> pair
 * is configured. This is the case by default in both the
 * <code>DispatcherServlet</code> and the <code>DispatcherPortlet</code>.
 * However, if you are defining custom <code>HandlerMappings</code> or
 * <code>HandlerAdapters</code>, then you need to add
 * <code>DefaultAnnotationHandlerMapping</code> and
 * <code>AnnotationMethodHandlerAdapter</code> to your configuration.</code>.
 *
 * <p><b>NOTE:</b> Spring 3.1 introduced a new set of support classes for
 * <code>@RequestMapping</code> methods in Servlet environments called
 * <code>RequestMappingHandlerMapping</code> and
 * <code>RequestMappingHandlerAdapter</code>. They are recommended for use and
 * even required to take advantage of new features in Spring MVC 3.1 (search
 * {@literal "@MVC 3.1-only"} in this source file) and going forward.
 * The new support classes are enabled by default from the MVC namespace and
 * with use of the MVC Java config (<code>@EnableWebMvc</code>) but must be
 * configured explicitly if using neither.
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * <code>@RequestMapping</code> and <code>@SessionAttributes</code> - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5
 * @see RequestParam
 * @see ModelAttribute
 * @see SessionAttributes
 * @see InitBinder
 * @see org.springframework.web.context.request.WebRequest
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * @see org.springframework.web.portlet.mvc.annotation.DefaultAnnotationHandlerMapping
 * @see org.springframework.web.portlet.mvc.annotation.AnnotationMethodHandlerAdapter
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface RequestMapping {

	/**
	 * The primary mapping expressed by this annotation.
	 * <p>In a Servlet environment: the path mapping URIs (e.g. "/myPath.do").
	 * Ant-style path patterns are also supported (e.g. "/myPath/*.do").
	 * At the method level, relative paths (e.g. "edit.do") are supported
	 * within the primary mapping expressed at the type level.
	 * <p>In a Portlet environment: the mapped portlet modes
	 * (i.e. "EDIT", "VIEW", "HELP" or any custom modes).
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this primary mapping, narrowing it for a specific handler method.
	 */
	String[] value() default {};

	/**
	 * The HTTP request methods to map to, narrowing the primary mapping:
	 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this HTTP method restriction (i.e. the type-level restriction
	 * gets checked before the handler method is even resolved).
	 * <p>Supported for Servlet environments as well as Portlet 2.0 environments.
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
	 * <p>In a Servlet environment, parameter mappings are considered as restrictions
	 * that are enforced at the type level. The primary path mapping (i.e. the
	 * specified URI value) still has to uniquely identify the target handler, with
	 * parameter mappings simply expressing preconditions for invoking the handler.
	 * <p>In a Portlet environment, parameters are taken into account as mapping
	 * differentiators, i.e. the primary portlet mode mapping plus the parameter
	 * conditions uniquely identify the target handler. Different handlers may be
	 * mapped onto the same portlet mode, as long as their parameter mappings differ.
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
	 * <pre>
	 * &#064;RequestMapping(value = "/something", headers = "content-type=text/*")
	 * </pre>
	 * will match requests with a Content-Type of "text/html", "text/plain", etc.
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings inherit
	 * this header restriction (i.e. the type-level restriction
	 * gets checked before the handler method is even resolved).
	 * <p>Maps against HttpServletRequest headers in a Servlet environment,
	 * and against PortletRequest properties in a Portlet 2.0 environment.
	 * @see org.springframework.http.MediaType
	 */
	String[] headers() default {};

	/**
	 * The consumable media types of the mapped request, narrowing the primary mapping.
	 * <p>The format is a single media type or a sequence of media types,
	 * with a request only mapped if the {@code Content-Type} matches one of these media types.
	 * Examples:
	 * <pre>
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
	 * <pre>
	 * produces = "text/plain"
	 * produces = {"text/plain", "application/*"}
	 * </pre>
	 * Expressions can be negated by using the "!" operator, as in "!text/plain", which matches
	 * all requests with a {@code Accept} other than "text/plain".
	 * <p><b>Supported at the type level as well as at the method level!</b>
	 * When used at the type level, all method-level mappings override
	 * this consumes restriction.
	 * @see org.springframework.http.MediaType
	 */
	String[] produces() default {};

}
