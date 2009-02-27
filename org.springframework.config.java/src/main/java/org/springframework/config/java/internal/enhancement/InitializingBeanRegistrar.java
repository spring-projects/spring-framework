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
package org.springframework.config.java.internal.enhancement;

import java.lang.reflect.Method;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.config.java.annotation.BeanDefinitionRegistrar;
import org.springframework.config.java.model.ModelMethod;

class InitializingBeanRegistrar implements BeanDefinitionRegistrar {
    public boolean accepts(Method method) {
        return method.getName().equals("afterPropertiesSet")
            && method.getReturnType().equals(void.class);
    }
    
    public void register(ModelMethod method, BeanDefinitionRegistry registry) {
        // no-op
    }
}