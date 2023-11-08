/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.core.task;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for creating and configuring virtual threads in JDK 21.
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see VirtualThreadTaskExecutor
 */
public final class VirtualThreadDelegate {

    private final Thread.Builder threadBuilder = Thread.ofVirtual();

    /**
     * Create a virtual thread factory with default settings.
     *
     * @return a ThreadFactory for creating virtual threads
     */
    public ThreadFactory createVirtualThreadFactory() {
        return this.threadBuilder.factory();
    }

    /**
     * Create a virtual thread factory with a custom name prefix.
     *
     * @param threadNamePrefix the prefix for thread names
     * @return a ThreadFactory for creating virtual threads with custom names
     */
    public ThreadFactory createVirtualThreadFactory(String threadNamePrefix) {
        return this.threadBuilder.name(threadNamePrefix, 0).factory();
    }

    /**
     * Create a new virtual thread with a custom name and task.
     *
     * @param name the name for the virtual thread
     * @param task the Runnable task to be executed by the virtual thread
     * @return a new virtual thread
     */
    public Thread createVirtualThread(String name, Runnable task) {
        return this.threadBuilder.name(name).unstarted(task);
    }

    /**
     * Create a virtual thread factory with custom settings.
     *
     * @param threadNamePrefix the prefix for thread names
     * @param daemon whether the virtual threads should be daemon threads
     * @param priority the priority of the virtual threads
     * @param stackSize the stack size of the virtual threads
     * @param threadGroup the thread group for the virtual threads
     * @param virtualClock whether the virtual threads use virtual time
     * @param virtualCpu the virtual CPU time allotted to the virtual threads
     * @param virtualTimerTicks the virtual timer tick interval for the virtual threads
     * @param threadLifetime the maximum lifetime of the virtual threads
     * @param threadLifetimeUnit the time unit for the thread lifetime
     * @return a ThreadFactory for creating virtual threads with custom settings
     */
    public ThreadFactory createVirtualThreadFactory(String threadNamePrefix, boolean daemon, int priority,
                                                    long stackSize, ThreadGroup threadGroup, boolean virtualClock,
                                                    long virtualCpu, long virtualTimerTicks, long threadLifetime,
                                                    TimeUnit threadLifetimeUnit) {
        Thread.Builder builder = this.threadBuilder.name(threadNamePrefix)
                .daemon(daemon).priority(priority).stackSize(stackSize)
                .group(threadGroup).virtualClock(virtualClock)
                .virtualCpu(virtualCpu).virtualTimerTicks(virtualTimerTicks)
                .lifetime(threadLifetime, threadLifetimeUnit);
        return builder.factory();
    }
}
