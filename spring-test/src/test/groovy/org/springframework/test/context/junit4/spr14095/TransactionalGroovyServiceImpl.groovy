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

package org.springframework.test.context.junit4.spr14095

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Simple Groovy Service implementation.
 *
 * @author László Csontos
 * @since 4.3
 * @see AnnotationTransactionAttributeSourceTests
 */
@Service
@Transactional
class TransactionalGroovyServiceImpl implements TransactionalGroovyService {

    @Override
    void doTransaction() {
    }

}