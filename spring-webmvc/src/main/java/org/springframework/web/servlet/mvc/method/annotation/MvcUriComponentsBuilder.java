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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Creates instances of {@link org.springframework.web.util.UriComponentsBuilder}
 * by pointing to Spring MVC controllers and {@code @RequestMapping} methods.
 *
 * <p>The static {@code fromXxx(...)} methods prepare links relative to the
 * current request as determined by a call to
 * {@link org.springframework.web.servlet.support.ServletUriComponentsBuilder#fromCurrentServletMapping()}.
 *
 * <p>The static {@code fromXxx(UriComponentsBuilder,...)} methods can be given
 * the baseUrl when operating outside the context of a request.
 *
 * <p>You can also create an MvcUriComponentsBuilder instance with a baseUrl
 * via {@link #relativeTo(org.springframework.web.util.UriComponentsBuilder)}
 * and then use the non-static {@code withXxx(...)} method variants.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @author Sam Brannen
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
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param controllerType the controller to build a URI for
	 * @return a UriComponentsBuilder instance (never {@code null})
	 */
	public static UriComponentsBuilder fromController(UriComponentsBuilder builder,
			Class<?> controllerType) {

		builder = getBaseUrlToUse(builder);
		String mapping = getTypeRequestMapping(controllerType);
		return builder.path(mapping);
	}

	/**
	 * Create a {@link UriComponentsBuilder} from the mapping of a controller
	 * method and an array of method argument values. This method delegates
	 * to {@link #fromMethod(Class, Method, Object...)}.
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
	 * Create a {@link UriComponentsBuilder} by invoking a "mock" controller method.
	 * The controller method and the supplied argument values are then used to
	 * delegate to {@link #fromMethod(Class, Method, Object...)}.
	 * <p>For example, given this controller:
	 * <pre class="code">
	 * &#064;RequestMapping("/people/{id}/addresses")
	 * class AddressController {
	 *
	 *   &#064;RequestMapping("/{country}")
	 *   public HttpEntity<Void> getAddressesForCountry(&#064;PathVariable String country) { ... }
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
	 * @param info either the value returned from a "mock" controller
	 * invocation or the "mock" controller itself after an invocation
	 * @return a UriComponents instance
	 */
	public static UriComponentsBuilder fromMethodCall(Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info);
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
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param info either the value returned from a "mock" controller
	 * invocation or the "mock" controller itself after an invocation
	 * @return a UriComponents instance
	 */
	public static UriComponentsBuilder fromMethodCall(UriComponentsBuilder builder, Object info) {
		Assert.isInstanceOf(MethodInvocationInfo.class, info);
		MethodInvocationInfo invocationInfo = (MethodInvocationInfo) info;
		Class<?> controllerType = invocationInfo.getControllerType();
		Method method = invocationInfo.getControllerMethod();
		Object[] arguments = invocationInfo.getArgumentValues();
		return fromMethodInternal(builder, controllerType, method, arguments);
	}

	/**
	 * Create a URL from the name of a Spring MVC controller method's request mapping.
	 * <p>The configured
	 * {@link org.springframework.web.servlet.handler.HandlerMethodMappingNamingStrategy
	 * HandlerMethodMappingNamingStrategy} determines the names of controller
	 * method request mappings at startup. By default all mappings are assigned
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
	 *   public HttpEntity<Void> getPerson(&#064;PathVariable String id) { ... }
	 *
	 * }
	 * </pre>
	 *
	 * A JSP can prepare a URL to the controller method as follows:
	 *
	 * <pre class="code">
	 * <%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
	 *
	 * &lt;a href="${s:mvcUrl('PC#getPerson').arg(0,"123").build()}"&gt;Get Person&lt;/a&gt;
	 * </pre>
	 * <p>Note that it's not necessary to specify all arguments. Only the ones
	 * required to prepare the URL, mainly {@code @RequestParam} and {@code @PathVariable}).
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
	 * @param builder the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param name the mapping name
	 * @return a builder to prepare the URI String
	 * @throws IllegalArgumentException if the mapping name is not found or
	 * if there is no unique match
	 * @since 4.2
	 */
	public static MethodArgumentBuilder fromMappingName(UriComponentsBuilder builder, String name) {
		RequestMappingInfoHandlerMapping handlerMapping = getRequestMappingInfoHandlerMapping();
		List<HandlerMethod> handlerMethods = handlerMapping.getHandlerMethodsForMappingName(name);
		if (handlerMethods == null) {
			throw new IllegalArgumentException("Mapping mappingName not found: " + name);
		}
		if (handlerMethods.size() != 1) {
			throw new IllegalArgumentException("No unique match for mapping mappingName " +
					name + ": " + handlerMethods);
		}
		HandlerMethod handlerMethod = handlerMethods.get(0);
		Class<?> controllerType = handlerMethod.getBeanType();
		Method method = handlerMethod.getMethod();
		return new MethodArgumentBuilder(builder, controllerType, method);
	}

	/**
	 * Create a {@link UriComponentsBuilder} from the mapping of a controller method
	 * and an array of method argument values. The array of values  must match the
	 * signature of the controller method. Values for {@code @RequestParam} and
	 * {@code @PathVariable} are used for building the URI (via implementations of
	 * {@link org.springframework.web.method.support.UriComponentsContributor
	 * UriComponentsContributor}) while remaining argument values are ignored and
	 * can be {@code null}.
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
	 * @param baseUrl the builder for the base URL; the builder will be cloned
	 * and therefore not modified and may be re-used for further calls.
	 * @param controllerType the controller type
	 * @param method the controller method
	 * @param args argument values for the controller method
	 * @return a UriComponentsBuilder instance (never {@code null})
	 * @since 4.2
	 */
	public static UriComponentsBuilder fromMethod(UriComponentsBuilder baseUrl,
			Class<?> controllerType, Method method, Object... args) {

		return fromMethodInternal(baseUrl,
				(controllerType != null ? controllerType : method.getDeclaringClass()), method, args);
	}

	/**
	 * @see #fromMethod(Class, Method, Object...)
	 * @see #fromMethod(UriComponentsBuilder, Class, Method, Object...)
	 * @deprecated as of 4.2, this is deprecated in favor of the overloaded
	 * method that also accepts a controllerType argument
	 */
	@Deprecated
	public static UriComponentsBuilder fromMethod(Method method, Object... args) {
		return fromMethodInternal(null, method.getDeclaringClass(), method, args);
	}

	private static UriComponentsBuilder fromMethodInternal(UriComponentsBuilder baseUrl,
			Class<?> controllerType, Method method, Object... args) {

		baseUrl = getBaseUrlToUse(baseUrl);
		String typePath = getTypeRequestMapping(controllerType);
		String methodPath = getMethodRequestMapping(method);
		String path = pathMatcher.combine(typePath, methodPath);
		baseUrl.path(path);
		UriComponents uriComponents = applyContributors(baseUrl, method, args);
		return UriComponentsBuilder.newInstance().uriComponents(uriComponents);
	}

	private static UriComponentsBuilder getBaseUrlToUse(UriComponentsBuilder baseUrl) {
		if (baseUrl != null) {
			return baseUrl.cloneBuilder();
		}
		else {
			return ServletUriComponentsBuilder.fromCurrentServletMapping();
		}
	}

	private static String getTypeRequestMapping(Class<?> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(controllerType, RequestMapping.class);
		if (requestMapping == null) {
			return "/";
		}
		String[] paths = requestMapping.path();
		if (ObjectUtils.isEmpty(paths) || StringUtils.isEmpty(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isWarnEnabled()) {
			logger.warn("Multiple paths on controller " + controllerType.getName() + ", using first one");
		}
		return paths[0];
	}

	private static String getMethodRequestMapping(Method method) {
		Assert.notNull(method, "'method' must not be null");
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
		if (requestMapping == null) {
			throw new IllegalArgumentException("No @RequestMapping on: " + method.toGenericString());
		}
		String[] paths = requestMapping.path();
		if (ObjectUtils.isEmpty(paths) || StringUtils.isEmpty(paths[0])) {
			return "/";
		}
		if (paths.length > 1 && logger.isWarnEnabled()) {
			logger.warn("Multiple paths on method " + method.toGenericString() + ", using first one");
		}
		return paths[0];
	}

	private static Method getMethod(Class<?> controllerType, final String methodName, final Object... args) {
		MethodFilter selector = new MethodFilter() {
			@Override
			public boolean matches(Method method) {
				String name = method.getName();
				int argLength = method.getParameterTypes().length;
				return (name.equals(methodName) && argLength == args.length);
			}
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

	private static UriComponents applyContributors(UriComponentsBuilder builder, Method method, Object... args) {
		CompositeUriComponentsContributor contributor = getConfiguredUriComponentsContributor();
		if (contributor == null) {
			logger.debug("Using default CompositeUriComponentsContributor");
			contributor = defaultUriComponentsContributor;
		}

		int paramCount = method.getParameterTypes().length;
		int argCount = args.length;
		if (paramCount != argCount) {
			throw new IllegalArgumentException("Number of method parameters " + paramCount +
					" does not match number of argument values " + argCount);
		}

		final Map<String, Object> uriVars = new HashMap<String, Object>();
		for (int i = 0; i < paramCount; i++) {
			MethodParameter param = new SynthesizingMethodParameter(method, i);
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			contributor.contributeMethodArgument(param, args[i], builder, uriVars);
		}

		// We may not have all URI var values, expand only what we have
		return builder.build().expand(new UriComponents.UriTemplateVariables() {
			@Override
			public Object getValue(String name) {
				return uriVars.containsKey(name) ? uriVars.get(name) : UriComponents.UriTemplateVariables.SKIP_VALUE;
			}
		});
	}

	private static CompositeUriComponentsContributor getConfiguredUriComponentsContributor() {
		WebApplicationContext wac = getWebApplicationContext();
		if (wac == null) {
			return null;
		}
		try {
			return wac.getBean(MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME, CompositeUriComponentsContributor.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No CompositeUriComponentsContributor bean with name '" +
						MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME + "'");
			}
			return null;
		}
	}

	private static RequestMappingInfoHandlerMapping getRequestMappingInfoHandlerMapping() {
		WebApplicationContext wac = getWebApplicationContext();
		Assert.notNull(wac, "Cannot lookup handler method mappings without WebApplicationContext");
		try {
			return wac.getBean(RequestMappingInfoHandlerMapping.class);
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw new IllegalStateException("More than one RequestMappingInfoHandlerMapping beans found", ex);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new IllegalStateException("No RequestMappingInfoHandlerMapping bean", ex);
		}
	}

	private static WebApplicationContext getWebApplicationContext() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		if (requestAttributes == null) {
			logger.debug("No request bound to the current thread: is DispatcherSerlvet used?");
			return null;
		}

		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
		if (request == null) {
			logger.debug("Request bound to current thread is not an HttpServletRequest");
			return null;
		}

		String attributeName = DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(attributeName);
		if (wac == null) {
			logger.debug("No WebApplicationContext found: not in a DispatcherServlet request?");
			return null;
		}
		return wac;
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
	 * @param controllerType the target controller
	 */
	public static <T> T controller(Class<T> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		return initProxy(controllerType, new ControllerMethodInvocationInterceptor(controllerType));
	}

	@SuppressWarnings("unchecked")
	private static <T> T initProxy(Class<?> type, ControllerMethodInvocationInterceptor interceptor) {
		if (type.isInterface()) {
			ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
			factory.addInterface(type);
			factory.addInterface(MethodInvocationInfo.class);
			factory.addAdvice(interceptor);
			return (T) factory.getProxy();
		}

		else {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(type);
			enhancer.setInterfaces(new Class<?>[] {MethodInvocationInfo.class});
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

			Class<?> proxyClass = enhancer.createClass();
			Object proxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
				}
				catch (ObjenesisException ex) {
					logger.debug("Unable to instantiate controller proxy using Objenesis, " +
							"falling back to regular construction", ex);
				}
			}

			if (proxy == null) {
				try {
					proxy = proxyClass.newInstance();
				}
				catch (Exception ex) {
					throw new IllegalStateException("Unable to instantiate controller proxy using Objenesis, " +
							"and regular controller instantiation via default constructor fails as well", ex);
				}
			}

			((Factory) proxy).setCallbacks(new Callback[] {interceptor});
			return (T) proxy;
		}
	}

	/**
	 * An alternative to {@link #fromController(Class)} for use with an instance
	 * of this class created via a call to {@link #relativeTo}.
	 * @since 4.2
	 */
	public UriComponentsBuilder withController(Class<?> controllerType) {
		return fromController(this.baseUrl, controllerType);
	}

	/**
	 * An alternative to {@link #fromMethodName(Class, String, Object...)}} for
	 * use with an instance of this class created via {@link #relativeTo}.
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodName(Class<?> controllerType, String methodName, Object... args) {
		return fromMethodName(this.baseUrl, controllerType, methodName, args);
	}

	/**
	 * An alternative to {@link #fromMethodCall(Object)} for use with an instance
	 * of this class created via {@link #relativeTo}.
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethodCall(Object invocationInfo) {
		return fromMethodCall(this.baseUrl, invocationInfo);
	}

	/**
	 * An alternative to {@link #fromMappingName(String)} for use with an instance
	 * of this class created via {@link #relativeTo}.
	 * @since 4.2
	 */
	public MethodArgumentBuilder withMappingName(String mappingName) {
		return fromMappingName(this.baseUrl, mappingName);
	}

	/**
	 * An alternative to {@link #fromMethod(Class, Method, Object...)}
	 * for use with an instance of this class created via {@link #relativeTo}.
	 * @since 4.2
	 */
	public UriComponentsBuilder withMethod(Class<?> controllerType, Method method, Object... args) {
		return fromMethod(this.baseUrl, controllerType, method, args);
	}


	private static class ControllerMethodInvocationInterceptor
			implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor {

		private static final Method getControllerMethod =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getControllerMethod");

		private static final Method getArgumentValues =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getArgumentValues");

		private static final Method getControllerType =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getControllerType");

		private Method controllerMethod;

		private Object[] argumentValues;

		private Class<?> controllerType;

		ControllerMethodInvocationInterceptor(Class<?> controllerType) {
			this.controllerType = controllerType;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
			if (getControllerMethod.equals(method)) {
				return this.controllerMethod;
			}
			else if (getArgumentValues.equals(method)) {
				return this.argumentValues;
			}
			else if (getControllerType.equals(method)) {
				return this.controllerType;
			}
			else if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, obj, args);
			}
			else {
				this.controllerMethod = method;
				this.argumentValues = args;
				Class<?> returnType = method.getReturnType();
				return (void.class == returnType ? null : returnType.cast(initProxy(returnType, this)));
			}
		}

		@Override
		public Object invoke(org.aopalliance.intercept.MethodInvocation inv) throws Throwable {
			return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
		}
	}


	public interface MethodInvocationInfo {

		Method getControllerMethod();

		Object[] getArgumentValues();

		Class<?> getControllerType();
	}


	public static class MethodArgumentBuilder {

		private final Class<?> controllerType;

		private final Method method;

		private final Object[] argumentValues;

		private final UriComponentsBuilder baseUrl;

		/**
		 * @since 4.2
		 */
		public MethodArgumentBuilder(Class<?> controllerType, Method method) {
			this(null, controllerType, method);
		}

		/**
		 * @since 4.2
		 */
		public MethodArgumentBuilder(UriComponentsBuilder baseUrl, Class<?> controllerType, Method method) {
			Assert.notNull(controllerType, "'controllerType' is required");
			Assert.notNull(method, "'method' is required");
			this.baseUrl = (baseUrl != null ? baseUrl : initBaseUrl());
			this.controllerType = controllerType;
			this.method = method;
			this.argumentValues = new Object[method.getParameterTypes().length];
			for (int i = 0; i < this.argumentValues.length; i++) {
				this.argumentValues[i] = null;
			}
		}

		/**
		 * @deprecated as of 4.2, this is deprecated in favor of alternative constructors
		 * that accept a controllerType argument
		 */
		@Deprecated
		public MethodArgumentBuilder(Method method) {
			this(method.getDeclaringClass(), method);
		}

		private static UriComponentsBuilder initBaseUrl() {
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
			return UriComponentsBuilder.fromPath(builder.build().getPath());
		}

		public MethodArgumentBuilder arg(int index, Object value) {
			this.argumentValues[index] = value;
			return this;
		}

		public String build() {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method,
					this.argumentValues).build(false).encode().toUriString();
		}

		public String buildAndExpand(Object... uriVars) {
			return fromMethodInternal(this.baseUrl, this.controllerType, this.method,
					this.argumentValues).build(false).expand(uriVars).encode().toString();
		}
	}

}
