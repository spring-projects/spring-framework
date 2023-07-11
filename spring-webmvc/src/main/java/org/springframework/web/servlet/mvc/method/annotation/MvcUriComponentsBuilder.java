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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Creates instances of {@link org.springframework.web.util.UriComponentsBuilder}
 * by pointing to {@code @RequestMapping} methods on Spring MVC controllers.
 *
 * <p>There are several groups of methods:
 * <ul>
 * <li>Static {@code fromXxx(...)} methods to prepare links using information
 * from the current request as determined by a call to
 * {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder#fromCurrentServletMapping()}.
 * <li>Static {@code fromXxx(UriComponentsBuilder,...)} methods can be given
 * a baseUrl when operating outside the context of a request.
 * <li>Instance-based {@code withXxx(...)} methods where an instance of
 * MvcUriComponentsBuilder is created with a baseUrl via
 * {@link #relativeTo(org.springframework.web.util.UriComponentsBuilder)}.
 * </ul>
 *
 * <p><strong>Note:</strong> This class uses values from "Forwarded"
 * (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
 * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers,
 * if present, in order to reflect the client-originated protocol and address.
 * Consider using the {@code ForwardedHeaderFilter} in order to choose from a
 * central place whether to extract and use, or to discard such headers.
 * See the Spring Framework reference for more on this filter.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MvcUriComponentsBuilder {

	/**
	 * Well-known name for the {@link CompositeUriComponentsContributor} object in the bean factory.
	 */
	public static final String MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME = "mvcUriComponentsContributor";


	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final PathMatcher pathMatcher = new AntPathMatcher();

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static final CompositeUriComponentsContributor defaultUriComponentsContributor;

	static {
		defaultUriComponentsContributor = new CompositeUriComponentsContributor(
				new PathVariableMethodArgumentResolver(), new RequestParamMethodArgumentResolver(false));
	}

	private final UriComponentsBuilder baseUrl;


	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 * @see #fromController(Class)
	 * @see #fromMethodName(Class, String, Object...)
	 * @see #fromMethodCall(Object)
	 * @see #fromMappingName(String)
	 * @see #fromMethod(Class, Method, Object...)
	 */
	protected MvcUriComponentsBuilder(UriComponentsBuilder baseUrl) {
		Assert.notNull(baseUrl, "'baseUrl' is required");
		this.baseUrl = baseUrl;
	}


	/**
	 * Create an instance of this class with a base URL. After that calls to one
	 * of the instance based {@code withXxx(...}} methods will create URLs relative
	 * to the given base URL.
	 */
	public static MvcUriComponentsBuilder relativeTo(UriComponentsBuilder baseUrl) {
		return new MvcUriComponentsBuilder(baseUrl);
	}


	/**
	 * Create a {@link UriComponentsBuilder} from the mapping of a controller class
	 * and current request information including Servlet mapping. If the controller
	 * contains multiple mappings, only the first one is used.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param controllerType the controller to build a URI for
	 * @return a UriComponentsBuilder instance (never {@code null})
	 */
	public static UriComponentsBuilder fromController(Class<?> controllerType) {
		return fromController(null, controllerType);
	}

	/**
	 * An alternative to {@link #fromController(Class)} that accepts a
	 * {@code UriComponentsBuilder} representing the base URL. This is useful
	 * when using MvcUriComponentsBuilder outside the context of processing a
	 * request or to apply a custom baseUrl not matching the current request.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param controllerType the controller to build a URI for
	 * @return a UriComponentsBuilder instance (never {@code null})
	 */
	public static UriComponentsBuilder fromController(@Nullable UriComponentsBuilder builder,
			Class<?> controllerType) {

		builder = getBaseUrlToUse(builder);

		// Externally configured prefix via PathConfigurer..
		String prefix = getPathPrefix(controllerType);
		builder.path(prefix);

		String mapping = getClassMapping(controllerType);
		builder.path(mapping);

		return builder;
	}

	/**
	 * Create a {@link UriComponentsBuilder} from the mapping of a controller
	 * method and an array of method argument values. This method delegates
	 * to {@link #fromMethod(Class, Method, Object...)}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param controllerType the controller
	 * @param methodName the method name
	 * @param args the argument values
	 * @return a UriComponentsBuilder instance, never {@code null}
	 * @throws IllegalArgumentException if there is no matching or
	 * if there is more than one matching method
	 */
	public static UriComponentsBuilder fromMethodName(Class<?> controllerType,
			String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * An alternative to {@link #fromMethodName(Class, String, Object...)} that
	 * accepts a {@code UriComponentsBuilder} representing the base URL. This is
	 * useful when using MvcUriComponentsBuilder outside the context of processing
	 * a request or to apply a custom baseUrl not matching the current request.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param controllerType the controller
	 * @param methodName the method name
	 * @param args the argument values
	 * @return a UriComponentsBuilder instance, never {@code null}
	 * @throws IllegalArgumentException if there is no matching or
	 * if there is more than one matching method
	 */
	public static UriComponentsBuilder fromMethodName(UriComponentsBuilder builder,
			Class<?> controllerType, String methodName, Object... args) {

		Method method = getMethod(controllerType, methodName, args);
		return fromMethodInternal(builder, controllerType, method, args);
	}

	/**
	 * Create a {@link UriComponentsBuilder} from the mapping of a controller method
	 * and an array of method argument values. The array of values  must match the
	 * signature of the controller method. Values for {@code @RequestParam} and
	 * {@code @PathVariable} are used for building the URI (via implementations of
	 * {@link org.springframework.web.method.support.UriComponentsContributor
	 * UriComponentsContributor}) while remaining argument values are ignored and
	 * can be {@code null}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param controllerType the controller type
	 * @param method the controller method
	 * @param args argument values for the controller method
	 * @return a UriComponentsBuilder instance, never {@code null}
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethodInternal(null, controllerType, method, args);
	}

	/**
	 * An alternative to {@link #fromMethod(Class, Method, Object...)}
	 * that accepts a {@code UriComponentsBuilder} representing the base URL.
	 * This is useful when using MvcUriComponentsBuilder outside the context of
	 * processing a request or to apply a custom baseUrl not matching the
	 * current request.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param baseUrl the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param controllerType the controller type
	 * @param method the controller method
	 * @param args argument values for the controller method
	 * @return a UriComponentsBuilder instance (never {@code null})
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(UriComponentsBuilder baseUrl,
			@Nullable Class<?> controllerType, Method method, Object... args) {

		return fromMethodInternal(baseUrl,
				(controllerType != null ? controllerType : method.getDeclaringClass()), method, args);
	}

	/**
	 * Create a {@link UriComponentsBuilder} by invoking a "mock" controller method.
	 * The controller method and the supplied argument values are then used to
	 * delegate to {@link #fromMethod(Class, Method, Object...)}.
	 * <p>For example, given this controller:
	 * <pre class="code">
	 * &#064;RequestMapping("/people/{id}/addresses")
	 * class AddressController {
	 *
	 *   &#064;RequestMapping("/{country}")
	 *   public HttpEntity&lt;Void&gt; getAddressesForCountry(&#064;PathVariable String country) { ... }
	 *
	 *   &#064;RequestMapping(value="/", method=RequestMethod.POST)
	 *   public void addAddress(Address address) { ... }
	 * }
	 * </pre>
	 * A UriComponentsBuilder can be created:
	 * <pre class="code">
	 * // Inline style with static import of "MvcUriComponentsBuilder.on"
	 *
	 * MvcUriComponentsBuilder.fromMethodCall(
	 * 		on(AddressController.class).getAddressesForCountry("US")).buildAndExpand(1);
	 *
	 * // Longer form useful for repeated invocation (and void controller methods)
	 *
	 * AddressController controller = MvcUriComponentsBuilder.on(AddressController.class);
	 * controller.addAddress(null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * controller.getAddressesForCountry("US")
	 * builder = MvcUriComponentsBuilder.fromMethodCall(controller);
	 * </pre>
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param info either the value returned from a "mock" controller
	 * invocation or the "mock" controller itself after an invocation
	 * @return a UriComponents instance
	 * @see #on(Class)
	 * @see #controller(Class)
	 */
	public static UriComponentsBuilder fromMethodCall(Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(null, controllerType, method, arguments);
	}

	/**
	 * An alternative to {@link #fromMethodCall(Object)} that accepts a
	 * {@code UriComponentsBuilder} representing the base URL. This is useful
	 * when using MvcUriComponentsBuilder outside the context of processing a
	 * request or to apply a custom baseUrl not matching the current request.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param info either the value returned from a "mock" controller
	 * invocation or the "mock" controller itself after an invocation
	 * @return a UriComponents instance
	 */
	public static UriComponentsBuilder fromMethodCall(UriComponentsBuilder builder, Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info, "MethodInvocationInfo required");
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(builder, controllerType, method, arguments);
	}

	/**
	 * Return a "mock" controller instance. When an {@code @RequestMapping} method
	 * on the controller is invoked, the supplied argument values are remembered
	 * and the result can then be used to create a {@code UriComponentsBuilder}
	 * via {@link #fromMethodCall(Object)}.
	 * <p>Note that this is a shorthand version of {@link #controller(Class)} intended
	 * for inline use (with a static import), for example:
	 * <pre class="code">
	 * MvcUriComponentsBuilder.fromMethodCall(on(FooController.class).getFoo(1)).build();
	 * </pre>
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param controllerType the target controller
	 */
	public static <T> T on(Class<T> controllerType) {
		return controller(controllerType);
	}

	/**
	 * Return a "mock" controller instance. When an {@code @RequestMapping} method
	 * on the controller is invoked, the supplied argument values are remembered
	 * and the result can then be used to create {@code UriComponentsBuilder} via
	 * {@link #fromMethodCall(Object)}.
	 * <p>This is a longer version of {@link #on(Class)}. It is needed with controller
	 * methods returning void as well for repeated invocations.
	 * <pre class="code">
	 * FooController fooController = controller(FooController.class);
	 *
	 * fooController.saveFoo(1, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 *
	 * fooController.saveFoo(2, null);
	 * builder = MvcUriComponentsBuilder.fromMethodCall(fooController);
	 * </pre>
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param controllerType the target controller
	 */
	public static <T> T controller(Class<T> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		return ControllerMethodInvocationInterceptor.initProxy(controllerType, null);
	}

	/**
	 * Create a URL from the name of a Spring MVC controller method's request mapping.
	 * <p>The configured
	 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 * HandlerMethodMappingNamingStrategy} determines the names of controller
	 * method request mappings at startup. By default, all mappings are assigned
	 * a name based on the capital letters of the class name, followed by "#" as
	 * separator, and then the method name. For example "PC#getPerson"
	 * for a class named PersonController with method getPerson. In case the
	 * naming convention does not produce unique results, an explicit name may
	 * be assigned through the name attribute of the {@code @RequestMapping}
	 * annotation.
	 * <p>This is aimed primarily for use in view rendering technologies and EL
	 * expressions. The Spring URL tag library registers this method as a function
	 * called "mvcUrl".
	 * <p>For example, given this controller:
	 * <pre class="code">
	 * &#064;RequestMapping("/people")
	 * class PersonController {
	 *
	 *   &#064;RequestMapping("/{id}")
	 *   public HttpEntity&lt;Void&gt; getPerson(&#064;PathVariable String id) { ... }
	 *
	 * }
	 * </pre>
	 *
	 * A JSP can prepare a URL to the controller method as follows:
	 *
	 * <pre class="code">
	 * &lt;%@ taglib uri="http://www.springframework.org/tags" prefix="s" %&gt;
	 *
	 * &lt;a href="${s:mvcUrl('PC#getPerson').arg(0,"123").build()}"&gt;Get Person&lt;/a&gt;
	 * </pre>
	 * <p>Note that it's not necessary to specify all arguments. Only the ones
	 * required to prepare the URL, mainly {@code @RequestParam} and {@code @PathVariable}).
	 *
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param mappingName the mapping name
	 * @return a builder to prepare the URI String
	 * @throws IllegalArgumentException if the mapping name is not found or
	 * if there is no unique match
	 * @since 4.1
	 */
	public static MethodArgumentBuilder fromMappingName(String mappingName) {
		return fromMappingName(null, mappingName);
	}

	/**
	 * An alternative to {@link #fromMappingName(String)} that accepts a
	 * {@code UriComponentsBuilder} representing the base URL. This is useful
	 * when using MvcUriComponentsBuilder outside the context of processing a
	 * request or to apply a custom baseUrl not matching the current request.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param name the mapping name
	 * @return a builder to prepare the URI String
	 * @throws IllegalArgumentException if the mapping name is not found or
	 * if there is no unique match
	 * @since 4.2
	 */
	public static MethodArgumentBuilder fromMappingName(@Nullable UriComponentsBuilder builder, String name) {
		WebApplicationContext wac = getWebApplicationContext();
		Assert.notNull(wac, "No WebApplicationContext");
		Map<String, RequestMappingInfoHandlerMapping> map = wac.getBeansOfType(RequestMappingInfoHandlerMapping.class);
		List<HandlerMethod> handlerMethods = null;
		for (RequestMappingInfoHandlerMapping mapping : map.values()) {
			handlerMethods = mapping.getHandlerMethodsForMappingName(name);
			if (handlerMethods != null) {
				break;
			}
		}
		if (handlerMethods == null) {
			throw new IllegalArgumentException("Mapping not found: " + name);
		}
		else if (handlerMethods.size() != 1) {
			throw new IllegalArgumentException("No unique match for mapping " + name + ": " + handlerMethods);
		}
		else {
			HandlerMethod handlerMethod = handlerMethods.get(0);
			Class<?> controllerType = handlerMethod.getBeanType();
			Method method = handlerMethod.getMethod();
			return new MethodArgumentBuilder(builder, controllerType, method);
		}
	}


	// Instance methods, relative to a base UriComponentsBuilder...

	/**
	 * An alternative to {@link #fromController(Class)} for use with an instance
	 * of this class created via a call to {@link #relativeTo}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @since 4.2
	 */
	public UriComponentsBuilder withController(Class<?> controllerType) {
		return fromController(this.baseUrl, controllerType);
	}

	/**
	 * An alternative to {@link #fromMethodName(Class, String, Object...)}} for
	 * use with an instance of this class created via {@link #relativeTo}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodName(Class<?> controllerType, String methodName, Object... args) {
		return fromMethodName(this.baseUrl, controllerType, methodName, args);
	}

	/**
	 * An alternative to {@link #fromMethodCall(Object)} for use with an instance
	 * of this class created via {@link #relativeTo}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodCall(Object invocationInfo) {
		return fromMethodCall(this.baseUrl, invocationInfo);
	}

	/**
	 * An alternative to {@link #fromMappingName(String)} for use with an instance
	 * of this class created via {@link #relativeTo}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @since 4.2
	 */
	public MethodArgumentBuilder withMappingName(String mappingName) {
		return fromMappingName(this.baseUrl, mappingName);
	}

	/**
	 * An alternative to {@link #fromMethod(Class, Method, Object...)}
	 * for use with an instance of this class created via {@link #relativeTo}.
	 * <p><strong>Note:</strong> This method extracts values from "Forwarded"
	 * and "X-Forwarded-*" headers if found. See class-level docs.
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethod(this.baseUrl, controllerType, method, args);
	}


	private static UriComponentsBuilder fromMethodInternal(@Nullable UriComponentsBuilder builder,
			Class<?> controllerType, Method method, Object... args) {

		builder = getBaseUrlToUse(builder);

		// Externally configured prefix via PathConfigurer..
		String prefix = getPathPrefix(controllerType);
		builder.path(prefix);

		String typePath = getClassMapping(controllerType);
		String methodPath = getMethodMapping(method);
		String path = pathMatcher.combine(typePath, methodPath);
		path = PathPatternParser.defaultInstance.initFullPathPattern(path);
		builder.path(path);

		return applyContributors(builder, method, args);
	}

	private static UriComponentsBuilder getBaseUrlToUse(@Nullable UriComponentsBuilder baseUrl) {
		return baseUrl == null ?
				ServletUriComponentsBuilder.fromCurrentServletMapping() :
				baseUrl.cloneBuilder();
	}

	private static String getPathPrefix(Class<?> controllerType) {
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			Map<String, RequestMappingHandlerMapping> map = wac.getBeansOfType(RequestMappingHandlerMapping.class);
			for (RequestMappingHandlerMapping mapping : map.values()) {
				if (mapping.isHandler(controllerType)) {
					String prefix = mapping.getPathPrefix(controllerType);
					if (prefix != null) {
						return prefix;
					}
				}
			}
		}
		return "";
	}

	private static String getClassMapping(Class<?> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
		if (mapping == null) {
			return "/";
		}
		String[] paths = mapping.path();
		if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isTraceEnabled()) {
			logger.trace("Using first of multiple paths on " + controllerType.getName());
		}
		return paths[0];
	}

	private static String getMethodMapping(Method method) {
		Assert.notNull(method, "'method' must not be null");
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (requestMapping == null) {
			throw new IllegalArgumentException("No @RequestMapping on: " + method.toGenericString());
		}
		String[] paths = requestMapping.path();
		if (ObjectUtils.isEmpty(paths) || !StringUtils.hasLength(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isTraceEnabled()) {
			logger.trace("Using first of multiple paths on " + method.toGenericString());
		}
		return paths[0];
	}

	private static Method getMethod(Class<?> controllerType, final String methodName, final Object... args) {
		MethodFilter selector = method -> {
			String name = method.getName();
			int argLength = method.getParameterCount();
			return (name.equals(methodName) && argLength == args.length);
		};
		Set<Method> methods = MethodIntrospector.selectMethods(controllerType, selector);
		if (methods.size() == 1) {
			return methods.iterator().next();
		}
		else if (methods.size() > 1) {
			throw new IllegalArgumentException(String.format(
					"Found two methods named '%s' accepting arguments %s in controller %s: [%s]",
					methodName, Arrays.asList(args), controllerType.getName(), methods));
		}
		else {
			throw new IllegalArgumentException("No method named '" + methodName + "' with " + args.length +
					" arguments found in controller " + controllerType.getName());
		}
	}

	private static UriComponentsBuilder applyContributors(UriComponentsBuilder builder, Method method, Object... args) {
		CompositeUriComponentsContributor contributor = getUriComponentsContributor();

		int paramCount = method.getParameterCount();
		int argCount = args.length;
		if (paramCount != argCount) {
			throw new IllegalArgumentException("Number of method parameters " + paramCount +
					" does not match number of argument values " + argCount);
		}

		final Map<String, Object> uriVars = new HashMap<>();
		for (int i = 0; i < paramCount; i++) {
			MethodParameter param = new SynthesizingMethodParameter(method, i);
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			contributor.contributeMethodArgument(param, args[i], builder, uriVars);
		}

		// This may not be all the URI variables, supply what we have so far.
		return builder.uriVariables(uriVars);
	}

	private static CompositeUriComponentsContributor getUriComponentsContributor() {
		WebApplicationContext wac = getWebApplicationContext();
		if (wac != null) {
			try {
				return wac.getBean(MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME, CompositeUriComponentsContributor.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}
		return defaultUriComponentsContributor;
	}

	@Nullable
	private static WebApplicationContext getWebApplicationContext() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null) {
			return null;
		}
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		String attributeName = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(attributeName);
		if (wac == null) {
			return null;
		}
		return wac;
	}



	/**
	 * Method invocation information.
	 */
	public interface MethodInvocationInfo {

		/**
		 * Return the controller types.
		 */
		Class<?> getControllerType();

		/**
		 * Return the controller method.
		 */
		Method getControllerMethod();

		/**
		 * Return the argument values.
		 */
		Object[] getArgumentValues();
	}


	private static class ControllerMethodInvocationInterceptor
			implements MethodInterceptor, InvocationHandler, MethodInvocationInfo {

		private final Class<?> controllerType;

		@Nullable
		private Method controllerMethod;

		@Nullable
		private Object[] argumentValues;

		ControllerMethodInvocationInterceptor(Class<?> controllerType) {
			this.controllerType = controllerType;
		}

		@Override
		@Nullable
		public Object intercept(@Nullable Object obj, Method method, Object[] args, @Nullable MethodProxy proxy) {
			switch (method.getName()) {
				case "getControllerType": return this.controllerType;
				case "getControllerMethod": return this.controllerMethod;
				case "getArgumentValues": return this.argumentValues;
			}
			if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, obj, args);
			}
			else {
				this.controllerMethod = method;
				this.argumentValues = args;
				Class<?> returnType = method.getReturnType();
				try {
					return (returnType == void.class ? null : returnType.cast(initProxy(returnType, this)));
				}
				catch (Throwable ex) {
					throw new IllegalStateException(
							"Failed to create proxy for controller method return type: " + method, ex);
				}
			}
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, @Nullable Object[] args) {
			return intercept(proxy, method, (args != null ? args : new Object[0]), null);
		}

		@Override
		public Class<?> getControllerType() {
			return this.controllerType;
		}

		@Override
		public Method getControllerMethod() {
			Assert.state(this.controllerMethod != null, "Not initialized yet");
			return this.controllerMethod;
		}

		@Override
		public Object[] getArgumentValues() {
			Assert.state(this.argumentValues != null, "Not initialized yet");
			return this.argumentValues;
		}


		@SuppressWarnings("unchecked")
		private static <T> T initProxy(
				Class<?> controllerType, @Nullable ControllerMethodInvocationInterceptor interceptor) {

			interceptor = (interceptor != null ?
					interceptor : new ControllerMethodInvocationInterceptor(controllerType));

			if (controllerType == Object.class) {
				return (T) interceptor;
			}

			else if (controllerType.isInterface()) {
				ClassLoader classLoader = controllerType.getClassLoader();
				if (classLoader == null) {
					// JDK bootstrap loader -> use MethodInvocationInfo ClassLoader instead.
					classLoader = MethodInvocationInfo.class.getClassLoader();
				}
				else if (classLoader.getParent() == null) {
					// Potentially the JDK platform loader on JDK 9+
					ClassLoader miiClassLoader = MethodInvocationInfo.class.getClassLoader();
					ClassLoader miiParent = miiClassLoader.getParent();
					while (miiParent != null) {
						if (classLoader == miiParent) {
							// Suggested ClassLoader is ancestor of MethodInvocationInfo ClassLoader
							// -> use MethodInvocationInfo ClassLoader itself instead.
							classLoader = miiClassLoader;
							break;
						}
						miiParent = miiParent.getParent();
					}
				}
				Class<?>[] ifcs = new Class<?>[] {controllerType, MethodInvocationInfo.class};
				return (T) Proxy.newProxyInstance(classLoader, ifcs, interceptor);
			}

			else {
				Enhancer enhancer = new Enhancer();
				enhancer.setSuperclass(controllerType);
				enhancer.setInterfaces(new Class<?>[] {MethodInvocationInfo.class});
				enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
				enhancer.setCallbackType(MethodInterceptor.class);

				Class<?> proxyClass = enhancer.createClass();
				Object proxy = null;

				if (objenesis.isWorthTrying()) {
					try {
						proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
					}
					catch (ObjenesisException ex) {
						logger.debug("Failed to create controller proxy, falling back on default constructor", ex);
					}
				}

				if (proxy == null) {
					try {
						proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
					}
					catch (Throwable ex) {
						throw new IllegalStateException(
								"Failed to create controller proxy or use default constructor", ex);
					}
				}

				((Factory) proxy).setCallbacks(new Callback[] {interceptor});
				return (T) proxy;
			}
		}
	}


	/**
	 * Builder class to create URLs for method arguments.
	 */
	public static class MethodArgumentBuilder {

		private final Class<?> controllerType;

		private final Method method;

		private final Object[] argumentValues;

		private final UriComponentsBuilder baseUrl;

		/**
		 * Create a new {@link MethodArgumentBuilder} instance.
		 * @since 4.2
		 */
		public MethodArgumentBuilder(Class<?> controllerType, Method method) {
			this(null, controllerType, method);
		}

		/**
		 * Create a new {@link MethodArgumentBuilder} instance.
		 * @since 4.2
		 */
		public MethodArgumentBuilder(@Nullable UriComponentsBuilder baseUrl, Class<?> controllerType, Method method) {
			Assert.notNull(controllerType, "'controllerType' is required");
			Assert.notNull(method, "'method' is required");
			this.baseUrl = baseUrl != null ? baseUrl : UriComponentsBuilder.fromPath(getPath());
			this.controllerType = controllerType;
			this.method = method;
			this.argumentValues = new Object[method.getParameterCount()];
		}

		private static String getPath() {
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
			String path = builder.build().getPath();
			return path != null ? path : "";
		}

		public MethodArgumentBuilder arg(int index, Object value) {
			this.argumentValues[index] = value;
			return this;
		}

		/**
		 * Use this method only if you need to apply strong encoding to expanded
		 * URI variables by quoting all characters with reserved meaning.
		 * @since 5.0.8
		 */
		public MethodArgumentBuilder encode() {
			this.baseUrl.encode();
			return this;
		}

		public String build() {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues)
					.build().encode().toUriString();
		}

		public String buildAndExpand(Object... uriVars) {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method, this.argumentValues)
					.buildAndExpand(uriVars).encode().toString();
		}
	}

}
