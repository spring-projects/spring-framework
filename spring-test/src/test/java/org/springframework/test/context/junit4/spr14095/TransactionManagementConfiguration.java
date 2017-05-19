/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4.spr14095;

import org.springframework.context.annotation.Bean;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.ProxyTransactionManagementConfiguration;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Integration tests used to assess claims raised in
 * <a href="https://jira.spring.io/browse/SPR-14095" target="_blank">SPR-14095</a>.
 *
 * @author László Csontos
 * @since 4.3
 * @see AnnotationTransactionAttributeSourceTests
 */
@EnableTransactionManagement
public class TransactionManagementConfiguration extends ProxyTransactionManagementConfiguration implements TransactionManagementConfigurer {

    private static CallCountingTransactionManager transactionManager = new CallCountingTransactionManager();

    @Bean
    public TransactionalGroovyService testTransactionService() {
        return new TransactionalGroovyServiceImpl();
    }

    @Bean
    public CallCountingTransactionManager transactionManager() {
        return transactionManager;
    }

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return transactionManager();
    }

    @Override
    public TransactionAttributeSource transactionAttributeSource() {
        return new AnnotationTransactionAttributeSource();
    }

    @Override
    public TransactionInterceptor transactionInterceptor() {
        TransactionInterceptor interceptor = new TransactionInterceptor();

        interceptor.setTransactionAttributeSource(transactionAttributeSource());
        interceptor.setTransactionManager(transactionManager());

        return interceptor;
    }

}