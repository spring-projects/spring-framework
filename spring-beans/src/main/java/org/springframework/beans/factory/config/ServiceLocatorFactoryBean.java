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

package org.springframework.beans.factory.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link FactoryBean} implementation that takes an interface which must have one or more
 * methods with the signatures {@code MyType xxx()} or {@code MyType xxx(MyIdType id)}
 * (typically, {@code MyService getService()} or {@code MyService getService(String id)})
 * and creates a dynamic proxy which implements that interface, delegating to an
 * underlying {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>Such service locators permit the decoupling of calling code from
 * the {@link org.springframework.beans.factory.BeanFactory} API, by using an
 * appropriate custom locator interface. They will typically be used for
 * <b>prototype beans</b>, i.e. for factory methods that are supposed to
 * return a new instance for each call. The client receives a reference to the
 * service locator via setter or constructor injection, to be able to invoke
 * the locator's factory methods on demand. <b>For singleton beans, direct
 * setter or constructor injection of the target bean is preferable.</b>
 *
 * <p>On invocation of the no-arg factory method, or the single-arg factory
 * method with a String id of {@code null} or empty String, if exactly
 * <b>one</b> bean in the factory matches the return type of the factory
 * method, that bean is returned, otherwise a
 * {@link org.springframework.beans.factory.NoSuchBeanDefinitionException}
 * is thrown.
 *
 * <p>On invocation of the single-arg factory method with a non-null (and
 * non-empty) argument, the proxy returns the result of a
 * {@link org.springframework.beans.factory.BeanFactory#getBean(String)} call,
 * using a stringified version of the passed-in id as bean name.
 *
 * <p>A factory method argument will usually be a String, but can also be an
 * int or a custom enumeration type, for example, stringified via
 * {@code toString}. The resulting String can be used as bean name as-is,
 * provided that corresponding beans are defined in the bean factory.
 * Alternatively, {@link #setServiceMappings(java.util.Properties) a custom mapping}
 * between service ids and bean names can be defined.
 *
 * <p>By way of an example, consider the following service locator interface.
 * Note that this interface is not dependant on any Spring APIs.
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService ();
 *}</pre>
 *
 * <p>A sample config in an XML-based
 * {@link org.springframework.beans.factory.BeanFactory} might look as follows:
 *
 * <pre class="code">&lt;beans>
 *
 *   &lt;!-- Prototype bean since we have state -->
 *   &lt;bean id="myService" class="a.b.c.MyService" singleton="false"/>
 *
 *   &lt;!-- will lookup the above 'myService' bean by *TYPE* -->
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean">
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/>
 *   &lt;/bean>
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean">
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/>
 *   &lt;/bean>
 *
 *&lt;/beans></pre>
 *
 * <p>The attendant {@code MyClientBean} class implementation might then
 * look something like this:
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the Spring container
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService();
 *        // use the service object to effect the business logic...
 *    }
 *}</pre>
 *
 * <p>By way of an example that looks up a bean <b>by name</b>, consider
 * the following service locator interface. Again, note that this
 * interface is not dependant on any Spring APIs.
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService (String serviceName);
 *}</pre>
 *
 * <p>A sample config in an XML-based
 * {@link org.springframework.beans.factory.BeanFactory} might look as follows:
 *
 * <pre class="code">&lt;beans>
 *
 *   &lt;!-- Prototype beans since we have state (both extend MyService) -->
 *   &lt;bean id="specialService" class="a.b.c.SpecialService" singleton="false"/>
 *   &lt;bean id="anotherService" class="a.b.c.AnotherService" singleton="false"/>
 *
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean">
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/>
 *   &lt;/bean>
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean">
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/>
 *   &lt;/bean>
 *
 *&lt;/beans></pre>
 *
 * <p>The attendant {@code MyClientBean} class implementation might then
 * look something like this:
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the Spring container
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("specialService");
 *        // use the service object to effect the business logic...
 *    }
 *
 *    public void anotherBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("anotherService");
 *        // use the service object to effect the business logic...
 *    }
 *}</pre>
 *
 * <p>See {@link ObjectFactoryCreatingFactoryBean} for an alternate approach.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #setServiceLocatorInterface
 * @see #setServiceMappings
 * @see ObjectFactoryCreatingFactoryBean
 */
public class ServiceLocatorFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

	private Class serviceLocatorInterface;

	private Constructor serviceLocatorExceptionConstructor;

	private Properties serviceMappings;

	private ListableBeanFactory beanFactory;

	private Object proxy;


	/**
	 * Set the service locator interface to use, which must have one or more methods with
	 * the signatures {@code MyType xxx()} or {@code MyType xxx(MyIdType id)}
	 * (typically, {@code MyService getService()} or {@code MyService getService(String id)}).
	 * See the {@link ServiceLocatorFactoryBean class-level Javadoc} for
	 * information on the semantics of such methods.
	 */
	public void setServiceLocatorInterface(Class interfaceType) {
		this.serviceLocatorInterface = interfaceType;
	}

	/**
	 * Set the exception class that the service locator should throw if service
	 * lookup failed. The specified exception class must have a constructor
	 * with one of the following parameter types: {@code (String, Throwable)}
	 * or {@code (Throwable)} or {@code (String)}.
	 * <p>If not specified, subclasses of Spring's BeansException will be thrown,
	 * for example NoSuchBeanDefinitionException. As those are unchecked, the
	 * caller does not need to handle them, so it might be acceptable that
	 * Spring exceptions get thrown as long as they are just handled generically.
	 * @see #determineServiceLocatorExceptionConstructor
	 * @see #createServiceLocatorException
	 */
	public void setServiceLocatorExceptionClass(Class serviceLocatorExceptionClass) {
		if (serviceLocatorExceptionClass != null && !Exception.class.isAssignableFrom(serviceLocatorExceptionClass)) {
			throw new IllegalArgumentException(
					"serviceLocatorException [" + serviceLocatorExceptionClass.getName() + "] is not a subclass of Exception");
		}
		this.serviceLocatorExceptionConstructor =
				determineServiceLocatorExceptionConstructor(serviceLocatorExceptionClass);
	}

	/**
	 * Set mappings between service ids (passed into the service locator)
	 * and bean names (in the bean factory). Service ids that are not defined
	 * here will be treated as bean names as-is.
	 * <p>The empty string as service id key defines the mapping for {@code null} and
	 * empty string, and for factory methods without parameter. If not defined,
	 * a single matching bean will be retrieved from the bean factory.
	 * @param serviceMappings mappings between service ids and bean names,
	 * with service ids as keys as bean names as values
	 */
	public void setServiceMappings(Properties serviceMappings) {
		this.serviceMappings = serviceMappings;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new FatalBeanException(
					"ServiceLocatorFactoryBean needs to run in a BeanFactory that is a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() {
		if (this.serviceLocatorInterface == null) {
			throw new IllegalArgumentException("Property 'serviceLocatorInterface' is required");
		}

		// Create service locator proxy.
		this.proxy = Proxy.newProxyInstance(
				this.serviceLocatorInterface.getClassLoader(),
				new Class[] {this.serviceLocatorInterface},
				new ServiceLocatorInvocationHandler());
	}


	/**
	 * Determine the constructor to use for the given service locator exception
	 * class. Only called in case of a custom service locator exception.
	 * <p>The default implementation looks for a constructor with one of the
	 * following parameter types: {@code (String, Throwable)}
	 * or {@code (Throwable)} or {@code (String)}.
	 * @param exceptionClass the exception class
	 * @return the constructor to use
	 * @see #setServiceLocatorExceptionClass
	 */
	protected Constructor determineServiceLocatorExceptionConstructor(Class exceptionClass) {
		try {
			return exceptionClass.getConstructor(new Class[] {String.class, Throwable.class});
		}
		catch (NoSuchMethodException ex) {
			try {
				return exceptionClass.getConstructor(new Class[] {Throwable.class});
			}
			catch (NoSuchMethodException ex2) {
				try {
					return exceptionClass.getConstructor(new Class[] {String.class});
				}
				catch (NoSuchMethodException ex3) {
					throw new IllegalArgumentException(
							"Service locator exception [" + exceptionClass.getName() +
							"] neither has a (String, Throwable) constructor nor a (String) constructor");
				}
			}
		}
	}

	/**
	 * Create a service locator exception for the given cause.
	 * Only called in case of a custom service locator exception.
	 * <p>The default implementation can handle all variations of
	 * message and exception arguments.
	 * @param exceptionConstructor the constructor to use
	 * @param cause the cause of the service lookup failure
	 * @return the service locator exception to throw
	 * @see #setServiceLocatorExceptionClass
	 */
	protected Exception createServiceLocatorException(Constructor exceptionConstructor, BeansException cause) {
		Class[] paramTypes = exceptionConstructor.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			if (paramTypes[i].equals(String.class)) {
				args[i] = cause.getMessage();
			}
			else if (paramTypes[i].isInstance(cause)) {
				args[i] = cause;
			}
		}
		return (Exception) BeanUtils.instantiateClass(exceptionConstructor, args);
	}


	public Object getObject() {
		return this.proxy;
	}

	public Class<?> getObjectType() {
		return this.serviceLocatorInterface;
	}

	public boolean isSingleton() {
		return true;
	}


	/**
	 * Invocation handler that delegates service locator calls to the bean factory.
	 */
	private class ServiceLocatorInvocationHandler implements InvocationHandler {

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				// Use hashCode of service locator proxy.
				return System.identityHashCode(proxy);
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "Service locator: " + serviceLocatorInterface.getName();
			}
			else {
				return invokeServiceLocatorMethod(method, args);
			}
		}

		@SuppressWarnings("unchecked")
		private Object invokeServiceLocatorMethod(Method method, Object[] args) throws Exception {
			Class serviceLocatorMethodReturnType = getServiceLocatorMethodReturnType(method);
			try {
				String beanName = tryGetBeanName(args);
				if (StringUtils.hasLength(beanName)) {
					// Service locator for a specific bean name.
					return beanFactory.getBean(beanName, serviceLocatorMethodReturnType);
				}
				else {
					// Service locator for a bean type.
					return BeanFactoryUtils.beanOfTypeIncludingAncestors(beanFactory, serviceLocatorMethodReturnType);
				}
			}
			catch (BeansException ex) {
				if (serviceLocatorExceptionConstructor != null) {
					throw createServiceLocatorException(serviceLocatorExceptionConstructor, ex);
				}
				throw ex;
			}
		}

		/**
		 * Check whether a service id was passed in.
		 */
		private String tryGetBeanName(Object[] args) {
			String beanName = "";
			if (args != null && args.length == 1 && args[0] != null) {
				beanName = args[0].toString();
			}
			// Look for explicit serviceId-to-beanName mappings.
			if (serviceMappings != null) {
				String mappedName = serviceMappings.getProperty(beanName);
				if (mappedName != null) {
					beanName = mappedName;
				}
			}
			return beanName;
		}

		private Class getServiceLocatorMethodReturnType(Method method) throws NoSuchMethodException {
			Class[] paramTypes = method.getParameterTypes();
			Method interfaceMethod = serviceLocatorInterface.getMethod(method.getName(), paramTypes);
			Class serviceLocatorReturnType = interfaceMethod.getReturnType();

			// Check whether the method is a valid service locator.
			if (paramTypes.length > 1 || void.class.equals(serviceLocatorReturnType)) {
				throw new UnsupportedOperationException(
						"May only call methods with signature '<type> xxx()' or '<type> xxx(<idtype> id)' " +
						"on factory interface, but tried to call: " + interfaceMethod);
			}
			return serviceLocatorReturnType;
		}
	}

}
