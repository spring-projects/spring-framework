/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.junit4.annotation.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Custom configuration annotation with meta-annotation attribute overrides for
 * {@link ContextConfiguration#classes} and {@link ActiveProfiles#profiles} and
 * <strong>no</strong> default configuration local to the composed annotation.
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@ContextConfiguration
@ActiveProfiles
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigClassesAndProfilesMetaConfig {

	Class<?>[] classes() default {};

	String[] profiles() default {};

}
