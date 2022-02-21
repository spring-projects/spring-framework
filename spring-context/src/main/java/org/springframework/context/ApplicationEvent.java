/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context;

import java.util.EventObject;

/**
 * Class to be extended by all application events. Abstract as it
 * doesn't make sense for generic events to be published directly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 *
 *  Spring的事件驱动模型由三部分组成：
 * 事件: ApplicationEvent ,继承自JDK的 EventObject ，所有事件都要继承它,也就是被观察者
 * 事件发布者: ApplicationEventPublisher 及 ApplicationEventMulticaster 接口，使用这个接口，就可以发布事件了
 * 事件监听者: ApplicationListener ,继承JDK的 EventListener ，所有监听者都继承它，也就是我们所说的观察者，当然我们也可以使用注解 @EventListener ，效果是一样的
 *
 * 在Spring框架中，默认对ApplicationEvent事件提供了如下支持:
 * ContextStartedEvent：ApplicationContext启动后触发的事件
 * ContextStoppedEvent：ApplicationContext停止后触发的事件
 * ContextRefreshedEvent： ApplicationContext初始化或刷新完成后触发的事件 ；（容器初始化完成后调用，所以我们可以利用这个事件做一些初始化操作）
 * ContextClosedEvent：ApplicationContext关闭后触发的事件；（如 web 容器关闭时自动会触发spring容器的关闭，如果是普通 java 应用，需要调用ctx.registerShutdownHook();注册虚拟机关闭时的钩子才行）
 *
 */
public abstract class ApplicationEvent extends EventObject {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 7099057708183571937L;

	/** System time when the event happened. */
	private final long timestamp;


	/**
	 * Create a new ApplicationEvent.
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}


	/**
	 * Return the system time in milliseconds when the event happened.
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

}
