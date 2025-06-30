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

package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionManager;

/**
 * Interface to be implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableTransactionManagement} that wish to
 * (or need to) explicitly specify the default {@code PlatformTransactionManager} bean
 * (or {@code ReactiveTransactionManager} bean) to be used for annotation-driven
 * transaction management, as opposed to the default approach of a by-type lookup.
 * One reason this might be necessary is if there are two {@code PlatformTransactionManager}
 * beans (or two {@code ReactiveTransactionManager} beans) present in the container.
 *
 * <p>See @{@link EnableTransactionManagement} for general examples and context;
 * see {@link #annotationDrivenTransactionManager()} for detailed instructions.
 *
 * <p><b>NOTE: A {@code TransactionManagementConfigurer} will get initialized early.</b>
 * Do not inject common dependencies into autowired fields directly; instead, consider
 * declaring a lazy {@link org.springframework.beans.factory.ObjectProvider} for those.
 *
 * <p>Note that in by-type lookup disambiguation cases, an alternative approach to
 * implementing this interface is to simply mark one of the offending
 * {@code PlatformTransactionManager} {@code @Bean} methods (or
 * {@code ReactiveTransactionManager} {@code @Bean} methods) as
 * {@link org.springframework.context.annotation.Primary @Primary}.
 * This is even generally preferred since it doesn't lead to early initialization
 * of the {@code TransactionManager} bean.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 * @see org.springframework.context.annotation.Primary
 * @see org.springframework.transaction.PlatformTransactionManager
 * @see org.springframework.transaction.ReactiveTransactionManager
 */
public interface TransactionManagementConfigurer {

	/**
	 * Return the default transaction manager bean to use for annotation-driven database
	 * transaction management, i.e. when processing {@code @Transactional} methods.
	 * <p>There are two basic approaches to implementing this method:
	 * <h4>1. Implement the method and annotate it with {@code @Bean}</h4>
	 * In this case, the implementing {@code @Configuration} class implements this method,
	 * marks it with {@code @Bean}, and configures and returns the transaction manager
	 * directly within the method body:
	 * <pre class="code">
	 * &#064;Bean
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }</pre>
	 * <h4>2. Implement the method without {@code @Bean} and delegate to another existing
	 * {@code @Bean} method</h4>
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
	 * If taking approach #2, be sure that <em>only one</em> of the methods is marked
	 * with {@code @Bean}!
	 * <p>In either scenario #1 or #2, it is important that the
	 * {@code PlatformTransactionManager} instance is managed as a Spring bean within the
	 * container since most {@code PlatformTransactionManager} implementations take advantage
	 * of Spring lifecycle callbacks such as {@code InitializingBean} and
	 * {@code BeanFactoryAware}. Note that the same guidelines apply to
	 * {@code ReactiveTransactionManager} beans.
	 * @return a {@link org.springframework.transaction.PlatformTransactionManager} or
	 * {@link org.springframework.transaction.ReactiveTransactionManager} implementation
	 */
	TransactionManager annotationDrivenTransactionManager();

}
