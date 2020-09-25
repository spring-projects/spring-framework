/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default object instantiation strategy for use in BeanFactories.
 *
 * <p>Uses CGLIB to generate subclasses dynamically if methods need to be
 * overridden by the container to implement <em>Method Injection</em>.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.1
 */
public class CglibSubclassingInstantiationStrategy extends SimpleInstantiationStrategy {

	/**
	 * Index in the CGLIB callback array for passthrough behavior,
	 * in which case the subclass won't override the original class.
	 */
	private static final int PASSTHROUGH = 0;

	/**
	 * Index in the CGLIB callback array for a method that should
	 * be overridden to provide <em>method lookup</em>.
	 */
	private static final int LOOKUP_OVERRIDE = 1;

	/**
	 * Index in the CGLIB callback array for a method that should
	 * be overridden using generic <em>method replacer</em> functionality.
	 */
	private static final int METHOD_REPLACER = 2;


	@Override
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		return instantiateWithMethodInjection(bd, beanName, owner, null);
	}

	@Override
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
			@Nullable Constructor<?> ctor, Object... args) {

		// Must generate CGLIB subclass...
		return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
	}


	/**
	 * An inner class created for historical reasons to avoid external CGLIB dependency
	 * in Spring versions earlier than 3.2.
	 */
	private static class CglibSubclassCreator {

		private static final Class<?>[] CALLBACK_TYPES = new Class<?>[]
				{NoOp.class, LookupOverrideMethodInterceptor.class, ReplaceOverrideMethodInterceptor.class};

		private final RootBeanDefinition beanDefinition;

		private final BeanFactory owner;

		CglibSubclassCreator(RootBeanDefinition beanDefinition, BeanFactory owner) {
			this.beanDefinition = beanDefinition;
			this.owner = owner;
		}

		/**
		 * Create a new instance of a dynamically generated subclass implementing the
		 * required lookups.
		 * @param ctor constructor to use. If this is {@code null}, use the
		 * no-arg constructor (no parameterization, or Setter Injection)
		 * @param args arguments to use for the constructor.
		 * Ignored if the {@code ctor} parameter is {@code null}.
		 * @return new instance of the dynamically generated subclass
		 */
		public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
			Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
			Object instance;
			if (ctor == null) {
				instance = BeanUtils.instantiateClass(subclass);
			}
			else {
				try {
					Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
					instance = enhancedSubclassConstructor.newInstance(args);
				}
				catch (Exception ex) {
					throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
							"Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
				}
			}
			// SPR-10785: set callbacks directly on the instance instead of in the
			// enhanced class (via the Enhancer) in order to avoid memory leaks.
			Factory factory = (Factory) instance;
			factory.setCallbacks(new Callback[] {NoOp.INSTANCE,
					new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
					new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)});
			return instance;
		}

		/**
		 * Create an enhanced subclass of the bean class for the provided bean
		 * definition, using CGLIB.
		 */
		private Class<?> createEnhancedSubclass(RootBeanDefinition beanDefinition) {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(beanDefinition.getBeanClass());
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			if (this.owner instanceof ConfigurableBeanFactory) {
				ClassLoader cl = ((ConfigurableBeanFactory) this.owner).getBeanClassLoader();
				enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(cl));
			}
			enhancer.setCallbackFilter(new MethodOverrideCallbackFilter(beanDefinition));
			enhancer.setCallbackTypes(CALLBACK_TYPES);
			return enhancer.createClass();
		}
	}


	/**
	 * Class providing hashCode and equals methods required by CGLIB to
	 * ensure that CGLIB doesn't generate a distinct class per bean.
	 * Identity is based on class and bean definition.
	 */
	private static class CglibIdentitySupport {

		private final RootBeanDefinition beanDefinition;

		public CglibIdentitySupport(RootBeanDefinition beanDefinition) {
			this.beanDefinition = beanDefinition;
		}

		public RootBeanDefinition getBeanDefinition() {
			return this.beanDefinition;
		}

		@Override
		public boolean equals(Object other) {
			return (other != null && getClass() == other.getClass() &&
					this.beanDefinition.equals(((CglibIdentitySupport) other).beanDefinition));
		}

		@Override
		public int hashCode() {
			return this.beanDefinition.hashCode();
		}
	}


	/**
	 * CGLIB GeneratorStrategy variant which exposes the application ClassLoader
	 * as thread context ClassLoader for the time of class generation
	 * (in order for ASM to pick it up when doing common superclass resolution).
	 */
	private static class ClassLoaderAwareGeneratorStrategy extends DefaultGeneratorStrategy {

		@Nullable
		private final ClassLoader classLoader;

		public ClassLoaderAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public byte[] generate(ClassGenerator cg) throws Exception {
			if (this.classLoader == null) {
				return super.generate(cg);
			}

			Thread currentThread = Thread.currentThread();
			ClassLoader threadContextClassLoader;
			try {
				threadContextClassLoader = currentThread.getContextClassLoader();
			}
			catch (Throwable ex) {
				// Cannot access thread context ClassLoader - falling back...
				return super.generate(cg);
			}

			boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
			if (overrideClassLoader) {
				currentThread.setContextClassLoader(this.classLoader);
			}
			try {
				return super.generate(cg);
			}
			finally {
				if (overrideClassLoader) {
					// Reset original thread context ClassLoader.
					currentThread.setContextClassLoader(threadContextClassLoader);
				}
			}
		}
	}


	/**
	 * CGLIB callback for filtering method interception behavior.
	 */
	private static class MethodOverrideCallbackFilter extends CglibIdentitySupport implements CallbackFilter {

		private static final Log logger = LogFactory.getLog(MethodOverrideCallbackFilter.class);

		public MethodOverrideCallbackFilter(RootBeanDefinition beanDefinition) {
			super(beanDefinition);
		}

		@Override
		public int accept(Method method) {
			MethodOverride methodOverride = getBeanDefinition().getMethodOverrides().getOverride(method);
			if (logger.isTraceEnabled()) {
				logger.trace("MethodOverride for " + method + ": " + methodOverride);
			}
			if (methodOverride == null) {
				return PASSTHROUGH;
			}
			else if (methodOverride instanceof LookupOverride) {
				return LOOKUP_OVERRIDE;
			}
			else if (methodOverride instanceof ReplaceOverride) {
				return METHOD_REPLACER;
			}
			throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
					methodOverride.getClass().getName());
		}
	}


	/**
	 * CGLIB MethodInterceptor to override methods, replacing them with an
	 * implementation that returns a bean looked up in the container.
	 */
	private static class LookupOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

		private final BeanFactory owner;

		public LookupOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
			super(beanDefinition);
			this.owner = owner;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
			// Cast is safe, as CallbackFilter filters are used selectively.
			LookupOverride lo = (LookupOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
			Assert.state(lo != null, "LookupOverride not found");
			Object[] argsToUse = (args.length > 0 ? args : null);  // if no-arg, don't insist on args at all
			if (StringUtils.hasText(lo.getBeanName())) {
				Object bean = (argsToUse != null ? this.owner.getBean(lo.getBeanName(), argsToUse) :
						this.owner.getBean(lo.getBeanName()));
				// Detect package-protected NullBean instance through equals(null) check
				return (bean.equals(null) ? null : bean);
			}
			else {
				return (argsToUse != null ? this.owner.getBean(method.getReturnType(), argsToUse) :
						this.owner.getBean(method.getReturnType()));
			}
		}
	}


	/**
	 * CGLIB MethodInterceptor to override methods, replacing them with a call
	 * to a generic MethodReplacer.
	 */
	private static class ReplaceOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

		private final BeanFactory owner;

		public ReplaceOverrideMethodInterceptor(RootBeanDefinition beanDefinition, BeanFactory owner) {
			super(beanDefinition);
			this.owner = owner;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
			ReplaceOverride ro = (ReplaceOverride) getBeanDefinition().getMethodOverrides().getOverride(method);
			Assert.state(ro != null, "ReplaceOverride not found");
			// TODO could cache if a singleton for minor performance optimization
			MethodReplacer mr = this.owner.getBean(ro.getMethodReplacerBeanName(), MethodReplacer.class);
			return mr.reimplement(obj, method, args);
		}
	}

}
