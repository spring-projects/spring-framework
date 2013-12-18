/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A UriComponentsBuilder that helps to build URIs to Spring MVC controllers
 * and methods from their request mappings.
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MvcUriComponentsBuilder extends UriComponentsBuilder {

	/**
	 * Well-known name for the {@link CompositeUriComponentsContributor} object in the bean factory.
	 */
	public static final String MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME = "mvcUriComponentsContributor";


	private static final CompositeUriComponentsContributor defaultUriComponentsContributor;

	private static final PathMatcher pathMatcher = new AntPathMatcher();

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private static final ObjenesisStd objenesis = new ObjenesisStd(true);

	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);


	static {
		defaultUriComponentsContributor = new CompositeUriComponentsContributor(
				new PathVariableMethodArgumentResolver(), new RequestParamMethodArgumentResolver(false));
	}


	/**
	 * Create a {@link UriComponentsBuilder} by pointing to a controller class. The
	 * resulting builder contains the current request information up to and including
	 * the Servlet mapping plus any type-level request mapping. If the controller
	 * contains multiple mappings, the first one is used.
	 * @param controllerType the controller to create a URI for
	 * @return a UriComponentsBuilder instance
	 */
	public static UriComponentsBuilder fromController(Class<?> controllerType) {
		String mapping = getTypeRequestMapping(controllerType);
		return ServletUriComponentsBuilder.fromCurrentServletMapping().path(mapping);
	}

	/**
	 * Create a {@link UriComponentsBuilder} by pointing to a controller method and
	 * providing method argument values. The method is matched based on the provided
	 * method name and the number of argument values. If that results in a clash
	 * (i.e. overloaded methods with the same number of parameters), use
	 * {@link #fromMethod(java.lang.reflect.Method, Object...)} instead.
	 * <p>The argument values are used to prepare the URI for example expanding
	 * path variables, or adding query parameters. Any other arguments not
	 * relevant to the URI can be provided as {@literal null} and will be ignored.
	 * <p>Additional (custom) argument types can be supported through an implementation
	 * of {@link org.springframework.web.method.support.UriComponentsContributor}.
	 * @param controllerType the target controller type
	 * @param methodName the target method name
	 * @param argumentValues argument values matching to method parameters
	 * @return a UriComponentsBuilder instance
	 */
	public static UriComponentsBuilder fromMethodName(Class<?> controllerType, String methodName, Object... argumentValues) {
		Method match = null;
		for (Method method : controllerType.getDeclaredMethods()) {
			if (method.getName().equals(methodName) && method.getParameterTypes().length == argumentValues.length) {
				if (match != null) {
					throw new IllegalStateException("Found two methods named '" + methodName + "' having " +
							Arrays.asList(argumentValues) + " arguments, controller " + controllerType.getName());
				}
				match = method;
			}
		}
		if (match == null) {
			throw new IllegalArgumentException("No method '" + methodName + "' with " + argumentValues.length +
					" parameters found in " + controllerType.getName());
		}
		return fromMethod(match, argumentValues);
	}

	/**
	 * Create a {@link UriComponentsBuilder} by pointing to a controller method and
	 * providing method argument values. The method argument values are used to
	 * prepare the URI for example expanding path variables, or adding request
	 * parameters. Any other arguments not relevant to the URL can be provided as
	 * {@literal null} and will be ignored.
	 * <p>Additional (custom) argument types can be supported through an implementation
	 * of {@link org.springframework.web.method.support.UriComponentsContributor}.
	 * @param method the target controller method
	 * @param argumentValues argument values matching to method parameters
	 * @return a UriComponentsBuilder instance
	 */
	public static UriComponentsBuilder fromMethod(Method method, Object... argumentValues) {
		UriComponentsBuilder builder = ServletUriComponentsBuilder.newInstance().path(getMethodRequestMapping(method));
		UriComponents uriComponents = applyContributors(builder, method, argumentValues);

		String typePath = getTypeRequestMapping(method.getDeclaringClass());
		String methodPath = uriComponents.getPath();
		String path = pathMatcher.combine(typePath, methodPath);

		return ServletUriComponentsBuilder.fromCurrentServletMapping().path(path).queryParams(uriComponents.getQueryParams());
	}

	/**
	 * Create a {@link UriComponents} by invoking a method on a "mock" controller, similar
	 * to how test frameworks provide mock objects and record method invocations.
	 * <p>For example given this controller:
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
	 * A "mock" controller can be used as follows:
	 * <pre class="code">
	 * // Inline style with static import of MvcUriComponentsBuilder.mock
	 *
	 * MvcUriComponentsBuilder.fromMethodCall(
	 * 		mock(CustomerController.class).showAddresses("US")).buildAndExpand(1);
	 *
	 * // Longer style for preparing multiple URIs and for void controller methods
	 *
	 * CustomerController controller = MvcUriComponentsBuilder.mock(CustomController.class);
	 * controller.addAddress(null);
	 *
	 * MvcUriComponentsBuilder.fromMethodCall(controller);
	 * </pre>
	 * The above  supports {@code @PathVariable} and {@code @RequestParam} method parameters.
	 * Any other arguments can be provided as {@literal null} and will be ignored.
	 * <p>Additional (custom) argument types can be supported through an implementation
	 * of {@link org.springframework.web.method.support.UriComponentsContributor}.
	 * @param methodInvocationInfo either the value returned from a "mock" controller
	 * invocation or the "mock" controller itself after an invocation
	 * @return a UriComponents instance
	 */
	public static UriComponentsBuilder fromMethodCall(Object methodInvocationInfo) {
		Assert.isInstanceOf(MethodInvocationInfo.class, methodInvocationInfo);
		MethodInvocationInfo info = (MethodInvocationInfo) methodInvocationInfo;

		Method method = info.getControllerMethod();
		Object[] argumentValues = info.getArgumentValues();

		UriComponentsBuilder builder = ServletUriComponentsBuilder.newInstance().path(getMethodRequestMapping(method));
		UriComponents uriComponents = applyContributors(builder, method, argumentValues);

		String typeMapping = getTypeRequestMapping(method.getDeclaringClass());
		String methodMapping = uriComponents.getPath();
		String path = pathMatcher.combine(typeMapping, methodMapping);

		return ServletUriComponentsBuilder.fromCurrentServletMapping().path(path).queryParams(uriComponents.getQueryParams());
	}


	private static String getTypeRequestMapping(Class<?> controllerType) {
		Assert.notNull(controllerType, "'controllerType' must not be null");
		RequestMapping annot = AnnotationUtils.findAnnotation(controllerType, RequestMapping.class);
		if ((annot == null) || ObjectUtils.isEmpty(annot.value()) || StringUtils.isEmpty(annot.value()[0])) {
			return "/";
		}
		if (annot.value().length > 1) {
			logger.warn("Multiple mappings on controller " + controllerType.getName() + ", using the first one");
		}
		return annot.value()[0];
	}

	private static String getMethodRequestMapping(Method method) {
		RequestMapping annot = AnnotationUtils.findAnnotation(method, RequestMapping.class);
		Assert.notNull(annot, "No @RequestMapping on: " + method.toGenericString());
		if (ObjectUtils.isEmpty(annot.value()) || StringUtils.isEmpty(annot.value()[0])) {
			return "/";
		}
		if (annot.value().length > 1) {
			logger.debug("Multiple mappings on method " + method.toGenericString() + ", using first one");
		}
		return annot.value()[0];
	}

	private static UriComponents applyContributors(UriComponentsBuilder builder, Method method, Object[] args) {
		CompositeUriComponentsContributor contributor = getConfiguredUriComponentsContributor();
		if (contributor == null) {
			logger.debug("Using default CompositeUriComponentsContributor");
			contributor = defaultUriComponentsContributor;
		}

		int paramCount = method.getParameterTypes().length;
		int argCount = args.length;
		Assert.isTrue(paramCount == argCount,  "Number of method parameters " + paramCount +
				" does not match number of argument values " + argCount);

		Map<String, Object> uriVars = new HashMap<String, Object>();
		for (int i=0; i < paramCount; i++) {
			MethodParameter param = new MethodParameter(method, i);
			param.initParameterNameDiscovery(parameterNameDiscoverer);
			contributor.contributeMethodArgument(param, args[i], builder, uriVars);
		}
		return builder.buildAndExpand(uriVars);
	}

	protected static CompositeUriComponentsContributor getConfiguredUriComponentsContributor() {
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
		WebApplicationContext wac = (WebApplicationContext) request.getAttribute(
				DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (wac == null) {
			logger.debug("No WebApplicationContext found: not in a DispatcherServlet request?");
			return null;
		}
		try {
			String beanName = MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME;
			return wac.getBean(beanName, CompositeUriComponentsContributor.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No CompositeUriComponentsContributor bean with name '" +
						MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME + "'");
			}
			return null;
		}
	}

	/**
	 * Return a "mock" controller instance. When an {@code @RequestMapping} method
	 * on the controller is invoked, the supplied argument values are remembered
	 * and the result can then be used to prepare a URL to the method via
	 * {@link #fromMethodCall(Object)}.
	 * <p>This is a shorthand version of {@link #controller(Class)} intended for
	 * inline use as follows:
	 * <pre class="code">
	 * UriComponentsBuilder builder = MvcUriComponentsBuilder.fromMethodCall(
	 * 		on(FooController.class).getFoo(1)).build();
	 * </pre>
	 * @param controllerType the target controller
	 */
	public static <T> T on(Class<T> controllerType) {
		return controller(controllerType);
	}

	/**
	 * Return a "mock" controller instance. When an {@code @RequestMapping} method
	 * on the controller is invoked, the supplied argument values are remembered
	 * and the result can then be used to prepare a URL to the method via
	 * {@link #fromMethodCall(Object)}.
	 * <p>This is a longer version of {@link #on(Class)} for use with void controller
	 * methods as well as for creating multiple links in succession.
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
		return initProxy(controllerType, new ControllerMethodInvocationInterceptor());
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
			enhancer.setInterfaces(new Class<?>[]{MethodInvocationInfo.class});
			enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);
			Factory factory = (Factory) objenesis.newInstance(enhancer.createClass());
			factory.setCallbacks(new Callback[] {interceptor});
			return (T) factory;
		}
	}


	private static class ControllerMethodInvocationInterceptor
			implements org.springframework.cglib.proxy.MethodInterceptor, MethodInterceptor {

		private static final Method getControllerMethod =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getControllerMethod");

		private static final Method getArgumentValues =
				ReflectionUtils.findMethod(MethodInvocationInfo.class, "getArgumentValues");

		private Method controllerMethod;

		private Object[] argumentValues;

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) {
			if (getControllerMethod.equals(method)) {
				return this.controllerMethod;
			}
			else if (getArgumentValues.equals(method)) {
				return this.argumentValues;
			}
			else if (ReflectionUtils.isObjectMethod(method)) {
				return ReflectionUtils.invokeMethod(method, obj, args);
			}
			else {
				this.controllerMethod = method;
				this.argumentValues = args;
				Class<?> returnType = method.getReturnType();
				return (void.class.equals(returnType) ? null : returnType.cast(initProxy(returnType, this)));
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
	}

}
