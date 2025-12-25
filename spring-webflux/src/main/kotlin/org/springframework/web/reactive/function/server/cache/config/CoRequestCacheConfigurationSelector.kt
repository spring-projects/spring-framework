/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.function.server.cache.config

import org.springframework.context.annotation.AdviceMode
import org.springframework.context.annotation.AdviceModeImportSelector
import org.springframework.context.annotation.AutoProxyRegistrar
import org.springframework.web.reactive.function.server.cache.EnableCoRequestCaching
import kotlin.reflect.jvm.jvmName

private const val UNSUPPORTED_ADVISE_MODE_MESSAGE = "CoRequestCaching does not support aspectj advice mode"

/**
 * Select which classes to import according to the value of [EnableCoRequestCaching.mode] on the importing
 * `@Configuration` class.
 *
 * Only [AdviceMode.PROXY] is currently supported.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 * @see CoRequestCacheConfiguration
 */
internal class CoRequestCacheConfigurationSelector : AdviceModeImportSelector<EnableCoRequestCaching>() {
	override fun selectImports(adviceMode: AdviceMode): Array<out String> =
		when (adviceMode) {
			AdviceMode.PROXY -> arrayOf(AutoProxyRegistrar::class.jvmName, CoRequestCacheConfiguration::class.jvmName)
			AdviceMode.ASPECTJ -> throw UnsupportedOperationException(UNSUPPORTED_ADVISE_MODE_MESSAGE)
		}
}
