/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.Map;
import javax.management.MBeanServer;
import javax.naming.NamingException;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.jmx.support.WebSphereMBeanServerFactoryBean;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@code @Configuration} class that registers a {@link AnnotationMBeanExporter} bean.
 *
 * <p>This configuration class is automatically imported when using the
 * {@link EnableMBeanExport} annotation. See its javadoc for complete usage details.
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @since 3.2
 * @see EnableMBeanExport
 */
@Configuration
public class MBeanExportConfiguration implements ImportAware, EnvironmentAware, BeanFactoryAware {

	private static final String MBEAN_EXPORTER_BEAN_NAME = "mbeanExporter";

	@Nullable
	private AnnotationAttributes enableMBeanExport;

	@Nullable
	private Environment environment;

	@Nullable
	private BeanFactory beanFactory;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableMBeanExport.class.getName());
		this.enableMBeanExport = AnnotationAttributes.fromMap(map);
		if (this.enableMBeanExport == null) {
			throw new IllegalArgumentException(
					"@EnableMBeanExport is not present on importing class " + importMetadata.getClassName());
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Bean(name = MBEAN_EXPORTER_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public AnnotationMBeanExporter mbeanExporter() {
		AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
		Assert.state(this.enableMBeanExport != null, "No EnableMBeanExport annotation found");
		setupDomain(exporter, this.enableMBeanExport);
		setupServer(exporter, this.enableMBeanExport);
		setupRegistrationPolicy(exporter, this.enableMBeanExport);
		return exporter;
	}

	private void setupDomain(AnnotationMBeanExporter exporter, AnnotationAttributes enableMBeanExport) {
		String defaultDomain = enableMBeanExport.getString("defaultDomain");
		if (StringUtils.hasLength(defaultDomain) && this.environment != null) {
			defaultDomain = this.environment.resolvePlaceholders(defaultDomain);
		}
		if (StringUtils.hasText(defaultDomain)) {
			exporter.setDefaultDomain(defaultDomain);
		}
	}

	private void setupServer(AnnotationMBeanExporter exporter, AnnotationAttributes enableMBeanExport) {
		String server = enableMBeanExport.getString("server");
		if (StringUtils.hasLength(server) && this.environment != null) {
			server = this.environment.resolvePlaceholders(server);
		}
		if (StringUtils.hasText(server)) {
			Assert.state(this.beanFactory != null, "No BeanFactory set");
			exporter.setServer(this.beanFactory.getBean(server, MBeanServer.class));
		}
		else {
			SpecificPlatform specificPlatform = SpecificPlatform.get();
			if (specificPlatform != null) {
				MBeanServer mbeanServer = specificPlatform.getMBeanServer();
				if (mbeanServer != null) {
					exporter.setServer(mbeanServer);
				}
			}
		}
	}

	private void setupRegistrationPolicy(AnnotationMBeanExporter exporter, AnnotationAttributes enableMBeanExport) {
		RegistrationPolicy registrationPolicy = enableMBeanExport.getEnum("registration");
		exporter.setRegistrationPolicy(registrationPolicy);
	}


	public enum SpecificPlatform {

		WEBLOGIC("weblogic.management.Helper") {
			@Override
			public MBeanServer getMBeanServer() {
				try {
					return new JndiLocatorDelegate().lookup("java:comp/env/jmx/runtime", MBeanServer.class);
				}
				catch (NamingException ex) {
					throw new MBeanServerNotFoundException("Failed to retrieve WebLogic MBeanServer from JNDI", ex);
				}
			}
		},

		WEBSPHERE("com.ibm.websphere.management.AdminServiceFactory") {
			@Override
			public MBeanServer getMBeanServer() {
				WebSphereMBeanServerFactoryBean fb = new WebSphereMBeanServerFactoryBean();
				fb.afterPropertiesSet();
				return fb.getObject();
			}
		};

		private final String identifyingClass;

		SpecificPlatform(String identifyingClass) {
			this.identifyingClass = identifyingClass;
		}

		@Nullable
		public abstract MBeanServer getMBeanServer();

		@Nullable
		public static SpecificPlatform get() {
			ClassLoader classLoader = MBeanExportConfiguration.class.getClassLoader();
			for (SpecificPlatform environment : values()) {
				if (ClassUtils.isPresent(environment.identifyingClass, classLoader)) {
					return environment;
				}
			}
			return null;
		}
	}

}
