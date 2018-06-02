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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that gives you the power to disable the job execution at all.
 *
 * <p>The annotated method must expect no arguments. It should return a value
 * of type {@code java.lang.Boolean} or just {@code boolean}. The return value {@code true} is interpreted as a signal
 * to continue the Schedule processing, {@code false} disables any job in the class. If null value is returned \
 * the {@link NullPointerException} will be thrown.
 *
 * <p>The @{@link BeforeSchedule} will affect all jobs in the class. It is called after all the injected fields are
 * initialized, so they all can be used to determinate whatever we need to start a job or not. It calls only once,
 * there is no way to disable already enabled job, and vise versa.
 *
 * @author Nikolai Bogdanov
 * @since 5.1
 * @see EnableScheduling
 * @see ScheduledAnnotationBeanPostProcessor
 * @see Scheduled
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeforeSchedule {
}
