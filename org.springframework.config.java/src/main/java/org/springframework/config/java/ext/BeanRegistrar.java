package org.springframework.config.java.ext;

import static java.lang.String.*;
import static org.springframework.util.StringUtils.*;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.config.java.BeanDefinitionRegistrar;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ConfigurationClass;
import org.springframework.config.java.MalformedConfigurationException;
import org.springframework.config.java.ModelMethod;
import org.springframework.config.java.UsageError;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;


// TODO: SJC-242 document BeanHandler
// TODO: SJC-242 make package-private
class BeanRegistrar implements BeanDefinitionRegistrar {

	private static final Log logger = LogFactory.getLog(BeanRegistrar.class);

	/**
	 * Ensures that <var>member</var> is a method and is annotated (directly or indirectly)
	 * with {@link Bean @Bean}.
	 */
	public boolean accepts(Method method) {
		return AnnotationUtils.findAnnotation(method, Bean.class) != null;
	}

	// TODO: SJC-242 method too long
	public void register(ModelMethod method, BeanDefinitionRegistry registry) {
		RootBeanDefinition beanDef = new ConfigurationClassBeanDefinition();

		ConfigurationClass configClass = method.getDeclaringClass();

		beanDef.setFactoryBeanName(configClass.getBeanName());
		beanDef.setFactoryMethodName(method.getName());

		Bean bean = method.getRequiredAnnotation(Bean.class);

		Configuration defaults = configClass.getMetadata();

		// consider scoping
		beanDef.setScope(bean.scope());

		// consider autowiring
		if (bean.autowire() != AnnotationUtils.getDefaultValue(Bean.class, "autowire"))
			beanDef.setAutowireMode(bean.autowire().value());
		else if (defaults.defaultAutowire() != AnnotationUtils.getDefaultValue(Configuration.class,
		        "defaultAutowire"))
			beanDef.setAutowireMode(defaults.defaultAutowire().value());

		String beanName = method.getName();

		// has this already been overriden (i.e.: via XML)?
		if (containsBeanDefinitionIncludingAncestry(beanName, registry)) {
			BeanDefinition existingBeanDef = getBeanDefinitionIncludingAncestry(beanName, registry);

			// is the existing bean definition one that was created by JavaConfig?
			if (!(existingBeanDef instanceof ConfigurationClassBeanDefinition)) {
				// no -> then it's an external override, probably XML

				// ensure that overriding is ok
				if (bean.allowOverriding() == false) {
					UsageError error = configClass.new IllegalBeanOverrideError(null, method);
					throw new MalformedConfigurationException(error);
				}

				// overriding is legal, return immediately
				logger.info(format(
				        "Skipping loading bean definition for %s: a definition for bean '%s' already exists. "
				                + "This is likely due to an override in XML.", method, beanName));
				return;
			}
		}

		// propagate this bean's 'role' attribute
		beanDef.setRole(bean.role());

		// consider aliases
		for (String alias : bean.aliases())
			registry.registerAlias(beanName, alias);

		// TODO: re-enable for Lazy support
		// // is this bean marked as primary for disambiguation?
		// if (bean.primary() == Primary.TRUE)
		// beanDef.setPrimary(true);
		//
		// // is this bean lazily instantiated?
		// if ((bean.lazy() == Lazy.TRUE)
		// || ((bean.lazy() == Lazy.UNSPECIFIED) && (defaults.defaultLazy() == Lazy.TRUE)))
		// beanDef.setLazyInit(true);

		// does this bean have a custom init-method specified?
		String initMethodName = bean.initMethodName();
		if (hasText(initMethodName))
			beanDef.setInitMethodName(initMethodName);

		// does this bean have a custom destroy-method specified?
		String destroyMethodName = bean.destroyMethodName();
		if (hasText(destroyMethodName))
			beanDef.setDestroyMethodName(destroyMethodName);

		// TODO: re-enable for @ScopedProxy support
		// is this method annotated with @ScopedProxy?
		// ScopedProxy scopedProxy = method.getAnnotation(ScopedProxy.class);
		// if (scopedProxy != null) {
		// RootBeanDefinition targetDef = beanDef;
		//
		// // Create a scoped proxy definition for the original bean name,
		// // "hiding" the target bean in an internal target definition.
		// String targetBeanName =
		// ScopedProxy.Util.resolveHiddenScopedProxyBeanName(beanName);
		// RootBeanDefinition scopedProxyDefinition = new
		// RootBeanDefinition(ScopedProxyFactoryBean.class);
		// scopedProxyDefinition.getPropertyValues().addPropertyValue("targetBeanName",
		// targetBeanName);
		//
		// if (scopedProxy.proxyTargetClass())
		// targetDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE,
		// Boolean.TRUE);
		// // ScopedFactoryBean's "proxyTargetClass" default is TRUE, so we
		// // don't need to set it explicitly here.
		// else
		// scopedProxyDefinition.getPropertyValues().addPropertyValue("proxyTargetClass",
		// Boolean.FALSE);
		//
		// // The target bean should be ignored in favor of the scoped proxy.
		// targetDef.setAutowireCandidate(false);
		//
		// // Register the target bean as separate bean in the factory
		// registry.registerBeanDefinition(targetBeanName, targetDef);
		//
		// // replace the original bean definition with the target one
		// beanDef = scopedProxyDefinition;
		// }

		// TODO: re-enable for @Meta support
		// does this bean method have any @Meta annotations?
		// for (Meta meta : bean.meta())
		// beanDef.addMetadataAttribute(new BeanMetadataAttribute(meta.key(),
		// meta.value()));

		if (bean.dependsOn().length > 0)
			beanDef.setDependsOn(bean.dependsOn());

		logger.info(format("Registering bean definition for @Bean method %s.%s()", configClass.getName(),
		        beanName));

		registry.registerBeanDefinition(beanName, beanDef);

	}

	private boolean containsBeanDefinitionIncludingAncestry(String beanName, BeanDefinitionRegistry registry) {
		try {
			getBeanDefinitionIncludingAncestry(beanName, registry);
			return true;
		} catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	private BeanDefinition getBeanDefinitionIncludingAncestry(String beanName, BeanDefinitionRegistry registry) {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, registry);
		ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) registry;

		do {
			if (clbf.containsBeanDefinition(beanName))
				return registry.getBeanDefinition(beanName);

			BeanFactory parent = clbf.getParentBeanFactory();
			if (parent == null) {
				clbf = null;
			} else if (parent instanceof ConfigurableListableBeanFactory) {
				clbf = (ConfigurableListableBeanFactory) parent;
				// TODO: re-enable
				// } else if (parent instanceof AbstractApplicationContext) {
				// clbf = ((AbstractApplicationContext) parent).getBeanFactory();
			} else {
				throw new IllegalStateException("unknown parent type: " + parent.getClass().getName());
			}
		} while (clbf != null);

		throw new NoSuchBeanDefinitionException(format("No bean definition matching name '%s' "
		        + "could be found in %s or its ancestry", beanName, registry));
	}

}

/**
 * {@link RootBeanDefinition} marker subclass used to signify that a bean definition created
 * by JavaConfig as opposed to any other configuration source. Used in bean overriding cases
 * where it's necessary to determine whether the bean definition was created externally
 * (e.g. via XML).
 */
@SuppressWarnings("serial")
class ConfigurationClassBeanDefinition extends RootBeanDefinition {
}