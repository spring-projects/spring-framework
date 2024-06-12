/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.aot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.beans.factory.aot.AotException;
import org.springframework.beans.factory.aot.AotProcessingException;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.lang.Nullable;

/**
 * A collection of {@link BeanFactoryInitializationAotContribution AOT
 * contributions} obtained from {@link BeanFactoryInitializationAotProcessor AOT
 * processors}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class BeanFactoryInitializationAotContributions {

	private final List<BeanFactoryInitializationAotContribution> contributions;


	BeanFactoryInitializationAotContributions(DefaultListableBeanFactory beanFactory) {
		this(beanFactory, AotServices.factoriesAndBeans(beanFactory));
	}

	BeanFactoryInitializationAotContributions(DefaultListableBeanFactory beanFactory,
			AotServices.Loader loader) {
		this.contributions = getContributions(beanFactory, getProcessors(loader));
	}


	private static List<BeanFactoryInitializationAotProcessor> getProcessors(
			AotServices.Loader loader) {
		List<BeanFactoryInitializationAotProcessor> processors = new ArrayList<>(
				loader.load(BeanFactoryInitializationAotProcessor.class).asList());
		processors.add(new RuntimeHintsBeanFactoryInitializationAotProcessor());
		return Collections.unmodifiableList(processors);
	}

	private List<BeanFactoryInitializationAotContribution> getContributions(
			DefaultListableBeanFactory beanFactory,
			List<BeanFactoryInitializationAotProcessor> processors) {
		List<BeanFactoryInitializationAotContribution> contributions = new ArrayList<>();
		for (BeanFactoryInitializationAotProcessor processor : processors) {
			BeanFactoryInitializationAotContribution contribution = processAheadOfTime(processor, beanFactory);
			if (contribution != null) {
				contributions.add(contribution);
			}
		}
		return Collections.unmodifiableList(contributions);
	}

	@Nullable
	private BeanFactoryInitializationAotContribution processAheadOfTime(BeanFactoryInitializationAotProcessor processor,
			DefaultListableBeanFactory beanFactory) {

		try {
			return processor.processAheadOfTime(beanFactory);
		}
		catch (AotException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new AotProcessingException("Error executing '" +
					processor.getClass().getName() + "': " + ex.getMessage(), ex);
		}
	}

	void applyTo(GenerationContext generationContext,
			BeanFactoryInitializationCode beanFactoryInitializationCode) {
		for (BeanFactoryInitializationAotContribution contribution : this.contributions) {
			contribution.applyTo(generationContext, beanFactoryInitializationCode);
		}
	}

}
