/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Enables Spring's scheduled task execution capability, similar to
 * functionality found in Spring's {@code <task:*>} XML namespace. To be used
 * on {@link Configuration @Configuration} classes as follows:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     // various &#064;Bean definitions
 * }</pre>
 *
 * <p>This enables detection of {@link Scheduled @Scheduled} annotations on any
 * Spring-managed bean in the container. For example, given a class {@code MyTask}:
 *
 * <pre class="code">
 * package com.myco.tasks;
 *
 * public class MyTask {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }</pre>
 *
 * <p>the following configuration would ensure that {@code MyTask.work()} is called
 * once every 1000 ms:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyTask task() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>Alternatively, if {@code MyTask} were annotated with {@code @Component}, the
 * following configuration would ensure that its {@code @Scheduled} method is
 * invoked at the desired interval:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * &#064;ComponentScan(basePackages="com.myco.tasks")
 * public class AppConfig {
 * }</pre>
 *
 * <p>Methods annotated with {@code @Scheduled} may even be declared directly within
 * {@code @Configuration} classes:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig {
 *
 *     &#064;Scheduled(fixedRate=1000)
 *     public void work() {
 *         // task execution logic
 *     }
 * }</pre>
 *
 * <p>By default, Spring will search for an associated scheduler definition: either
 * a unique {@link org.springframework.scheduling.TaskScheduler} bean in the context,
 * or a {@code TaskScheduler} bean named "taskScheduler" otherwise; the same lookup
 * will also be performed for a {@link java.util.concurrent.ScheduledExecutorService}
 * bean. If neither of the two is resolvable, a local single-threaded default
 * scheduler will be created and used within the registrar.
 *
 * <p>When more control is desired, a {@code @Configuration} class may implement
 * {@link SchedulingConfigurer}. This allows access to the underlying
 * {@link ScheduledTaskRegistrar} instance. For example, the following example
 * demonstrates how to customize the {@link Executor} used to execute scheduled
 * tasks:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskExecutor());
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskExecutor() {
 *         return Executors.newScheduledThreadPool(100);
 *     }
 * }</pre>
 *
 * <p>Note in the example above the use of {@code @Bean(destroyMethod="shutdown")}.
 * This ensures that the task executor is properly shut down when the Spring
 * application context itself is closed.
 *
 * <p>Implementing {@code SchedulingConfigurer} also allows for fine-grained
 * control over task registration via the {@code ScheduledTaskRegistrar}.
 * For example, the following configures the execution of a particular bean
 * method per a custom {@code Trigger} implementation:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableScheduling
 * public class AppConfig implements SchedulingConfigurer {
 *
 *     &#064;Override
 *     public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
 *         taskRegistrar.setScheduler(taskScheduler());
 *         taskRegistrar.addTriggerTask(
 *             () -&gt; myTask().work(),
 *             new CustomTrigger()
 *         );
 *     }
 *
 *     &#064;Bean(destroyMethod="shutdown")
 *     public Executor taskScheduler() {
 *         return Executors.newScheduledThreadPool(42);
 *     }
 *
 *     &#064;Bean
 *     public MyTask myTask() {
 *         return new MyTask();
 *     }
 * }</pre>
 *
 * <p>For reference, the example above can be compared to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;task:annotation-driven scheduler="taskScheduler"/&gt;
 *
 *     &lt;task:scheduler id="taskScheduler" pool-size="42"/&gt;
 *
 *     &lt;task:scheduled-tasks scheduler="taskScheduler"&gt;
 *         &lt;task:scheduled ref="myTask" method="work" fixed-rate="1000"/&gt;
 *     &lt;/task:scheduled-tasks&gt;
 *
 *     &lt;bean id="myTask" class="com.foo.MyTask"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>The examples are equivalent save that in XML a <em>fixed-rate</em> period is used
 * instead of a custom <em>{@code Trigger}</em> implementation; this is because the
 * {@code task:} namespace {@code scheduled} cannot easily expose such support. This is
 * but one demonstration how the code-based approach allows for maximum configurability
 * through direct access to the actual component.
 *
 * <p><b>Note: {@code @EnableScheduling} applies to its local application context only,
 * allowing for selective scheduling of beans at different levels.</b> Please redeclare
 * {@code @EnableScheduling} in each individual context, e.g. the common root web
 * application context and any separate {@code DispatcherServlet} application contexts,
 * if you need to apply its behavior at multiple levels.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Scheduled
 * @see SchedulingConfiguration
 * @see SchedulingConfigurer
 * @see ScheduledTaskRegistrar
 * @see Trigger
 * @see ScheduledAnnotationBeanPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SchedulingConfiguration.class)
@Documented
public @interface EnableScheduling {

}
