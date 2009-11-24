/*
 * Copyright 2002-2009 the original author or authors.
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

/**
 * Annotation for mapping web requests onto specific handler classes and/or
 * handler methods. Provides consistent style between Servlet and Portlet
 * environments, with the semantics adapting to the concrete environment.
 *
 * <p><b>NOTE:</b> Method-level mappings are only allowed to narrow the mapping
 * expressed at the class level (if any). In the Servlet case, an HTTP path needs to
 * uniquely map onto one specific handler bean (not spread across multiple handler beans);
 * the remaining mapping parameters and conditions are effectively assertions only.
 * In the Portlet case, a portlet mode in combination with specific parameter conditions
 * needs to uniquely map onto one specific handler bean, with all conditions evaluated
 * for mapping purposes. It is strongly recommended to co-locate related handler methods
 * into the same bean and therefore keep the mappings simple and intuitive.
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
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter#setSynchronizeOnSession "synchronizeOnSession"}
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
 * <li>{@link PathVariable @PathVariable} annotated parameters for access to
 * URI template values (i.e. /hotels/{hotel}). Variable values will be
 * converted to the declared method argument type. By default, the URI template
 * will match against the regular expression {@code [^\.]*} (i.e. any character
 * other than period), but this can be changed by specifying another regular
 * expression, like so: /hotels/{hotel:\d+}.
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
 * <li>{@link RequestBody @RequestBody} annotated parameters for access to
 * the Servlet request HTTP contents. Parameter values will be
 * converted to the declared method argument type using 
 * {@linkplain org.springframework.http.converter.HttpMessageConverter message
 * converters}.
 * <li>{@link java.util.Map} / {@link org.springframework.ui.Model} /
 * {@link org.springframework.ui.ModelMap} for enriching the implicit model
 * that will be exposed to the web view.
 * <li>Command/form objects to bind parameters to: as bean properties or fields,
 * with customizable type conversion, depending on {@link InitBinder} methods
 * and/or the HandlerAdapter configuration - see the "webBindingInitializer"
 * property on AnnotationMethodHandlerAdapter.
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
 * <p><b>NOTE: <code>@RequestMapping</code> will only be processed if a
 * corresponding <code>HandlerMapping</code> (for type level annotations)
 * and/or <code>HandlerAdapter</code> (for method level annotations) is
 * present in the dispatcher.</b> This is the case by default in both
 * <code>DispatcherServlet</code> and <code>DispatcherPortlet</code>.
 * However, if you are defining custom <code>HandlerMappings</code> or
 * <code>HandlerAdapters</code>, then you need to make sure that a
 * corresponding custom <code>DefaultAnnotationHandlerMapping</code>
 * and/or <code>AnnotationMethodHandlerAdapter</code> is defined as well
 * - provided that you intend to use <code>@RequestMapping</code>.
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
 * @see org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping
 * @see org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter
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
	 * <p>In case of Servlet-based handler methods, the method names are
	 * taken into account for narrowing if no path was specified explicitly,
	 * according to the specified
	 * {@link org.springframework.web.servlet.mvc.multiaction.MethodNameResolver}
	 * (by default an
	 * {@link org.springframework.web.servlet.mvc.multiaction.InternalPathMethodNameResolver}).
	 * Note that this only applies in case of ambiguous annotation mappings
	 * that do not specify a path mapping explicitly. In other words,
	 * the method name is only used for narrowing among a set of matching
	 * methods; it does not constitute a primary path mapping itself.
	 * <p>If you have a single default method (without explicit path mapping),
	 * then all requests without a more specific mapped method found will
	 * be dispatched to it. If you have multiple such default methods, then
	 * the method name will be taken into account for choosing between them.
	 */
	String[] value() default {};

	/**
	 * The HTTP request methods to map to, narrowing the primary mapping:
	 * GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE.
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
	 * to have the given value. "myParam" style expressions are also supported,
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
	 * to have the given value. "My-Header" style expressions are also supported,
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

}
