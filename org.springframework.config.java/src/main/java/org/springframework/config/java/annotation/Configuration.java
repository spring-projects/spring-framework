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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.config.java.ext.Bean;
import org.springframework.stereotype.Component;


/**
 * Annotation indicating that a class is a "Java Configuration" class, meaning that it exposes one
 * or more {@link Bean} methods. Holds similar information to that held in the default values of a
 * bean factory; can generally be thought of as the JavaConfig equivalent of XML's 'beans' root
 * element.
 *
 * <p>Note however that the information here is not used to populate the defaults of the owning bean
 * factory, which would affect other configurations. In the style of the Java configuration
 * mechanism generally, each Java configuration class is kept isolated.</p>
 *
 * <p>Configuration-annotated classes are also candidates for component scanning thanks to the fact
 * that this annotation is meta-annotated with {@link Component @Component}.</p>
 *
 * @author  Rod Johnson
 * @author  Chris Beams
 */
@Component
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configuration {

    /**
     * Configuration name. Allow different variants, such as test, production
     * etc. Default will always match.
     * @return
     */
    String[] names() default "";

    /**
     * Specifies the default autowiring strategy.
     *
     * @see Autowire
     * @return
     */
    Autowire defaultAutowire() default Autowire.INHERITED;

//    /**
//     * Dependency check strategy. By default, the dependency check is
//     * unspecified, that is the default Spring option will apply. In most cases,
//     * it means no dependency check will be done.
//     *
//     * @see DependencyCheck
//     * @return
//     */
//    DependencyCheck defaultDependencyCheck() default DependencyCheck.UNSPECIFIED;
//
//    /**
//     * Bean instantiation strategy. By default, it is unspecified.
//     *
//     * @see Lazy
//     * @return
//     */
//    Lazy defaultLazy() default Lazy.UNSPECIFIED;

    /**
     * Do we autowire with aspects from the enclosing factory scope?
     */
    boolean useFactoryAspects() default false;

    /**
     * Do we check {@link Required @Required} methods to make sure they've been
     * called?
     */
    boolean checkRequired() default false;

}
