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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @BootstrapWith} defines class-level metadata that is used to determine
 * how to bootstrap the <em>Spring TestContext Framework</em>.
 *
 * <p>This annotation may also be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BootstrapWith {

	/**
	 * The {@link TestContextBootstrapper} to use to bootstrap the <em>Spring
	 * TestContext Framework</em>.
	 */
	Class<? extends TestContextBootstrapper> value() default TestContextBootstrapper.class;

}
