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

package org.springframework.test.context.script;

import java.lang.annotation.*;

/**
 * Test annotation to execute SQL scripts before each test method.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SqlScripts {

	/**
	 * The resource location for Sql scripts.
	 *
	 * @return an array of sql scripts.
	 */
	String[] value() default {};

	/**
	 * Run the scripts within transaction.
	 *
	 * @return true if executing the script under the test transaction.
	 */
	boolean withinTransaction() default true;

	/**
	 * Datasource bean name when multiple datasource exist.
	 *
	 * @return a datasource bean name.
	 */
	String dataSource() default "";

}
