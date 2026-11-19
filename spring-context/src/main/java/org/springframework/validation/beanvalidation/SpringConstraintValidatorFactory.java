/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.validation.beanvalidation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JSR-303 {@link ConstraintValidatorFactory} implementation that delegates to a
 * Spring BeanFactory for creating autowired {@link ConstraintValidator} instances.
 *
 * <p>Note that this class is meant for programmatic use, not for declarative use
 * in a standard {@code validation.xml} file. Consider
 * {@link org.springframework.web.bind.support.SpringWebConstraintValidatorFactory}
 * for declarative use in a web application, for example, with JAX-RS or JAX-WS.
 *
 * <p>When running with AOT-generated artifacts (for example, in a native image),
 * the annotation-driven injection post-processors are not registered in the
 * BeanFactory. This factory therefore applies {@code @Autowired}, {@code @Value},
 * {@code @Inject} and {@code @Resource} injection to created validators itself.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean(Class)
 * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
 */
public class SpringConstraintValidatorFactory implements ConstraintValidatorFactory {

	private static final boolean JAKARTA_ANNOTATIONS_PRESENT = ClassUtils.isPresent(
			"jakarta.annotation.Resource", SpringConstraintValidatorFactory.class.getClassLoader());


	private final AutowireCapableBeanFactory beanFactory;

	private final @Nullable ConstraintValidatorFactory defaultConstraintValidatorFactory;

	private final @Nullable AotInjectionDelegate aotInjectionDelegate;


	/**
	 * Create a new SpringConstraintValidatorFactory for the given BeanFactory.
	 * @param beanFactory the target BeanFactory
	 */
	public SpringConstraintValidatorFactory(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.defaultConstraintValidatorFactory = null;
		this.aotInjectionDelegate = AotInjectionDelegate.createIfNecessary(beanFactory);
	}

	/**
	 * Create a new SpringConstraintValidatorFactory for the given BeanFactory.
	 * @param beanFactory the target BeanFactory
	 * @param defaultConstraintValidatorFactory the default ConstraintValidatorFactory
	 * as exposed by the validation provider (for creating provider-internal validator
	 * implementations which might not be publicly accessible in a module path setup)
	 * @since 7.0.3
	 */
	public SpringConstraintValidatorFactory(
			AutowireCapableBeanFactory beanFactory, ConstraintValidatorFactory defaultConstraintValidatorFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.defaultConstraintValidatorFactory = defaultConstraintValidatorFactory;
		this.aotInjectionDelegate = AotInjectionDelegate.createIfNecessary(beanFactory);
	}


	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		if (this.defaultConstraintValidatorFactory != null) {
			// Create provider-internal validator implementations through default ConstraintValidatorFactory.
			String providerModuleName = this.defaultConstraintValidatorFactory.getClass().getModule().getName();
			if (providerModuleName != null && providerModuleName.equals(key.getModule().getName())) {
				return this.defaultConstraintValidatorFactory.getInstance(key);
			}
		}
		T instance = this.beanFactory.createBean(key);
		if (this.aotInjectionDelegate != null) {
			this.aotInjectionDelegate.processInjection(instance);
		}
		return instance;
	}

	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		this.beanFactory.destroyBean(instance);
	}


	/**
	 * Delegate that applies annotation-driven injection to validator instances when running
	 * with AOT-generated artifacts, since the corresponding post-processors are
	 * excluded from the AOT-optimized BeanFactory.
	 */
	private static final class AotInjectionDelegate {

		private final AutowiredAnnotationBeanPostProcessor autowiredProcessor;

		private final @Nullable CommonAnnotationBeanPostProcessor commonProcessor;

		private AotInjectionDelegate(ConfigurableListableBeanFactory beanFactory) {
			this.autowiredProcessor = new AutowiredAnnotationBeanPostProcessor();
			this.autowiredProcessor.setBeanFactory(beanFactory);
			if (JAKARTA_ANNOTATIONS_PRESENT) {
				this.commonProcessor = new CommonAnnotationBeanPostProcessor();
				this.commonProcessor.setBeanFactory(beanFactory);
			}
			else {
				this.commonProcessor = null;
			}
		}

		void processInjection(Object instance) {
			this.autowiredProcessor.processInjection(instance);
			if (this.commonProcessor != null) {
				this.commonProcessor.processInjection(instance);
			}
		}

		static @Nullable AotInjectionDelegate createIfNecessary(AutowireCapableBeanFactory beanFactory) {
			if (AotDetector.useGeneratedArtifacts() &&
					beanFactory instanceof ConfigurableListableBeanFactory clbf &&
					!hasAutowiredAnnotationProcessor(clbf)) {
				return new AotInjectionDelegate(clbf);
			}
			return null;
		}

		private static boolean hasAutowiredAnnotationProcessor(ConfigurableListableBeanFactory beanFactory) {
			return (beanFactory.getBeanNamesForType(
					AutowiredAnnotationBeanPostProcessor.class, true, false).length > 0);
		}
	}

}
