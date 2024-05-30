/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.metrics;

import org.springframework.lang.Nullable;

import java.util.function.Supplier;

/**
 * Step recording metrics about a particular phase or action happening during the {@link ApplicationStartup}.
 *
 * <p>The lifecycle of a {@code StartupStep} goes as follows:
 * <ol>
 * <li>the step is created and starts by calling {@link ApplicationStartup#start(String) the application startup}
 * and is assigned a unique {@link StartupStep#getId() id}.
 * <li>we can then attach information with {@link Tags} during processing
 * <li>we then need to mark the {@link #end()} of the step
 * </ol>
 *
 * <p>Implementations can track the "execution time" or other metrics for steps.
 * 记录ApplicationStartup期间发生的特定阶段或动作的指标。 实现可以跟踪步骤的“执行时间”或其他指标。
 * StartupStep的生命周期如下：
 * 		1. 该步骤通过调用应用程序启动来创建和启动，并分配了唯一的id
 * 		2. 然后，我们可以在处理过程中将信息附加到 StartupStep.Tags
 * 		3. 然后我们需要标记步骤的end()方法
 *
 * @author Brian Clozel
 * @since 5.3
 */
public interface StartupStep {

	/**
	 * Return the name of the startup step.
	 * <p>A step name describes the current action or phase. This technical
	 * name should be "." namespaced and can be reused to describe other instances of
	 * similar steps during application startup.
	 * 返回启动步骤的名称
	 */
	String getName();

	/**
	 * Return the unique id for this step within the application startup.
	 * 在应用程序启动中返回此步骤的唯一id
	 */
	long getId();

	/**
	 * Return, if available, the id of the parent step.
	 * <p>The parent step is the step that was started the most recently
	 * when the current step was created.
	 * 返回父步骤的id（如果可用）
	 * 父步骤是最近创建当前步骤时启动的步骤
	 */
	@Nullable
	Long getParentId();

	/**
	 * Add a {@link Tag} to the step.
	 * 在步骤中添加 StartupStep.Tag
	 *
	 * @param key   tag key
	 * @param value tag value
	 */
	StartupStep tag(String key, String value);

	/**
	 * Add a {@link Tag} to the step.
	 * 在步骤中添加 StartupStep.Tag
	 *
	 * @param key   tag key
	 * @param value {@link Supplier} for the tag value
	 */
	StartupStep tag(String key, Supplier<String> value);

	/**
	 * Return the {@link Tag} collection for this step.
	 */
	Tags getTags();

	/**
	 * Record the state of the step and possibly other metrics like execution time.
	 * <p>Once ended, changes on the step state are not allowed.
	 * 记录步骤的状态以及可能的其他指标，如执行时间
	 * 结束后，不允许更改步骤状态
	 */

	void end();


	/**
	 * Immutable collection of {@link Tag}.
	 * StartupStep.Tag的不可变集合
	 */
	interface Tags extends Iterable<Tag> {
	}


	/**
	 * Simple key/value association for storing step metadata.
	 * 用于存储步骤元数据的简单键/值关联
	 */
	interface Tag {

		/**
		 * Return the {@code Tag} name.
		 * 返回标记名
		 */
		String getKey();

		/**
		 * Return the {@code Tag} value.
		 * 返回标记值
		 */
		String getValue();
	}

}
