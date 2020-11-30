/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aopalliance.intercept;

import org.aopalliance.aop.Advice;

/**
 * This interface represents a generic interceptor.
 *
 * <p>A generic interceptor can intercept runtime events that occur
 * within a base program. Those events are materialized by (reified
 * in) joinpoints. Runtime joinpoints can be invocations, field
 * access, exceptions...
 *
 * <p>This interface is not used directly. Use the sub-interfaces
 * to intercept specific events. For instance, the following class
 * implements some specific interceptors in order to implement a
 * debugger:
 *
 * <pre class=code>
 * class DebuggingInterceptor implements MethodInterceptor,
 *     ConstructorInterceptor {
 *
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     debug(i.getMethod(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     debug(i.getConstructor(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   void debug(AccessibleObject ao, Object this, Object value) {
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 * @see Joinpoint
 * {
 *       拦截器标记型接口。拦截器可以在运行时拦截事件的调用，比如方法调用，属性访问，构造函数调用，异常抛出。
 *     这些事件都会用joinpoint实例来物化。
 *     	这个接口并没有直接被使用，而是使用具体的子类来拦截特定的事件，例如：
 *
 *     	class DebuggingInterceptor implements MethodInterceptor, ConstructorInterceptor {
 *
 *      Object invoke(MethodInvocation i) throws Throwable {
 *        debug(i.getMethod(), i.getThis(), i.getArgs());
 *        return i.proceed();
 *      }
 *
 *      Object construct(ConstructorInvocation i) throws Throwable {
 *        debug(i.getConstructor(), i.getThis(), i.getArgs());
 *        return i.proceed();
 *      }
 *
 *      void debug(AccessibleObject ao, Object this, Object value) {
 *        ...
 *      }
 *    }
 * }
 */
public interface Interceptor extends Advice {

}
