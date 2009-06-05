/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.scheduling.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.JdkVersion;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'executor' element of the 'task' namespace.
 * 
 * @author Mark Fisher
 * @since 3.0
 */
public class ExecutorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		if (this.shouldUseBackport(element)) {
			return "org.springframework.scheduling.backportconcurrent.ThreadPoolTaskExecutor";
		}
		return "org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String keepAliveSeconds = element.getAttribute("keep-alive");
		if (StringUtils.hasText(keepAliveSeconds)) {
			builder.addPropertyValue("keepAliveSeconds", keepAliveSeconds);
		}
		String queueCapacity = element.getAttribute("queue-capacity");
		if (StringUtils.hasText(queueCapacity)) {
			builder.addPropertyValue("queueCapacity", queueCapacity);
		}
		this.configureRejectionPolicy(element, builder);
		String size = element.getAttribute("size");
		if (!StringUtils.hasText(size)) {
			return;
		}
		Integer[] range = null;
		try {
			int separatorIndex = size.indexOf('-');
			if (separatorIndex != -1) {
				range = new Integer[2];
				range[0] = Integer.valueOf(size.substring(0, separatorIndex));
				range[1] = Integer.valueOf(size.substring(separatorIndex + 1, size.length()));
				if (range[0] > range[1]) {
					parserContext.getReaderContext().error(
							"Lower bound of size range must not exceed the upper bound.", element);
				}
				if (!StringUtils.hasText(queueCapacity)) {
					// no queue-capacity provided, so unbounded
					if (range[0] == 0) {
						// actually set 'corePoolSize' to the upper bound of the range
						// but allow core threads to timeout
						builder.addPropertyValue("allowCoreThreadTimeOut", true);
						range[0] = range[1];
					}
					else {
						// non-zero lower bound implies a core-max size range
						parserContext.getReaderContext().error(
								"A non-zero lower bound for the size range requires a queue-capacity value.", element);
					}
				}
			}
			else {
				Integer value = Integer.valueOf(size);
				range = new Integer[] {value, value};
			}
		}
		catch (NumberFormatException ex) {
			parserContext.getReaderContext().error("Invalid size value [" + size + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported.",
					element, ex);
		}
		if (range != null) {
			builder.addPropertyValue("corePoolSize", range[0]);
			builder.addPropertyValue("maxPoolSize", range[1]);
		}
	}

	private void configureRejectionPolicy(Element element, BeanDefinitionBuilder builder) {
		String rejectionPolicy = element.getAttribute("rejection-policy");
		if (!StringUtils.hasText(rejectionPolicy)) {
			return;
		}
		Object handler = null;		
		boolean createBackportHandler = this.shouldUseBackport(element);
		if (rejectionPolicy.equals("ABORT")) {
			if (createBackportHandler) {
				handler = new edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor.AbortPolicy();
			}
			else {
				handler = new ThreadPoolExecutor.AbortPolicy();
			}
		}
		if (rejectionPolicy.equals("CALLER_RUNS")) {
			if (createBackportHandler) {
				handler = new edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy();
			}
			else {
				handler = new ThreadPoolExecutor.CallerRunsPolicy();
			}
		}
		if (rejectionPolicy.equals("DISCARD")) {
			if (createBackportHandler) {
				handler = new edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor.DiscardPolicy();
			}
			handler = new ThreadPoolExecutor.DiscardPolicy();
		}
		if (rejectionPolicy.equals("DISCARD_OLDEST")) {
			if (createBackportHandler) {
				handler = new edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy();
			}
			handler = new ThreadPoolExecutor.DiscardOldestPolicy();
		}
		builder.addPropertyValue("rejectedExecutionHandler", handler);
	}

	private boolean shouldUseBackport(Element element) {
		String size = element.getAttribute("size");
		return StringUtils.hasText(size) && size.startsWith("0")
				&& JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_16;
	}

}
