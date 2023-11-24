/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.context.annotation.configuration;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

/**
 * @author Marcin Piela
 * @author Juergen Hoeller
 */
class Spr12526Tests {

	@Test
	void testInjection() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(TestContext.class);
		CustomCondition condition = ctx.getBean(CustomCondition.class);

		condition.setCondition(true);
		FirstService firstService = (FirstService) ctx.getBean(Service.class);
		assertThat(firstService.getDependency()).as("FirstService.dependency is null").isNotNull();

		condition.setCondition(false);
		SecondService secondService = (SecondService) ctx.getBean(Service.class);
		assertThat(secondService.getDependency()).as("SecondService.dependency is null").isNotNull();

		ctx.close();
	}


	@Configuration
	static class TestContext {

		@Bean
		@Scope(SCOPE_SINGLETON)
		CustomCondition condition() {
			return new CustomCondition();
		}

		@Bean
		@Scope(SCOPE_PROTOTYPE)
		Service service(CustomCondition condition) {
			return (condition.check() ? new FirstService() : new SecondService());
		}

		@Bean
		DependencyOne dependencyOne() {
			return new DependencyOne();
		}

		@Bean
		DependencyTwo dependencyTwo() {
			return new DependencyTwo();
		}
	}


	public static class CustomCondition {

		private boolean condition;

		public boolean check() {
			return condition;
		}

		public void setCondition(boolean value) {
			this.condition = value;
		}
	}


	public interface Service {

		void doStuff();
	}


	public static class FirstService implements Service {

		private DependencyOne dependency;


		@Override
		public void doStuff() {
			if (dependency == null) {
				throw new IllegalStateException("FirstService: dependency is null");
			}
		}

		@Resource(name = "dependencyOne")
		public void setDependency(DependencyOne dependency) {
			this.dependency = dependency;
		}


		public DependencyOne getDependency() {
			return dependency;
		}
	}


	public static class SecondService implements Service {

		private DependencyTwo dependency;

		@Override
		public void doStuff() {
			if (dependency == null) {
				throw new IllegalStateException("SecondService: dependency is null");
			}
		}

		@Resource(name = "dependencyTwo")
		public void setDependency(DependencyTwo dependency) {
			this.dependency = dependency;
		}


		public DependencyTwo getDependency() {
			return dependency;
		}
	}


	public static class DependencyOne {
	}


	public static class DependencyTwo {
	}

}
