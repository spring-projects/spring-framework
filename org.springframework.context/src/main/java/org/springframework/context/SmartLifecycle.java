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

	boolean isAutoStartup();

	int getShutdownOrder();

	void stop(Runnable callback);

}
