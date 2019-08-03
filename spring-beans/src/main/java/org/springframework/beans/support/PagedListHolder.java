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

package org.springframework.beans.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * PagedListHolder is a simple state holder for handling lists of objects,
 * separating them into pages. Page numbering starts with 0.
 *
 * <p>This is mainly targeted at usage in web UIs. Typically, an instance will be
 * instantiated with a list of beans, put into the session, and exported as model.
 * The properties can all be set/get programmatically, but the most common way will
 * be data binding, i.e. populating the bean from request parameters. The getters
 * will mainly be used by the view.
 *
 * <p>Supports sorting the underlying list via a {@link SortDefinition} implementation,
 * available as property "sort". By default, a {@link MutableSortDefinition} instance
 * will be used, toggling the ascending value on setting the same property again.
 *
 * <p>The data binding names have to be called "pageSize" and "sort.ascending",
 * as expected by BeanWrapper. Note that the names and the nesting syntax match
 * the respective JSTL EL expressions, like "myModelAttr.pageSize" and
 * "myModelAttr.sort.ascending".
 *
 * @author Juergen Hoeller
 * @since 19.05.2003
 * @param <E> the element type
 * @see #getPageList()
 * @see org.springframework.beans.support.MutableSortDefinition
 */
@SuppressWarnings("serial")
public class PagedListHolder<E> implements Serializable {

	/**
	 * The default page size.
	 */
	public static final int DEFAULT_PAGE_SIZE = 10;

	/**
	 * The default maximum number of page links.
	 */
	public static final int DEFAULT_MAX_LINKED_PAGES = 10;


	private List<E> source = Collections.emptyList();

	@Nullable
	private Date refreshDate;

	@Nullable
	private SortDefinition sort;

	@Nullable
	private SortDefinition sortUsed;

	private int pageSize = DEFAULT_PAGE_SIZE;

	private int page = 0;

	private boolean newPageSet;

	private int maxLinkedPages = DEFAULT_MAX_LINKED_PAGES;


	/**
	 * Create a new holder instance.
	 * You'll need to set a source list to be able to use the holder.
	 * @see #setSource
	 */
	public PagedListHolder() {
		this(new ArrayList<>(0));
	}

	/**
	 * Create a new holder instance with the given source list, starting with
	 * a default sort definition (with "toggleAscendingOnProperty" activated).
	 * @param source the source List
	 * @see MutableSortDefinition#setToggleAscendingOnProperty
	 */
	public PagedListHolder(List<E> source) {
		this(source, new MutableSortDefinition(true));
	}

	/**
	 * Create a new holder instance with the given source list.
	 * @param source the source List
	 * @param sort the SortDefinition to start with
	 */
	public PagedListHolder(List<E> source, SortDefinition sort) {
		setSource(source);
		setSort(sort);
	}


	/**
	 * Set the source list for this holder.
	 */
	public void setSource(List<E> source) {
		Assert.notNull(source, "Source List must not be null");
		this.source = source;
		this.refreshDate = new Date();
		this.sortUsed = null;
	}

	/**
	 * Return the source list for this holder.
	 */
	public List<E> getSource() {
		return this.source;
	}

	/**
	 * Return the last time the list has been fetched from the source provider.
	 */
	@Nullable
	public Date getRefreshDate() {
		return this.refreshDate;
	}

	/**
	 * Set the sort definition for this holder.
	 * Typically an instance of MutableSortDefinition.
	 * @see org.springframework.beans.support.MutableSortDefinition
	 */
	public void setSort(@Nullable SortDefinition sort) {
		this.sort = sort;
	}

	/**
	 * Return the sort definition for this holder.
	 */
	@Nullable
	public SortDefinition getSort() {
		return this.sort;
	}

	/**
	 * Set the current page size.
	 * Resets the current page number if changed.
	 * <p>Default value is 10.
	 */
	public void setPageSize(int pageSize) {
		if (pageSize != this.pageSize) {
			this.pageSize = pageSize;
			if (!this.newPageSet) {
				this.page = 0;
			}
		}
	}

	/**
	 * Return the current page size.
	 */
	public int getPageSize() {
		return this.pageSize;
	}

	/**
	 * Set the current page number.
	 * Page numbering starts with 0.
	 */
	public void setPage(int page) {
		this.page = page;
		this.newPageSet = true;
	}

	/**
	 * Return the current page number.
	 * Page numbering starts with 0.
	 */
	public int getPage() {
		this.newPageSet = false;
		if (this.page >= getPageCount()) {
			this.page = getPageCount() - 1;
		}
		return this.page;
	}

	/**
	 * Set the maximum number of page links to a few pages around the current one.
	 */
	public void setMaxLinkedPages(int maxLinkedPages) {
		this.maxLinkedPages = maxLinkedPages;
	}

	/**
	 * Return the maximum number of page links to a few pages around the current one.
	 */
	public int getMaxLinkedPages() {
		return this.maxLinkedPages;
	}


	/**
	 * Return the number of pages for the current source list.
	 */
	public int getPageCount() {
		float nrOfPages = (float) getNrOfElements() / getPageSize();
		return (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages);
	}

	/**
	 * Return if the current page is the first one.
	 */
	public boolean isFirstPage() {
		return getPage() == 0;
	}

	/**
	 * Return if the current page is the last one.
	 */
	public boolean isLastPage() {
		return getPage() == getPageCount() -1;
	}

	/**
	 * Switch to previous page.
	 * Will stay on first page if already on first page.
	 */
	public void previousPage() {
		if (!isFirstPage()) {
			this.page--;
		}
	}

	/**
	 * Switch to next page.
	 * Will stay on last page if already on last page.
	 */
	public void nextPage() {
		if (!isLastPage()) {
			this.page++;
		}
	}

	/**
	 * Return the total number of elements in the source list.
	 */
	public int getNrOfElements() {
		return getSource().size();
	}

	/**
	 * Return the element index of the first element on the current page.
	 * Element numbering starts with 0.
	 */
	public int getFirstElementOnPage() {
		return (getPageSize() * getPage());
	}

	/**
	 * Return the element index of the last element on the current page.
	 * Element numbering starts with 0.
	 */
	public int getLastElementOnPage() {
		int endIndex = getPageSize() * (getPage() + 1);
		int size = getNrOfElements();
		return (endIndex > size ? size : endIndex) - 1;
	}

	/**
	 * Return a sub-list representing the current page.
	 */
	public List<E> getPageList() {
		return getSource().subList(getFirstElementOnPage(), getLastElementOnPage() + 1);
	}

	/**
	 * Return the first page to which create a link around the current page.
	 */
	public int getFirstLinkedPage() {
		return Math.max(0, getPage() - (getMaxLinkedPages() / 2));
	}

	/**
	 * Return the last page to which create a link around the current page.
	 */
	public int getLastLinkedPage() {
		return Math.min(getFirstLinkedPage() + getMaxLinkedPages() - 1, getPageCount() - 1);
	}


	/**
	 * Resort the list if necessary, i.e. if the current {@code sort} instance
	 * isn't equal to the backed-up {@code sortUsed} instance.
	 * <p>Calls {@code doSort} to trigger actual sorting.
	 * @see #doSort
	 */
	public void resort() {
		SortDefinition sort = getSort();
		if (sort != null && !sort.equals(this.sortUsed)) {
			this.sortUsed = copySortDefinition(sort);
			doSort(getSource(), sort);
			setPage(0);
		}
	}

	/**
	 * Create a deep copy of the given sort definition,
	 * for use as state holder to compare a modified sort definition against.
	 * <p>Default implementation creates a MutableSortDefinition instance.
	 * Can be overridden in subclasses, in particular in case of custom
	 * extensions to the SortDefinition interface. Is allowed to return
	 * null, which means that no sort state will be held, triggering
	 * actual sorting for each {@code resort} call.
	 * @param sort the current SortDefinition object
	 * @return a deep copy of the SortDefinition object
	 * @see MutableSortDefinition#MutableSortDefinition(SortDefinition)
	 */
	protected SortDefinition copySortDefinition(SortDefinition sort) {
		return new MutableSortDefinition(sort);
	}

	/**
	 * Actually perform sorting of the given source list, according to
	 * the given sort definition.
	 * <p>The default implementation uses Spring's PropertyComparator.
	 * Can be overridden in subclasses.
	 * @see PropertyComparator#sort(java.util.List, SortDefinition)
	 */
	protected void doSort(List<E> source, SortDefinition sort) {
		PropertyComparator.sort(source, sort);
	}

}
