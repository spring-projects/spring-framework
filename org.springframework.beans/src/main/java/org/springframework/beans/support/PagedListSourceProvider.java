/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.support;

import java.util.List;
import java.util.Locale;

/**
 * Callback that provides the source for a reloadable List.
 * Used by {@link RefreshablePagedListHolder}.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @deprecated as of Spring 2.5, to be removed in Spring 3.0
 * @see org.springframework.beans.support.RefreshablePagedListHolder#setSourceProvider
 */
public interface PagedListSourceProvider {

	/**
	 * Load the List for the given Locale and filter settings.
	 * The filter object can be of any custom class, preferably a bean
	 * for easy data binding from a request. An instance will simply
	 * get passed through to this callback method.
	 * @param locale Locale that the List should be loaded for,
	 * or <code>null</code> if not locale-specific
	 * @param filter object representing filter settings,
	 * or <code>null</code> if no filter options are used
	 * @return the loaded List
	 * @see org.springframework.beans.support.RefreshablePagedListHolder#setLocale
	 * @see org.springframework.beans.support.RefreshablePagedListHolder#setFilter
	 */
	List loadList(Locale locale, Object filter);

}
