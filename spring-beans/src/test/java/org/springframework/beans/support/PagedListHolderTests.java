/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Jean-Pierre PAWLAK
 * @author Chris Beams
 * @since 20.05.2003
 */
public class PagedListHolderTests {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testPagedListHolder() {
		TestBean tb1 = new TestBean();
		tb1.setName("eva");
		tb1.setAge(25);
		TestBean tb2 = new TestBean();
		tb2.setName("juergen");
		tb2.setAge(99);
		TestBean tb3 = new TestBean();
		tb3.setName("Rod");
		tb3.setAge(32);
		List tbs = new ArrayList();
		tbs.add(tb1);
		tbs.add(tb2);
		tbs.add(tb3);

		PagedListHolder holder = new PagedListHolder(tbs);
		assertThat(holder.getSource()).as("Correct source").isSameAs(tbs);
		assertThat(holder.getNrOfElements()).as("Correct number of elements").isEqualTo(3);
		assertThat(holder.getPageCount()).as("Correct number of pages").isEqualTo(1);
		assertThat(holder.getPageSize()).as("Correct page size").isEqualTo(PagedListHolder.DEFAULT_PAGE_SIZE);
		assertThat(holder.getPage()).as("Correct page number").isEqualTo(0);
		assertThat(holder.isFirstPage()).as("First page").isTrue();
		assertThat(holder.isLastPage()).as("Last page").isTrue();
		assertThat(holder.getFirstElementOnPage()).as("Correct first element").isEqualTo(0);
		assertThat(holder.getLastElementOnPage()).as("Correct first element").isEqualTo(2);
		assertThat(holder.getPageList().size()).as("Correct page list size").isEqualTo(3);
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb1);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb2);
		assertThat(holder.getPageList().get(2)).as("Correct page list contents").isSameAs(tb3);

		holder.setPageSize(2);
		assertThat(holder.getPageCount()).as("Correct number of pages").isEqualTo(2);
		assertThat(holder.getPageSize()).as("Correct page size").isEqualTo(2);
		assertThat(holder.getPage()).as("Correct page number").isEqualTo(0);
		assertThat(holder.isFirstPage()).as("First page").isTrue();
		assertThat(holder.isLastPage()).as("Last page").isFalse();
		assertThat(holder.getFirstElementOnPage()).as("Correct first element").isEqualTo(0);
		assertThat(holder.getLastElementOnPage()).as("Correct last element").isEqualTo(1);
		assertThat(holder.getPageList().size()).as("Correct page list size").isEqualTo(2);
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb1);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb2);

		holder.setPage(1);
		assertThat(holder.getPage()).as("Correct page number").isEqualTo(1);
		assertThat(holder.isFirstPage()).as("First page").isFalse();
		assertThat(holder.isLastPage()).as("Last page").isTrue();
		assertThat(holder.getFirstElementOnPage()).as("Correct first element").isEqualTo(2);
		assertThat(holder.getLastElementOnPage()).as("Correct last element").isEqualTo(2);
		assertThat(holder.getPageList().size()).as("Correct page list size").isEqualTo(1);
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb3);

		holder.setPageSize(3);
		assertThat(holder.getPageCount()).as("Correct number of pages").isEqualTo(1);
		assertThat(holder.getPageSize()).as("Correct page size").isEqualTo(3);
		assertThat(holder.getPage()).as("Correct page number").isEqualTo(0);
		assertThat(holder.isFirstPage()).as("First page").isTrue();
		assertThat(holder.isLastPage()).as("Last page").isTrue();
		assertThat(holder.getFirstElementOnPage()).as("Correct first element").isEqualTo(0);
		assertThat(holder.getLastElementOnPage()).as("Correct last element").isEqualTo(2);

		holder.setPage(1);
		holder.setPageSize(2);
		assertThat(holder.getPageCount()).as("Correct number of pages").isEqualTo(2);
		assertThat(holder.getPageSize()).as("Correct page size").isEqualTo(2);
		assertThat(holder.getPage()).as("Correct page number").isEqualTo(1);
		assertThat(holder.isFirstPage()).as("First page").isFalse();
		assertThat(holder.isLastPage()).as("Last page").isTrue();
		assertThat(holder.getFirstElementOnPage()).as("Correct first element").isEqualTo(2);
		assertThat(holder.getLastElementOnPage()).as("Correct last element").isEqualTo(2);

		holder.setPageSize(2);
		holder.setPage(1);
		((MutableSortDefinition) holder.getSort()).setProperty("name");
		((MutableSortDefinition) holder.getSort()).setIgnoreCase(false);
		holder.resort();
		assertThat(holder.getSource()).as("Correct source").isSameAs(tbs);
		assertThat(holder.getNrOfElements()).as("Correct number of elements").isEqualTo(3);
		assertThat(holder.getPageCount()).as("Correct number of pages").isEqualTo(2);
		assertThat(holder.getPageSize()).as("Correct page size").isEqualTo(2);
		assertThat(holder.getPage()).as("Correct page number").isEqualTo(0);
		assertThat(holder.isFirstPage()).as("First page").isTrue();
		assertThat(holder.isLastPage()).as("Last page").isFalse();
		assertThat(holder.getFirstElementOnPage()).as("Correct first element").isEqualTo(0);
		assertThat(holder.getLastElementOnPage()).as("Correct last element").isEqualTo(1);
		assertThat(holder.getPageList().size()).as("Correct page list size").isEqualTo(2);
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb3);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb1);

		((MutableSortDefinition) holder.getSort()).setProperty("name");
		holder.resort();
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb2);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb1);

		((MutableSortDefinition) holder.getSort()).setProperty("name");
		holder.resort();
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb3);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb1);

		holder.setPage(1);
		assertThat(holder.getPageList().size()).as("Correct page list size").isEqualTo(1);
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb2);

		((MutableSortDefinition) holder.getSort()).setProperty("age");
		holder.resort();
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb1);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb3);

		((MutableSortDefinition) holder.getSort()).setIgnoreCase(true);
		holder.resort();
		assertThat(holder.getPageList().get(0)).as("Correct page list contents").isSameAs(tb1);
		assertThat(holder.getPageList().get(1)).as("Correct page list contents").isSameAs(tb3);

		holder.nextPage();
		assertThat(holder.getPage()).isEqualTo(1);
		holder.previousPage();
		assertThat(holder.getPage()).isEqualTo(0);
		holder.nextPage();
		assertThat(holder.getPage()).isEqualTo(1);
		holder.nextPage();
		assertThat(holder.getPage()).isEqualTo(1);
		holder.previousPage();
		assertThat(holder.getPage()).isEqualTo(0);
		holder.previousPage();
		assertThat(holder.getPage()).isEqualTo(0);
	}



	public static class MockFilter {

		private String name = "";
		private String age = "";
		private String extendedInfo = "";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAge() {
			return age;
		}

		public void setAge(String age) {
			this.age = age;
		}

		public String getExtendedInfo() {
			return extendedInfo;
		}

		public void setExtendedInfo(String extendedInfo) {
			this.extendedInfo = extendedInfo;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof MockFilter mockFilter)) {
				return false;
			}

			if (!age.equals(mockFilter.age)) {
				return false;
			}
			if (!extendedInfo.equals(mockFilter.extendedInfo)) {
				return false;
			}
			if (!name.equals(mockFilter.name)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result;
			result = name.hashCode();
			result = 29 * result + age.hashCode();
			result = 29 * result + extendedInfo.hashCode();
			return result;
		}
	}

}
