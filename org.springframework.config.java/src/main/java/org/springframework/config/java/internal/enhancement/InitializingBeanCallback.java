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

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

// TODO: should not be necessary to make configurations implement InitializingBean anymore once @Value is in the picture
class InitializingBeanCallback implements MethodInterceptor {

    private final DefaultListableBeanFactory beanFactory;

    public InitializingBeanCallback(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
    
        // TODO: SJC-242 ExternalValueInjector - revisit this line (inline method?)
    	// TODO: should be handled by @Value
        //new ExternalValueInjector(beanFactory).injectExternalValues(obj);
    
        // only call the superclass afterPropertiesSet method if it is actually implemented
        if(!InitializingBean.class.equals(method.getDeclaringClass()))
            return proxy.invokeSuper(obj, args);
        
        return Void.TYPE;
    }
}