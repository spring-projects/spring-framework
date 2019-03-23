/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.junit4.spr8849;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite to investigate claims raised in
 * <a href="https://jira.spring.io/browse/SPR-8849">SPR-8849</a>.
 *
 * <h3>Work Around</h3>
 * <p>By using a SpEL expression to generate a random {@code database-name}
 * for the embedded database (see {@code datasource-config.xml}), we ensure
 * that each {@code ApplicationContext} that imports the common configuration
 * will create an embedded database with a unique name.
 *
 * <p>To reproduce the problem mentioned in SPR-8849, delete the declaration
 * of the {@code database-name} attribute of the embedded database in
 * {@code datasource-config.xml} and run this <em>suite</em>.
 *
 * <h3>Solution</h3>
 * <p>As of Spring 4.2, a proper solution is possible thanks to SPR-8849.
 * {@link TestClass3} and {@link TestClass4} both import
 * {@code datasource-config-with-auto-generated-db-name.xml} which makes
 * use of the new {@code generate-name} attribute of {@code <jdbc:embedded-database>}.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@SuppressWarnings("javadoc")
@RunWith(Suite.class)
@SuiteClasses({ TestClass1.class, TestClass2.class, TestClass3.class, TestClass4.class })
public class Spr8849Tests {

}
