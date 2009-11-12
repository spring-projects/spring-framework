/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context;

/**
 * An extension of the Lifecycle interface for those beans that require to be
 * started upon ApplicationContext refresh and/or shutdown in a particular order.
 * 
 * @author Mark Fisher
 * @since 3.0
 */
public interface SmartLifecycle extends Lifecycle {

	/**
	 * Return whether this Lifecycle component should be started automatically
	 * by the container when the ApplicationContext is refreshed. A value of
	 * "false" indicates that the component is intended to be started manually.
	 */
	boolean isAutoStartup();

	/**
	 * Return the order in which this Lifecycle component should be stopped.
	 * The shutdown process begins with the component(s) having the <i>lowest</i>
	 * value and ends with the highest value (Integer.MIN_VALUE is the lowest
	 * possible, and Integer.MAX_VALUE is the highest possible). Any Lifecycle
	 * components within the context that do not also implement SmartLifecycle
	 * will be treated as if they have a value of Integer.MAX_VALUE.
	 */
	int getShutdownOrder();

	/**
	 * Indicates that a Lifecycle component must stop if it is currently running.
	 * The provided callback is used by the LifecycleProcessor to support an
	 * ordered, and potentially concurrent, shutdown of all components having a
	 * common shutdown order value. The callback <b>must</b> be executed after
	 * the SmartLifecycle component does indeed stop.
	 */
	void stop(Runnable callback);

}
