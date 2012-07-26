/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.transaction.annotation;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * Interface to be implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableTransactionManagement} that wish
 * or need to specify explicitly the {@link PlatformTransactionManager} bean to be used
 * for annotation-driven transaction management, as opposed to the default approach of a
 * by-type lookup. One reason this might be necessary is if there are two
 * {@code PlatformTransactionManager} beans present in the container.
 *
 * <p>See @{@link EnableTransactionManagement} for general examples and context; see
 * {@link #annotationDrivenTransactionManager()} for detailed instructions.
 *
 * <p>Note that in by-type lookup disambiguation cases, an alternative approach to
 * implementing this interface is to simply mark one of the offending {@code
 * PlatformTransactionManager} {@code @Bean} methods as @{@link
 * org.springframework.context.annotation.Primary Primary}.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 * @see org.springframework.context.annotation.Primary
 */
public interface TransactionManagementConfigurer {

	/**
	 * Return the transaction manager bean to use for annotation-driven database
	 * transaction management, i.e. when processing {@code @Transactional} methods.
	 *
	 * <p>There are two basic approaches to implementing this method:
	 * <h3>1. Implement the method and annotate it with {@code @Bean}</h3>
	 * In this case, the implementing {@code @Configuration} class implements this method,
	 * marks it with {@code @Bean} and configures and returns the transaction manager
	 * directly within the method body:
	 * <pre class="code">
	 * &#064;Bean
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }</pre>
	 * <h3>2. Implement the method without {@code @Bean} and delegate to another existing
	 * {@code @Bean} method</h3>
	 * <pre class="code">
	 * &#064;Bean
	 * public PlatformTransactionManager txManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }
	 *
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return txManager(); // reference the existing {@code @Bean} method above
	 * }</pre>
	 *
	 * If taking approach #2, be sure that <em>only one</em> of the methods is marked with
	 * {@code @Bean}!
	 *
	 * <p>In either scenario #1 or #2, it is important that the
	 * {@code PlatformTransactionManager} instance is managed as a Spring bean within the
	 * container as all {@code PlatformTransactionManager} implementations take
	 * advantage of Spring lifecycle callbacks such as {@code InitializingBean} and {@code
	 * BeanFactoryAware}.
	 */
	PlatformTransactionManager annotationDrivenTransactionManager();

}
