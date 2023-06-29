package org.springframework.web.servlet.view;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * A {@link org.springframework.web.servlet.ViewResolver} implementation that uses
 * bean definitions from dedicated {@code @Configuration class} for view definitions.
 *
 * <p>Note: This {@code ViewResolver} implements the {@link Ordered} interface
 * in order to allow for flexible participation in {@code ViewResolver} chaining.
 * For example, some special views could be defined via this {@code ViewResolver}
 * (giving it 0 as "order" value), while all remaining views could be resolved by
 * a {@link UrlBasedViewResolver}.
 *
 * @author Marten Deinum
 * @since 6.1
 * @see BeanNameViewResolver
 */
public class JavaConfigViewResolver extends BeanNameViewResolver implements InitializingBean, DisposableBean {

	private final Set<Class<?>> viewConfigureationClasses = new LinkedHashSet<>();

	@Nullable
	private ConfigurableApplicationContext cachedFactory;

	public void register(Class<?>... viewConfigureationClasses) {
		Assert.notEmpty(viewConfigureationClasses, "At least one configuration class must be specified");
		Collections.addAll(this.viewConfigureationClasses, viewConfigureationClasses);
	}

	@Override
	protected synchronized BeanFactory initFactory() throws BeansException {
		if (this.cachedFactory != null) {
			return this.cachedFactory;
		}

		ApplicationContext applicationContext = obtainApplicationContext();

		// Create child ApplicationContext for views.
		AnnotationConfigWebApplicationContext factory = new AnnotationConfigWebApplicationContext();
		factory.setParent(applicationContext);
		factory.setServletContext(getServletContext());
		factory.register(viewConfigureationClasses.toArray(new Class<?>[0]));

		factory.refresh();
		this.cachedFactory = factory;
		return factory;

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initFactory();
	}

	@Override
	public void destroy() throws Exception {
		if (this.cachedFactory != null) {
			this.cachedFactory.close();
		}
	}
}
