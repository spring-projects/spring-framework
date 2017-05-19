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

import static org.junit.Assert.assertEquals;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.tests.transaction.CallCountingTransactionManager;

/**
 * Integration tests used to assess claims raised in
 * <a href="https://jira.spring.io/browse/SPR-14095" target="_blank">SPR-14095</a>.
 *
 * @author László Csontos
 * @since 4.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TransactionManagementConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class AnnotationTransactionAttributeSourceTests {

    @Autowired
    private TransactionalGroovyService transactionalGroovyService;

    @Autowired
    private CallCountingTransactionManager callCountingTransactionManager;

    @Before
    public void setUp() {
        callCountingTransactionManager.clear();
    }

    @Test
    public void testDoTransaction() {
        DefaultGroovyMethods.invokeMethod(transactionalGroovyService, "doTransaction", null);

        assertEquals(1, callCountingTransactionManager.commits);
        assertEquals(0, callCountingTransactionManager.rollbacks);
    }

    @Test
    public void testGetMetaClass() {
        DefaultGroovyMethods.getMetaClass(transactionalGroovyService);

        assertEquals(0, callCountingTransactionManager.commits);
        assertEquals(0, callCountingTransactionManager.rollbacks);
    }

}