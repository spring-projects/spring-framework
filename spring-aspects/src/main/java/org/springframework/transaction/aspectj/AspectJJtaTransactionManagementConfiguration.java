/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurationSelector;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

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
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class AspectJJtaTransactionManagementConfiguration extends AspectJTransactionManagementConfiguration {

	@Bean(name = TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JtaAnnotationTransactionAspect jtaTransactionAspect(TransactionAttributeSource transactionAttributeSource) {
		JtaAnnotationTransactionAspect txAspect = JtaAnnotationTransactionAspect.aspectOf();
		txAspect.setTransactionAttributeSource(transactionAttributeSource);
		if (this.txManager != null) {
			txAspect.setTransactionManager(this.txManager);
		}
		return txAspect;
	}

}
