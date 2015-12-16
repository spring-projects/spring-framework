/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.util.CollectionUtils;

/**
 * Static, factory methods for {@link ResultHandler}-based result actions.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java editor
 * favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class MockMvcResultHandlers {

	/**
	 * Print {@link MvcResult} details to the "standard" output stream.
	 */
	public static ResultHandler print() {
		return new ConsolePrintingResultHandler();
	}


	/**
	 * A {@link PrintingResultHandler} that writes to the "standard" output stream.
	 */
	private static class ConsolePrintingResultHandler extends PrintingResultHandler {

		public ConsolePrintingResultHandler() {
			super(new ResultValuePrinter() {
				@Override
				public void printHeading(String heading) {
					System.out.println();
					System.out.println(String.format("%20s:", heading));
				}
				@Override
				public void printValue(String label, Object value) {
					if (value != null && value.getClass().isArray()) {
						value = CollectionUtils.arrayToList(value);
					}
					System.out.println(String.format("%20s = %s", label, value));
				}
			});
		}
	}

}
