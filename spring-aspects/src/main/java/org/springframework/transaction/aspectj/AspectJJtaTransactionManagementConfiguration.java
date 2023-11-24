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

package org.springframework.transaction.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurationSelector;
import org.springframework.transaction.config.TransactionManagementConfigUtils;

/**
 * {@code @Configuration} class that registers the Spring infrastructure beans necessary
 * to enable AspectJ-based annotation-driven transaction management for the JTA 1.2
 * {@link jakarta.transaction.Transactional} annotation in addition to Spring's own
 * {@link org.springframework.transaction.annotation.Transactional} annotation.
 *
 * @author Juergen Hoeller
 * @since 5.1
 * @see EnableTransactionManagement
 * @see TransactionManagementConfigurationSelector
 */
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJJtaTransactionManagementConfiguration extends AspectJTransactionManagementConfiguration {

	@Bean(name = TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JtaAnnotationTransactionAspect jtaTransactionAspect() {
		JtaAnnotationTransactionAspect txAspect = JtaAnnotationTransactionAspect.aspectOf();
		if (this.txManager != null) {
			txAspect.setTransactionManager(this.txManager);
		}
		return txAspect;
	}

}
