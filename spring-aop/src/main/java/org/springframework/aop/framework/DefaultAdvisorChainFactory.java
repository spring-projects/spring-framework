/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.*;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AdvisorChainFactory 默认实现类
 *
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

    @Override
    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
            Advised config, Method method, @Nullable Class<?> targetClass) {

        // This is somewhat tricky... We have to process introductions first,
        // but we need to preserve order in the ultimate list.
        // 获得 DefaultAdvisorAdapterRegistry 对象
        AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
        Advisor[] advisors = config.getAdvisors(); // 该类可筛选的 Advisor 集合，需要进过下面逻辑的筛选，再创建成对应的拦截器，添加到 interceptorList 中。
        List<Object> interceptorList = new ArrayList<>(advisors.length); // 拦截器的结果集合
        Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass()); // 真实的类
        Boolean hasIntroductions = null; // 是否 advisors 中有 IntroductionAdvisor 对象。

        // 遍历 Advisor 集合
        for (Advisor advisor : advisors) {
            if (advisor instanceof PointcutAdvisor) {
                // Add it conditionally.
                PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
                if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
                    MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
                    // 判断是否匹配
                    boolean match;
                    if (mm instanceof IntroductionAwareMethodMatcher) {
                        if (hasIntroductions == null) {
                            hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
                        }
                        match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
                    } else {
                        match = mm.matches(method, actualClass);
                    }
                    if (match) {
                        MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
                        if (mm.isRuntime()) { // 运行时，封装成对应的 InterceptorAndDynamicMethodMatcher 拦截器对象。TODO 芋艿，暂时没详细调试
                            // Creating a new object instance in the getInterceptors() method
                            // isn't a problem as we normally cache created chains.
                            for (MethodInterceptor interceptor : interceptors) {
                                interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
                            }
                        } else { // 非运行时，直接添加
                            interceptorList.addAll(Arrays.asList(interceptors));
                        }
                    }
                }
            } else if (advisor instanceof IntroductionAdvisor) { // TODO 芋艿，后面在调试
                IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
                if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
                    Interceptor[] interceptors = registry.getInterceptors(advisor);
                    interceptorList.addAll(Arrays.asList(interceptors));
                }
            } else { // TODO 芋艿，暂时未调试到这个情况
                Interceptor[] interceptors = registry.getInterceptors(advisor);
                interceptorList.addAll(Arrays.asList(interceptors));
            }
        }

        return interceptorList;
    }

    /**
     * Determine whether the Advisors contain matching introductions.
     */
    private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
        for (Advisor advisor : advisors) {
            if (advisor instanceof IntroductionAdvisor) {
                IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
                if (ia.getClassFilter().matches(actualClass)) {
                    return true;
                }
            }
        }
        return false;
    }

}
