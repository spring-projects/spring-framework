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

package org.springframework.ejb.interceptor;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.InvocationContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;

/**
 * EJB3-compliant interceptor class that injects Spring beans into
 * fields and methods which are annotated with {@code @Autowired}.
 * Performs injection after construction as well as after activation
 * of a passivated bean.
 *
 * <p>To be applied through an {@code @Interceptors} annotation in
 * the EJB Session Bean or Message-Driven Bean class, or through an
 * {@code interceptor-binding} XML element in the EJB deployment descriptor.
 *
 * <p>Delegates to Spring's {@link AutowiredAnnotationBeanPostProcessor}
 * underneath, allowing for customization of its specific settings through
 * overriding the {@link #configureBeanPostProcessor} template method.
 *
 * <p>The actual BeanFactory to obtain Spring beans from is determined
 * by the {@link #getBeanFactory} template method. The default implementation
 * obtains the Spring {@link ContextSingletonBeanFactoryLocator}, initialized
 * from the default resource location <strong>classpath*:beanRefContext.xml</strong>,
 * and obtains the single ApplicationContext defined there.
 *
 * <p><b>NOTE: If you have more than one shared ApplicationContext definition available
 * in your EJB class loader, you need to override the {@link #getBeanFactoryLocatorKey}
 * method and provide a specific locator key for each autowired EJB.</b>
 * Alternatively, override the {@link #getBeanFactory} template method and
 * obtain the target factory explicitly.
 *
 * <p><b>WARNING: Do not define the same bean as Spring-managed bean and as
 * EJB3 session bean in the same deployment unit.</b> In particular, be
 * careful when using the {@code &lt;context:component-scan&gt;} feature
 * in combination with the deployment of Spring-based EJB3 session beans:
 * Make sure that the EJB3 session beans are <i>not</i> autodetected as
 * Spring-managed beans as well, using appropriate package restrictions.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 * @see org.springframework.context.access.ContextSingletonBeanFactoryLocator
 * @see #getBeanFactoryLocatorKey
 * @see org.springframework.ejb.support.AbstractEnterpriseBean#setBeanFactoryLocator
 * @see org.springframework.ejb.support.AbstractEnterpriseBean#setBeanFactoryLocatorKey
 */
public class SpringBeanAutowiringInterceptor {

	/*
	 * We're keeping the BeanFactoryReference per target object in order to
	 * allow for using a shared interceptor instance on pooled target beans.
	 * This is not strictly necessary for EJB3 Session Beans and Message-Driven
	 * Beans, where interceptor instances get created per target bean instance.
	 * It simply protects against future usage of the interceptor in a shared scenario.
	 */
	private final Map<Object, BeanFactoryReference> beanFactoryReferences =
			new WeakHashMap<Object, BeanFactoryReference>();


	/**
	 * Autowire the target bean after construction as well as after passivation.
	 * @param invocationContext the EJB3 invocation context
	 */
	@PostConstruct
	@PostActivate
	public void autowireBean(InvocationContext invocationContext) {
		doAutowireBean(invocationContext.getTarget());
		try {
			invocationContext.proceed();
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			// Cannot declare a checked exception on WebSphere here - so we need to wrap.
			throw new EJBException(ex);
		}
	}

	/**
	 * Actually autowire the target bean after construction/passivation.
	 * @param target the target bean to autowire
	 */
	protected void doAutowireBean(Object target) {
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		configureBeanPostProcessor(bpp, target);
		bpp.setBeanFactory(getBeanFactory(target));
		bpp.processInjection(target);
	}

	/**
	 * Template method for configuring the
	 * {@link AutowiredAnnotationBeanPostProcessor} used for autowiring.
	 * @param processor the AutowiredAnnotationBeanPostProcessor to configure
	 * @param target the target bean to autowire with this processor
	 */
	protected void configureBeanPostProcessor(AutowiredAnnotationBeanPostProcessor processor, Object target) {
	}

	/**
	 * Determine the BeanFactory for autowiring the given target bean.
	 * @param target the target bean to autowire
	 * @return the BeanFactory to use (never {@code null})
	 * @see #getBeanFactoryReference
	 */
	protected BeanFactory getBeanFactory(Object target) {
		BeanFactory factory = getBeanFactoryReference(target).getFactory();
		if (factory instanceof ApplicationContext) {
			factory = ((ApplicationContext) factory).getAutowireCapableBeanFactory();
		}
		return factory;
	}

	/**
	 * Determine the BeanFactoryReference for the given target bean.
	 * <p>The default implementation delegates to {@link #getBeanFactoryLocator}
	 * and {@link #getBeanFactoryLocatorKey}.
	 * @param target the target bean to autowire
	 * @return the BeanFactoryReference to use (never {@code null})
	 * @see #getBeanFactoryLocator
	 * @see #getBeanFactoryLocatorKey
	 * @see org.springframework.beans.factory.access.BeanFactoryLocator#useBeanFactory(String)
	 */
	protected BeanFactoryReference getBeanFactoryReference(Object target) {
		String key = getBeanFactoryLocatorKey(target);
		BeanFactoryReference ref = getBeanFactoryLocator(target).useBeanFactory(key);
		this.beanFactoryReferences.put(target, ref);
		return ref;
	}

	/**
	 * Determine the BeanFactoryLocator to obtain the BeanFactoryReference from.
	 * <p>The default implementation exposes Spring's default
	 * {@link ContextSingletonBeanFactoryLocator}.
	 * @param target the target bean to autowire
	 * @return the BeanFactoryLocator to use (never {@code null})
	 * @see org.springframework.context.access.ContextSingletonBeanFactoryLocator#getInstance()
	 */
	protected BeanFactoryLocator getBeanFactoryLocator(Object target) {
		return ContextSingletonBeanFactoryLocator.getInstance();
	}

	/**
	 * Determine the BeanFactoryLocator key to use. This typically indicates
	 * the bean name of the ApplicationContext definition in
	 * <strong>classpath*:beanRefContext.xml</strong> resource files.
	 * <p>The default is {@code null}, indicating the single
	 * ApplicationContext defined in the locator. This must be overridden
	 * if more than one shared ApplicationContext definition is available.
	 * @param target the target bean to autowire
	 * @return the BeanFactoryLocator key to use (or {@code null} for
	 * referring to the single ApplicationContext defined in the locator)
	 */
	protected String getBeanFactoryLocatorKey(Object target) {
		return null;
	}


	/**
	 * Release the factory which has been used for autowiring the target bean.
	 * @param invocationContext the EJB3 invocation context
	 */
	@PreDestroy
	@PrePassivate
	public void releaseBean(InvocationContext invocationContext) {
		doReleaseBean(invocationContext.getTarget());
		try {
			invocationContext.proceed();
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			// Cannot declare a checked exception on WebSphere here - so we need to wrap.
			throw new EJBException(ex);
		}
	}

	/**
	 * Actually release the BeanFactoryReference for the given target bean.
	 * @param target the target bean to release
	 */
	protected void doReleaseBean(Object target) {
		BeanFactoryReference ref = this.beanFactoryReferences.remove(target);
		if (ref != null) {
			ref.release();
		}
	}

}
