/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Test annotation to indicate that the annotated {@code public void}
 * method should be executed <em>after</em> a transaction is ended for test
 * methods configured to run within a transaction via the
 * {@code &#064;Transactional} annotation.
 * </p>
 * <p>
 * The {@code &#064;AfterTransaction} methods of superclasses will be
 * executed after those of the current class.
 * </p>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.transaction.annotation.Transactional
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AfterTransaction {
}
