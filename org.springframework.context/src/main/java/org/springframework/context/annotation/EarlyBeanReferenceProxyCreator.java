/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Creates proxies for beans referenced from within @Feature methods.
 *
 * TODO SPR-7420: document
 * - discuss why proxies are important (avoiding side effects of early instantiation)
 * - discuss benefits of interface-based proxies over concrete proxies
 * - make it clear that both of the above are possible
 * - discuss invocation of @Bean methods and how they too return proxies.
 *   this 'proxy returning a proxy' approach can be confusing at first, but the
 *   implementation should help in making it clear.
 *
 * @author Chris Beams
 * @since 3.1
 */
class EarlyBeanReferenceProxyCreator {

	static final String FINAL_CLASS_ERROR_MESSAGE =
		"Cannot create subclass proxy for bean type %s because it is a final class. " +
		"Make the class non-final or inject the bean by interface rather than by concrete class.";

	static final String MISSING_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE =
		"Cannot create subclass proxy for bean type %s because it does not have a no-arg constructor. " +
		"Add a no-arg constructor or attempt to inject the bean by interface rather than by concrete class.";

	static final String PRIVATE_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE =
		"Cannot create subclass proxy for bean type %s because its no-arg constructor is private. " +
		"Increase the visibility of the no-arg constructor or attempt to inject the bean by interface rather " +
		"than by concrete class.";

	private final AutowireCapableBeanFactory beanFactory;


	/**
	 * Create a new proxy creator that will dereference proxy target beans against
	 * the given bean factory.
	 */
	public EarlyBeanReferenceProxyCreator(AutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Create a proxy that will ultimately dereference its target object using
	 * the given dependency descriptor.
	 */
	public Object createProxy(DependencyDescriptor descriptor) {
		return doCreateProxy(new ResolveDependencyTargetBeanDereferencingInterceptor(descriptor));
	}

	/**
	 * Create a proxy that looks up target beans using the given dereferencing interceptor.
	 *
	 * @see EarlyBeanReferenceProxy#dereferenceTargetBean()
	 */
	private Object doCreateProxy(TargetBeanDereferencingInterceptor targetBeanDereferencingInterceptor) {
		Enhancer enhancer = new Enhancer();
		Class<?> targetBeanType = targetBeanDereferencingInterceptor.getTargetBeanType();
		if (targetBeanType.isInterface()) {
			enhancer.setSuperclass(Object.class);
			enhancer.setInterfaces(new Class<?>[] {targetBeanType, EarlyBeanReferenceProxy.class});
		} else {
			assertClassIsProxyCapable(targetBeanType);
			enhancer.setSuperclass(targetBeanType);
			enhancer.setInterfaces(new Class<?>[] {EarlyBeanReferenceProxy.class});
		}
		enhancer.setCallbacks(new Callback[] {
			new BeanMethodInterceptor(),
			new ObjectMethodsInterceptor(),
			targetBeanDereferencingInterceptor,
			new TargetBeanDelegatingMethodInterceptor()
		});
		enhancer.setCallbackFilter(new CallbackFilter() {
			public int accept(Method method) {
				if (BeanAnnotationHelper.isBeanAnnotated(method)) {
					return 0;
				}
				if (ReflectionUtils.isObjectMethod(method)) {
					return 1;
				}
				if (method.getName().equals("dereferenceTargetBean")) {
					return 2;
				}
				return 3;
			}
		});
		return enhancer.create();
	}

	/**
	 * Return whether the given class is capable of being subclass proxied by CGLIB.
	 */
	private static void assertClassIsProxyCapable(Class<?> clazz) {
		Assert.isTrue(!clazz.isInterface(), "class parameter must be a concrete type");
		if ((clazz.getModifiers() & Modifier.FINAL) != 0) {
			throw new ProxyCreationException(String.format(FINAL_CLASS_ERROR_MESSAGE, clazz.getName()));
		}
		try {
			// attempt to retrieve the no-arg constructor for the class
			Constructor<?> noArgCtor = clazz.getDeclaredConstructor();
			if ((noArgCtor.getModifiers() & Modifier.PRIVATE) != 0) {
				throw new ProxyCreationException(String.format(PRIVATE_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE, clazz.getName()));
			}
		} catch (NoSuchMethodException ex) {
			throw new ProxyCreationException(String.format(MISSING_NO_ARG_CONSTRUCTOR_ERROR_MESSAGE, clazz.getName()));
		}
	}


	/**
	 * Interceptor for @Bean-annotated methods called from early-proxied bean instances, such as
	 * @Configuration class instances. Invoking instance methods on early-proxied beans usually
	 * causes an eager bean lookup, but in the case of @Bean methods, it is important to return
	 * a proxy.
	 */
	private class BeanMethodInterceptor implements MethodInterceptor {

		public Object intercept(Object obj, final Method beanMethod, Object[] args, MethodProxy proxy) throws Throwable {
			return doCreateProxy(new ByNameLookupTargetBeanDereferencingInterceptor(beanMethod));
		}

	}


	/**
	 * Interceptor for methods declared by java.lang.Object()
	 */
	private static class ObjectMethodsInterceptor implements MethodInterceptor {

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			if (method.getName().equals("toString")) {
				return String.format("EarlyBeanReferenceProxy for bean of type %s",
						obj.getClass().getSuperclass().getSimpleName());
			}
			if (method.getName().equals("hashCode")) {
				return System.identityHashCode(obj);
			}
			if (method.getName().equals("equals")) {
				return obj == args[0];
			}
			if (method.getName().equals("finalize")) {
				return null;
			}
			return proxy.invokeSuper(obj, args);
		}

	}


	/**
	 * Strategy interface allowing for various approaches to dereferencing (i.e. 'looking up')
	 * the target bean for an early bean reference proxy.
	 *
	 * @see EarlyBeanReferenceProxy#dereferenceTargetBean()
	 */
	private interface TargetBeanDereferencingInterceptor extends MethodInterceptor {
		Class<?> getTargetBeanType();
	}


	/**
	 * Interceptor that dereferences the target bean for the proxy by calling
	 * {@link AutowireCapableBeanFactory#resolveDependency(DependencyDescriptor, String)}.
	 *
	 * @see EarlyBeanReferenceProxy#dereferenceTargetBean()
	 */
	private class ResolveDependencyTargetBeanDereferencingInterceptor implements TargetBeanDereferencingInterceptor {

		private final DependencyDescriptor descriptor;

		public ResolveDependencyTargetBeanDereferencingInterceptor(DependencyDescriptor descriptor) {
			this.descriptor = descriptor;
		}

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			return beanFactory.resolveDependency(descriptor, null);
		}

		public Class<?> getTargetBeanType() {
			return this.descriptor.getDependencyType();
		}

	}


	/**
	 * Interceptor that dereferences the target bean for the proxy by calling BeanFactory#getBean(String).
	 *
	 * @see EarlyBeanReferenceProxy#dereferenceTargetBean()
	 */
	private class ByNameLookupTargetBeanDereferencingInterceptor implements TargetBeanDereferencingInterceptor {

		private final Method beanMethod;

		public ByNameLookupTargetBeanDereferencingInterceptor(Method beanMethod) {
			this.beanMethod = beanMethod;
		}

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			return beanFactory.getBean(BeanAnnotationHelper.determineBeanNameFor(beanMethod));
		}

		public Class<?> getTargetBeanType() {
			return beanMethod.getReturnType();
		}

	}


	/**
	 * Interceptor that dereferences the target bean for the proxy and delegates the
	 * current method call to it.
	 * @see TargetBeanDereferencingInterceptor
	 */
	private static class TargetBeanDelegatingMethodInterceptor implements MethodInterceptor {

		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Object targetBean = ((EarlyBeanReferenceProxy)obj).dereferenceTargetBean();
			return method.invoke(targetBean, args);
		}

	}

}

