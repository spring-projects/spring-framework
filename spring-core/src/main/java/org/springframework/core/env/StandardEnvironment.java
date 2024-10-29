/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.env;

/**
 * {@link Environment} implementation suitable for use in 'standard' (i.e. non-web)
 * applications.
 *
 * <p>In addition to the usual functions of a {@link ConfigurableEnvironment} such as
 * property resolution and profile-related operations, this implementation configures two
 * default property sources, to be searched in the following order:
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
 * </ul>
 * <p>
 * That is, if the key "xyz" is present both in the JVM system properties as well as in
 * the set of environment variables for the current process, the value of key "xyz" from
 * system properties will return from a call to {@code environment.getProperty("xyz")}.
 * This ordering is chosen by default because system properties are per-JVM, while
 * environment variables may be the same across many JVMs on a given system.  Giving
 * system properties precedence allows for overriding of environment variables on a
 * per-JVM basis.
 *
 * <p>These default property sources may be removed, reordered, or replaced; and
 * additional property sources may be added using the {@link MutablePropertySources}
 * instance available from {@link #getPropertySources()}. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples.
 *
 * <p>See {@link SystemEnvironmentPropertySource} javadoc for details on special handling
 * of property names in shell environments (e.g. Bash) that disallow period characters in
 * variable names.
 * 适用于“标准”（即非web）应用程序的环境实现。
 * 除了ConfigurableEnvironment的常见功能（如属性解析和配置文件相关操作）外，此实现还配置了两个默认属性源，按以下顺序进行搜索：
 * 系统属性
 * 系统环境变量
 * 也就是说，如果键“xyz”既存在于JVM系统属性中，也存在于当前进程的环境变量集中，则系统属性中的键“xyz”的值将从对environment.getProperty的调用中返回。
 * 默认情况下选择此顺序是因为系统属性是针对每个JVM的，而给定系统上的许多JVM上的环境变量可能是相同的。赋予系统属性优先权允许在每个JVM的基础上重写环境变量。
 * 这些默认属性源可能会被删除、重新排序或替换；并且可以使用getPropertySources（）中提供的MutablePropertySources实例添加其他属性源。
 * 有关使用示例，请参阅ConfigurableEnvironment Javadoc。
 * 有关shell环境（如Bash）中不允许变量名中使用句点字符的属性名的特殊处理的详细信息，请参阅SystemEnvironmentPropertySource javadoc。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @see org.springframework.web.context.support.StandardServletEnvironment
 * @since 3.1
 * @since 3.1
 */
public class StandardEnvironment extends AbstractEnvironment {

	/**
	 * System environment property source name: {@value}.
	 * 系统环境属性源名称：{@value}
	 */
	public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

	/**
	 * JVM system properties property source name: {@value}.
	 * JVM系统属性属性源名称：{@value}
	 */
	public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";


	/**
	 * Create a new {@code StandardEnvironment} instance with a default
	 * {@link MutablePropertySources} instance.
	 * 使用默认的{@link MutablePropertySources}实例创建新的{@code StandardEnvironment}实例。
	 */
	public StandardEnvironment() {
	}

	/**
	 * Create a new {@code StandardEnvironment} instance with a specific
	 * {@link MutablePropertySources} instance.
	 * 使用默认的{@link MutablePropertySources}实例创建新的{@code StandardEnvironment}实例。
	 *
	 * @param propertySources property sources to use
	 * @since 5.3.4
	 */
	protected StandardEnvironment(MutablePropertySources propertySources) {
		super(propertySources);
	}


	/**
	 * Customize the set of property sources with those appropriate for any standard
	 * Java environment:
	 * <ul>
	 * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
	 * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
	 * </ul>
	 * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
	 * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
	 *
	 * @see AbstractEnvironment#customizePropertySources(MutablePropertySources)
	 * @see #getSystemProperties()
	 * @see #getSystemEnvironment()
	 */
	@Override
	protected void customizePropertySources(MutablePropertySources propertySources) {
		propertySources.addLast(
				new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
		propertySources.addLast(
				new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
	}

}
