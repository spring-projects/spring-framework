package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A registrar for handling {@code @BeanAliases} and {@code @BeanAlias} annotations.
 * This class registers aliases for beans defined via these annotations.
 *
 * <p>This class implements the {@link ImportBeanDefinitionRegistrar} interface to allow
 * custom processing of bean definitions during the import phase.
 *
 * @author Tiger Zhao
 * @since 7.0.0
 */
public class BeanAliasRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log logger = LogFactory.getLog(AnnotationBeanNameGenerator.class);

	@SuppressWarnings("unchecked")
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		// Register aliases from @BeanAliases annotation
		Map<String, Object> aliasesAttr = metadata.getAnnotationAttributes(BeanAliases.class.getName());
		if (null != aliasesAttr) {
			aliasesAttr.values().stream().flatMap(v -> Stream.of((Map<String, Object>[]) v)).forEach(attr -> registerAlias(registry, attr));
		}

		// Register alias from @BeanAlias annotation
		Map<String, Object> aliasAttr = metadata.getAnnotationAttributes(BeanAlias.class.getName());
		if (null != aliasAttr) {
			registerAlias(registry, aliasAttr);
		}
	}

	/**
	 * Registers an alias for a bean based on the provided attributes.
	 *
	 * @param registry   the bean definition registry to register the alias in
	 * @param attributes the attributes from the {@code @BeanAlias} or {@code @BeanAliases} annotation
	 */
	protected void registerAlias(BeanDefinitionRegistry registry, Map<String, Object> attributes) {
		String name = (String) attributes.get("name");
		Assert.hasLength(name, "name must not be empty");
		String[] aliases = (String[]) attributes.get("alias");
		Assert.notEmpty(aliases, "alias must not be empty");

		for (String alias : aliases) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("register bean alias for '%s': %s", name, alias));
			}
			registry.registerAlias(name, alias);
		}
	}

}
