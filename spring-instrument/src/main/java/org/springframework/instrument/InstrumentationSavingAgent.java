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

package org.springframework.instrument;

import java.lang.instrument.Instrumentation;

/**
 * Java agent that saves the {@link Instrumentation} interface from the JVM
 * for later use.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
 */
public final class InstrumentationSavingAgent {

	private static volatile Instrumentation instrumentation;


	private InstrumentationSavingAgent() {
	}


	/**
	 * Save the {@link Instrumentation} interface exposed by the JVM.
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * Save the {@link Instrumentation} interface exposed by the JVM.
	 * This method is required to dynamically load this Agent with the Attach API.
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * Return the {@link Instrumentation} interface exposed by the JVM.
	 * <p>Note that this agent class will typically not be available in the classpath
	 * unless the agent is actually specified on JVM startup. If you intend to do
	 * conditional checking with respect to agent availability, consider using
	 * {@link org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver#getInstrumentation()}
	 * instead - which will work without the agent class in the classpath as well.
	 * @return the {@code Instrumentation} instance previously saved when
	 * the {@link #premain} or {@link #agentmain} methods was called by the JVM;
	 * will be {@code null} if this class was not used as Java agent when this
	 * JVM was started or it wasn't installed as agent using the Attach API.
	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver#getInstrumentation()
	 */
	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}

}
