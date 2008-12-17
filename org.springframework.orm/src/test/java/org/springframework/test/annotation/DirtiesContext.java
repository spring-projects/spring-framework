/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Test annotation to indicate that a test method <em>dirties</em> the context
 * for the current test.
 * </p>
 * <p>
 * Using this annotation in conjunction with
 * {@link AbstractAnnotationAwareTransactionalTests} is less error-prone than
 * calling
 * {@link org.springframework.test.AbstractSingleSpringContextTests#setDirty() setDirty()}
 * explicitly because the call to <code>setDirty()</code> is guaranteed to
 * occur, even if the test failed. If only a particular code path in the test
 * dirties the context, prefer calling <code>setDirty()</code> explicitly --
 * and take care!
 * </p>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see org.springframework.test.AbstractSingleSpringContextTests
 */
@Target( { ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DirtiesContext {

}
