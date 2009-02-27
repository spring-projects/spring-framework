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

import static java.lang.String.*;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodProxy;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Intercepts the invocation of any {@link Bean}-annotated methods in order to ensure proper
 * handling of bean semantics such as scoping and AOP proxying.
 * 
 * @see Bean
 * @see BeanRegistrar
 * 
 * @author Chris Beams
 */
class BeanMethodInterceptor extends AbstractMethodInterceptor {

    /**
     * Enhances a {@link Bean @Bean} method to check the supplied BeanFactory for the existence
     * of this bean object.
     */
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        String beanName = getBeanName(method);
        
        // TODO: re-enable for @ScopedProxy support
//        boolean isScopedProxy = (AnnotationUtils.findAnnotation(method, ScopedProxy.class) != null);
//
//        String scopedBeanName = ScopedProxy.Util.resolveHiddenScopedProxyBeanName(beanName);
//        if (isScopedProxy && beanFactory.isCurrentlyInCreation(scopedBeanName))
//            beanName = scopedBeanName;

        if (factoryContainsBean(beanName)) {
            // we have an already existing cached instance of this bean -> retrieve it
            Object cachedBean = beanFactory.getBean(beanName);
            if (log.isInfoEnabled())
                log.info(format("Returning cached singleton object [%s] for @Bean method %s.%s",
                                cachedBean, method.getDeclaringClass().getSimpleName(), beanName));

            return cachedBean;
        }

        return proxy.invokeSuper(obj, args);
    }

    /**
     * Check the beanFactory to see whether the bean named <var>beanName</var> already exists.
     * Accounts for the fact that the requested bean may be "in creation", i.e.: we're in the
     * middle of servicing the initial request for this bean. From JavaConfig's perspective,
     * this means that the bean does not actually yet exist, and that it is now our job to
     * create it for the first time by executing the logic in the corresponding Bean method.
     * <p>
     * Said another way, this check repurposes {@link ConfigurableBeanFactory#isCurrentlyInCreation(String)}
     * to determine whether the container is calling this method or the user is calling this method.
     *
     * @param   beanName  name of bean to check for
     *
     * @return  true if <var>beanName</var> already exists in beanFactory
     */
    private boolean factoryContainsBean(String beanName) {
        return beanFactory.containsBean(beanName)
                   && !beanFactory.isCurrentlyInCreation(beanName);
    }


}
