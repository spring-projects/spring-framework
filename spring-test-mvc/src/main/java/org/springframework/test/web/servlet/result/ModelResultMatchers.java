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

package org.springframework.test.web.servlet.result;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

/**
 * Factory for assertions on the model. An instance of this class is
 * typically accessed via {@link MockMvcResultMatchers#model()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ModelResultMatchers {


	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#model()}.
	 */
	protected ModelResultMatchers() {
	}

	/**
	 * Assert a model attribute value with the given Hamcrest {@link Matcher}.
	 */
	public <T> ResultMatcher attribute(final String name, final Matcher<T> matcher) {
		return new ResultMatcher() {
			@SuppressWarnings("unchecked")
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				assertThat("Model attribute '" + name + "'", (T) mav.getModel().get(name), matcher);
			}
		};
	}

	/**
	 * Assert a model attribute value.
	 */
	public ResultMatcher attribute(final String name, final Object value) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				assertEquals("Model attribute '" + name + "'", value, mav.getModel().get(name));
			}
		};
	}

	/**
	 * Assert the given model attributes exist.
	 */
	public ResultMatcher attributeExists(final String... names) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				for (String name : names) {
					assertTrue("Model attribute '" + name + "' does not exist", mav.getModel().get(name) != null);
				}
			}
		};
	}

    /**
     * Assert the given model attributes do not exist
     */
    public ResultMatcher attributeDoesNotExist(final String... names) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult result) throws Exception {
                ModelAndView mav = getModelAndView(result);
                for (String name : names) {
                    assertTrue("Model attribute '" + name + "' exists", mav.getModel().get(name) == null);
                }
            }
        };
    }

	/**
	 * Assert the given model attribute(s) have errors.
	 */
	public ResultMatcher attributeErrorCount(final String name, final int expectedCount) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				Errors errors = getBindingResult(mav, name);
				assertEquals("Binding/validation error count for attribute [" + name + "], ",
						expectedCount, errors.getErrorCount());
			}
		};
	}

	/**
	 * Assert the given model attribute(s) have errors.
	 */
	public ResultMatcher attributeHasErrors(final String... names) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				for (String name : names) {
					BindingResult result = getBindingResult(mav, name);
					assertTrue("No errors for attribute [" + name + "]", result.hasErrors());
				}
			}
		};
	}

	/**
	 * Assert the given model attribute(s) do not have errors.
	 */
	public ResultMatcher attributeHasNoErrors(final String... names) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				for (String name : names) {
					BindingResult result = getBindingResult(mav, name);
					assertTrue("No errors for attribute [" + name + "]", !result.hasErrors());
				}
			}
		};
	}

	/**
	 * Assert the given model attribute field(s) have errors.
	 */
	public ResultMatcher attributeHasFieldErrors(final String name, final String... fieldNames) {
		return new ResultMatcher() {
			public void match(MvcResult mvcResult) throws Exception {
				ModelAndView mav = getModelAndView(mvcResult);
				BindingResult result = getBindingResult(mav, name);
				assertTrue("No errors for attribute: [" + name + "]", result.hasErrors());
				for (final String fieldName : fieldNames) {
					assertTrue("No errors for field: [" + fieldName + "] of attribute [" + name + "]",
							result.hasFieldErrors(fieldName));
				}
			}
		};
	}

	/**
	 * Assert the total number of errors in the model.
	 */
	public <T> ResultMatcher errorCount(final int expectedCount) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				int actualCount = getErrorCount(getModelAndView(result).getModelMap());
				assertEquals("Binding/validation error count", expectedCount, actualCount);
			}
		};
	}

	/**
	 * Assert the model has errors.
	 */
	public <T> ResultMatcher hasErrors() {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				int count = getErrorCount(getModelAndView(result).getModelMap());
				assertTrue("Expected binding/validation errors", count != 0);
			}
		};
	}

	/**
	 * Assert the model has no errors.
	 */
	public <T> ResultMatcher hasNoErrors() {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				for (Object value : mav.getModel().values()) {
					if (value instanceof Errors) {
						assertTrue("Unexpected binding/validation error(s) [" + value + "]",
								!((Errors) value).hasErrors());
					}
				}
			}
		};
	}

	/**
	 * Assert the number of model attributes.
	 */
	public <T> ResultMatcher size(final int size) {
		return new ResultMatcher() {
			public void match(MvcResult result) throws Exception {
				ModelAndView mav = getModelAndView(result);
				int actual = 0;
				for (String key : mav.getModel().keySet()) {
					if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
						actual++;
					}
				}
				assertEquals("Model size", size, actual);
			}
		};
	}

	private ModelAndView getModelAndView(MvcResult mvcResult) {
		ModelAndView mav = mvcResult.getModelAndView();
		assertTrue("No ModelAndView found", mav != null);
		return mav;
	}

	private BindingResult getBindingResult(ModelAndView mav, String name) {
		BindingResult result = (BindingResult) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
		assertTrue("No BindingResult for attribute: " + name, result != null);
		return result;
	}

	private int getErrorCount(ModelMap model) {
		int count = 0;
		for (Object value : model.values()) {
			if (value instanceof Errors) {
				count += ((Errors) value).getErrorCount();
			}
		}
		return count;
	}

}
