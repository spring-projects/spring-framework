/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.context.annotation;

/**
 * A {@link Condition} that offers more fine-grained control when used with
 * {@code @Configuration}. Allows certain {@link Condition}s to adapt when they match
 * based on the configuration phase. For example, a condition that checks if a bean
 * has already been registered might choose to only be evaluated during the
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} {@link ConfigurationPhase}.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see Configuration
 */
public interface ConfigurationCondition extends Condition {

	/**
	 * Return the {@link ConfigurationPhase} in which the condition should be evaluated.
	 *
	 * 返回应在其中评估条件的{@link ConfigurationPhase}。
	 */
	ConfigurationPhase getConfigurationPhase();


	/**
	 * The various configuration phases where the condition could be evaluated.
	 *
	 * 可以评估条件的各种配置阶段。
	 */
	enum ConfigurationPhase {

		/**
		 * The {@link Condition} should be evaluated as a {@code @Configuration}
		 * class is being parsed.
		 * <p>If the condition does not match at this point, the {@code @Configuration}
		 * class will not be added.
		 *
		 * 应该评估{@link Condition}，因为正在分析一个@Configuration}类。
		 * <p>如果此时条件不匹配，则不会添加{@code @Configuration}类。
		 */
		PARSE_CONFIGURATION,

		/**
		 * The {@link Condition} should be evaluated when adding a regular
		 * (non {@code @Configuration}) bean. The condition will not prevent
		 * {@code @Configuration} classes from being added.
		 * <p>At the time that the condition is evaluated, all {@code @Configuration}s
		 * will have been parsed.
		 *
		 * 添加常规（非{@code @Configuration}）bean时应该评估{@link Condition}。
		 * 该条件不会阻止添加{@code @Configuration}类。 <p>在评估条件的时候，所有{@code @Configuration}将被解析。
		 */
		REGISTER_BEAN
	}

}
