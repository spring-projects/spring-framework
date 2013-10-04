/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.i18n;

import java.util.TimeZone;

/**
 * Extension of {@link LocaleContext}, adding awareness of the current time zone.
 *
 * <p>Having this variant of LocaleContext set to {@link LocaleContextHolder} means
 * that some TimeZone-aware infrastructure has been configured, even if it may not
 * be able to produce a non-null TimeZone at the moment.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see LocaleContextHolder#getTimeZone()
 */
public interface TimeZoneAwareLocaleContext extends LocaleContext {

	/**
	 * Return the current TimeZone, which can be fixed or determined dynamically,
	 * depending on the implementation strategy.
	 * @return the current TimeZone, or {@code null} if no specific TimeZone associated
	 */
	TimeZone getTimeZone();

}
