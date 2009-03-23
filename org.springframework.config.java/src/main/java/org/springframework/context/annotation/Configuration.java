/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.support.ConfigurationClassPostProcessor;
import org.springframework.stereotype.Component;


/**
 * Indicates that a class declares one or more {@link Bean} methods and may be processed
 * by the Spring container to generate bean definitions and service requests for those beans
 * at runtime.
 * 
 * <p>Configuration is itself annotated as a {@link Component}, therefore Configuration
 * classes are candidates for component-scanning and may also take advantage of
 * {@link Autowire} at the field and method and constructor level.
 * 
 * <p>May be used in conjunction with the {@link Lazy} annotation to indicate that all Bean
 * methods declared within this class are by default lazily initialized.
 * 
 * <h3>Constraints</h3>
 * <ul>
 *    <li>Configuration classes must be non-final
 *    <li>Configuration classes must be non-local (may not be declared within a method)
 *    <li>Configuration classes must have a default/no-arg constructor or an
 *        {@link Autowired} constructor
 * </ul>
 * 
 * 
 * @author Rod Johnson
 * @author Chris Beams
 * @since 3.0
 * @see ConfigurationClassPostProcessor
 * @see Bean
 * @see Lazy
 */
@Component
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configuration {

}

// TODO: test constructor autowiring<br>
// TODO: test private Configuration classes<br>
// TODO: test @Lazy @Configuration<br>
