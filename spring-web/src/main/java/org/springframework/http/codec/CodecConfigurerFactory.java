/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;

/**
 * Internal delegate for loading the default codec configurer class names.
 * Models a loose relationship with the default implementations in the support
 * package, literally only needing to know the default class name to use.
 *
 * @author Juergen Hoeller
 * @since 5.0.1
 * @see ClientCodecConfigurer#create()
 * @see ServerCodecConfigurer#create()
 */
final class CodecConfigurerFactory {

	private static final String DEFAULT_CONFIGURERS_PATH = "CodecConfigurer.properties";

	private static final Map<Class<?>, Class<?>> defaultCodecConfigurers = new HashMap<>(4);

	static {
		try {
			Properties props = PropertiesLoaderUtils.loadProperties(
					new ClassPathResource(DEFAULT_CONFIGURERS_PATH, CodecConfigurerFactory.class));
			for (String ifcName : props.stringPropertyNames()) {
				String implName = props.getProperty(ifcName);
				Class<?> ifc = ClassUtils.forName(ifcName, CodecConfigurerFactory.class.getClassLoader());
				Class<?> impl = ClassUtils.forName(implName, CodecConfigurerFactory.class.getClassLoader());
				defaultCodecConfigurers.put(ifc, impl);
			}
		}
		catch (IOException | ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}
	}


	private CodecConfigurerFactory() {
	}


	@SuppressWarnings("unchecked")
	public static <T extends CodecConfigurer> T create(Class<T> ifc) {
		Class<?> impl = defaultCodecConfigurers.get(ifc);
		if (impl == null) {
			throw new IllegalStateException("No default codec configurer found for " + ifc);
		}
		return (T) BeanUtils.instantiateClass(impl);
	}

}
