/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.config.java.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.NoOp;

/**
 * Meta-annotation used to identify annotations as producers of beans and/or values.
 *
 * @author Chris Beams
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface Factory {

    /**
     * Specifies which registrar (if any) should be used to register
     * bean definitions for this {@link Factory} method.
     */
    Class<? extends BeanDefinitionRegistrar> registrarType();

    /**
     * Specifies what (if any) callback should be used when processing this {@link Factory} method.
     * Defaults to CGLIB's {@link NoOp}, which does nothing.
     * TODO: rename (interceptorType)?  to keep with the -or|-ar nomenclature
     */
    Class<? extends Callback> callbackType() default NoOp.class;

    /**
     * TODO: document
     * TODO: rename
     */
    Class<? extends Validator>[] validatorTypes() default {};
}
