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

package org.springframework.context.groovy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import groovy.lang.GroovyShell;
import org.junit.jupiter.api.Test;

import org.springframework.aop.SpringProxy;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericGroovyApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link GroovyBeanDefinitionReader}.
 *
 * @author Jeff Brown
 * @author Dave Syer
 * @author Sam Brannen
 */
class GroovyBeanDefinitionReaderTests {

	@Test
	void importSpringXml() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			beans {
				importBeans "classpath:org/springframework/context/groovy/test.xml"
			}
			""");

		assertThat(appCtx.getBean("foo")).isEqualTo("hello");
	}

	@Test
	void importBeansFromGroovy() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			beans {
				importBeans "classpath:org/springframework/context/groovy/applicationContext.groovy"
			}
			""");

		assertThat(appCtx.getBean("foo")).isEqualTo("hello");
	}

	@Test
	void singletonPropertyOnBeanDefinition() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy;
			beans {
				singletonBean(Bean1Impl) { bean ->
					bean.singleton = true
				}
				nonSingletonBean(Bean1Impl) { bean ->
					bean.singleton = false
				}
				unSpecifiedScopeBean(Bean1Impl)
			}
			""");

		assertThat(appCtx.isSingleton("singletonBean")).as("singletonBean should have been a singleton").isTrue();
		assertThat(appCtx.isSingleton("nonSingletonBean")).as("nonSingletonBean should not have been a singleton").isFalse();
		assertThat(appCtx.isSingleton("unSpecifiedScopeBean")).as("unSpecifiedScopeBean should not have been a singleton").isTrue();
	}

	@Test
	void inheritPropertiesFromAbstractBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy;
			beans {
				myB(Bean1Impl) {
					person = "wombat"
				}

				myAbstractA(Bean2) { bean ->
					bean."abstract" = true
					age = 10
					bean1 = myB
				}
				myConcreteB {
					it.parent = myAbstractA
				}
			}
			""");

		Bean2 bean = (Bean2) appCtx.getBean("myConcreteB");
		assertThat(bean.getAge()).isEqualTo(10);
		assertThat(bean.bean1).isNotNull();
	}

	@Test
	void contextComponentScanSpringTag() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			beans {
				xmlns context:"http://www.springframework.org/schema/context"

				context.'component-scan'( 'base-package':"org.springframework.context.groovy" )
			}
			""");

		assertThat(appCtx.getBean("person")).isInstanceOf(AdvisedPerson.class);
	}

	@Test
	void useSpringNamespaceAsMethod() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy;
			beans {
				xmlns aop:"http://www.springframework.org/schema/aop"

				fred(AdvisedPersonImpl) {
					name = "Fred"
					age = 45
				}
				birthdayCardSenderAspect(BirthdayCardSender)

				aop {
					config("proxy-target-class": false) {
						aspect(id: "sendBirthdayCard", ref: "birthdayCardSenderAspect") {
							after method: "onBirthday", pointcut: "execution(void org.springframework.context.groovy.AdvisedPerson.birthday()) and this(person)"
						}
					}
				}
			}
			""");

		AdvisedPerson fred = (AdvisedPerson) appCtx.getBean("fred");
		assertThat(fred).isInstanceOf(SpringProxy.class);
		fred.birthday();

		BirthdayCardSender birthDaySender = (BirthdayCardSender) appCtx.getBean("birthdayCardSenderAspect");

		assertThat(birthDaySender.peopleSentCards).hasSize(1);
		assertThat(birthDaySender.peopleSentCards.get(0).getName()).isEqualTo("Fred");
	}

	@Test
	@SuppressWarnings("unchecked")
	void useTwoSpringNamespaces() {
		GenericApplicationContext appCtx = new GenericApplicationContext();
		TestScope scope = new TestScope();
		appCtx.getBeanFactory().registerScope("test", scope);

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				xmlns aop:"http://www.springframework.org/schema/aop"
				xmlns util:"http://www.springframework.org/schema/util"
				bean1(Bean1Impl) { bean ->
					bean.scope = "test"
					aop.'scoped-proxy'("proxy-target-class": false)
				}
				util.list(id: 'foo') {
					value 'one'
					value 'two'
				}
			}
			""");

		assertThat((List<String>)appCtx.getBean("foo")).containsExactly("one", "two");

		assertThat(appCtx.getBean("bean1")).isNotNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();

		// should only be true because bean not initialized until proxy called
		assertThat(scope.instanceCount).isEqualTo(2);

		appCtx = new GenericApplicationContext();
		appCtx.getBeanFactory().registerScope("test", scope);

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			import java.util.ArrayList
			beans {
				xmlns aop:"http://www.springframework.org/schema/aop", util:"http://www.springframework.org/schema/util"
				bean1(Bean1Impl) { bean ->
					bean.scope = "test"
					aop.'scoped-proxy'("proxy-target-class": false)
				}
				util.list(id: 'foo') {
					value 'one'
					value 'two'
				}
			}
			""");

		assertThat((List<String>)appCtx.getBean("foo")).containsExactly("one", "two");

		assertThat(appCtx.getBean("bean1")).isNotNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();

		// should only be true because bean not initialized until proxy called
		assertThat(scope.instanceCount).isEqualTo(4);
	}

	@Test
	void springAopSupport() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				xmlns aop:"http://www.springframework.org/schema/aop"

				fred(AdvisedPersonImpl) {
					name = "Fred"
					age = 45
				}
				birthdayCardSenderAspect(BirthdayCardSender)

				aop.config("proxy-target-class": false) {
					aspect(id: "sendBirthdayCard", ref: "birthdayCardSenderAspect") {
						after method:"onBirthday", pointcut: "execution(void org.springframework.context.groovy.AdvisedPerson.birthday()) and this(person)"
					}
				}
			}
			""");

		AdvisedPerson fred = (AdvisedPerson) appCtx.getBean("fred");
		assertThat(fred).isInstanceOf(SpringProxy.class);
		fred.birthday();

		BirthdayCardSender birthDaySender = (BirthdayCardSender) appCtx.getBean("birthdayCardSenderAspect");

		assertThat(birthDaySender.peopleSentCards).hasSize(1);
		assertThat(birthDaySender.peopleSentCards.get(0).getName()).isEqualTo("Fred");
	}

	@Test
	void springScopedProxyBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		TestScope scope = new TestScope();
		appCtx.getBeanFactory().registerScope("test", scope);
		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				xmlns aop:"http://www.springframework.org/schema/aop"
				bean1(Bean1Impl) { bean ->
					bean.scope = "test"
					aop.'scoped-proxy'('proxy-target-class': false)
				}
			}
			""");

		assertThat(appCtx.getBean("bean1")).isNotNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();

		// should only be true because bean not initialized until proxy called
		assertThat(scope.instanceCount).isEqualTo(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void springNamespaceBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				xmlns util: 'http://www.springframework.org/schema/util'
				util.list(id: 'foo') {
					value 'one'
					value 'two'
				}
			}
			""");

		assertThat((List<String>) appCtx.getBean("foo")).containsExactly("one", "two");
	}

	@Test
	void namedArgumentConstructor() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				holyGrail(HolyGrailQuest)
				knights(KnightsOfTheRoundTable, "Camelot", leader: "lancelot", quest: holyGrail)
			}
			""");

		KnightsOfTheRoundTable knights = (KnightsOfTheRoundTable) appCtx.getBean("knights");
		HolyGrailQuest quest = (HolyGrailQuest) appCtx.getBean("holyGrail");

		assertThat(knights.getName()).isEqualTo("Camelot");
		assertThat(knights.leader).isEqualTo("lancelot");
		assertThat(knights.quest).isEqualTo(quest);
	}

	@Test
	void abstractBeanDefinition() {
		GenericApplicationContext appCtx = new GenericApplicationContext();
		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				abstractBean {
					leader = "Lancelot"
				}
				quest(HolyGrailQuest)
				knights(KnightsOfTheRoundTable, "Camelot") { bean ->
					bean.parent = abstractBean
					quest = quest
				}
			}
			""");

		KnightsOfTheRoundTable knights = (KnightsOfTheRoundTable) appCtx.getBean("knights");
		assertThat(knights).isNotNull();
		assertThatExceptionOfType(BeanIsAbstractException.class)
			.isThrownBy(() -> appCtx.getBean("abstractBean"));
		assertThat(knights.leader).isEqualTo("Lancelot");

		appCtx.close();
	}

	@Test
	void abstractBeanDefinitionWithClass() {
		GenericApplicationContext appCtx = new GenericApplicationContext();
		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				abstractBean(KnightsOfTheRoundTable) { bean ->
					bean.'abstract' = true
					leader = "Lancelot"
				}
				quest(HolyGrailQuest)
				knights("Camelot") { bean ->
					bean.parent = abstractBean
					quest = quest
				}
			}
			""");

		assertThatExceptionOfType(BeanIsAbstractException.class)
			.isThrownBy(() -> appCtx.getBean("abstractBean"));

		KnightsOfTheRoundTable knights = (KnightsOfTheRoundTable) appCtx.getBean("knights");
		assertThat(knights).isNotNull();
		assertThat(knights.leader).isEqualTo("Lancelot");

		appCtx.close();
	}

	@Test
	void scopes() {
		GenericApplicationContext appCtx = new GenericApplicationContext();
		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				myBean(ScopeTestBean) { bean ->
					bean.scope = "prototype"
				}
				myBean2(ScopeTestBean)
			}
			""");

		Object b1 = appCtx.getBean("myBean");
		Object b2 = appCtx.getBean("myBean");

		assertThat(b1).isNotSameAs(b2);

		b1 = appCtx.getBean("myBean2");
		b2 = appCtx.getBean("myBean2");

		assertThat(b1).isEqualTo(b2);

		appCtx.close();
	}

	@Test
	void simpleBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				bean1(Bean1Impl) {
					person = "homer"
					age = 45
					props = [overweight:"true", height:"1.8m"]
					children = ["bart", "lisa"]
				}
			}
			""");

		assertThat(appCtx.containsBean("bean1")).isTrue();
		Bean1 bean1 = (Bean1) appCtx.getBean("bean1");

		assertThat(bean1.getPerson()).isEqualTo("homer");
		assertThat(bean1.getAge()).isEqualTo(45);
		assertThat(bean1.getProps().getProperty("overweight")).isEqualTo("true");
		assertThat(bean1.getProps().getProperty("height")).isEqualTo("1.8m");
		assertThat(bean1.getChildren()).containsExactly("bart", "lisa");

		appCtx.close();
	}

	@Test
	void beanWithParentRef() {
		GenericApplicationContext parentAppCtx = new GenericApplicationContext();
		loadGroovyDsl(parentAppCtx, """
			package org.springframework.context.groovy
			beans {
				homer(Bean1Impl) {
					person = "homer"
					age = 45
					props = [overweight:true, height:"1.8m"]
					children = ["bart", "lisa"]
				}
			}
			""");

		GenericApplicationContext appCtx = new GenericApplicationContext(parentAppCtx);
		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				bart(Bean2) {
					person = "bart"
					parent = ref("homer", true)
				}
			}
			""");

		assertThat(appCtx.containsBean("bart")).isTrue();
		Bean2 bart = (Bean2) appCtx.getBean("bart");
		assertThat(bart.parent.getPerson()).isEqualTo("homer");
	}

	@Test
	void withAnonymousInnerBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				bart(Bean1Impl) {
					person = "bart"
					age = 11
				}
				lisa(Bean1Impl) {
					person = "lisa"
					age = 9
				}
				marge(Bean2) {
					person = "marge"
					bean1 =  { Bean1Impl b ->
								person = "homer"
								age = 45
								props = [overweight:true, height:"1.8m"]
								children = ["bart", "lisa"] }
					children = [bart, lisa]
				}
			}
			""");

		Bean2 marge = (Bean2) appCtx.getBean("marge");
		assertThat(marge.bean1.getPerson()).isEqualTo("homer");
	}

	@Test
	void withUntypedAnonymousInnerBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				homer(Bean1Factory)
				bart(Bean1Impl) {
					person = "bart"
					age = 11
				}
				lisa(Bean1Impl) {
					person = "lisa"
					age = 9
				}
				marge(Bean2) {
					person = "marge"
					bean1 =  { bean ->
								bean.factoryBean = "homer"
								bean.factoryMethod = "newInstance"
								person = "homer" }
					children = [bart, lisa]
				}
			}
			""");

		Bean2 marge = (Bean2) appCtx.getBean("marge");
		assertThat(marge.bean1.getPerson()).isEqualTo("homer");
	}

	@Test
	void beanReferences() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				homer(Bean1Impl) {
					person = "homer"
					age = 45
					props = [overweight:true, height:"1.8m"]
					children = ["bart", "lisa"]
				}
				bart(Bean1Impl) {
					person = "bart"
					age = 11
				}
				lisa(Bean1Impl) {
					person = "lisa"
					age = 9
				}
				marge(Bean2) {
					person = "marge"
					bean1 = homer
					children = [bart, lisa]
				}
			}
			""");

		Object homer = appCtx.getBean("homer");
		Bean2 marge = (Bean2) appCtx.getBean("marge");
		Bean1 bart = (Bean1) appCtx.getBean("bart");
		Bean1 lisa = (Bean1) appCtx.getBean("lisa");

		assertThat(marge.bean1).isEqualTo(homer);
		assertThat(marge.getChildren()).hasSize(2);

		assertThat(marge.getChildren()).containsExactly(bart, lisa);
	}

	@Test
	void beanWithConstructor() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				homer(Bean1Impl) {
					person = "homer"
					age = 45
				}
				marge(Bean3, "marge", homer) {
					age = 40
				}
			}
			""");

		Bean3 marge = (Bean3) appCtx.getBean("marge");
		assertThat(marge.getPerson()).isEqualTo("marge");
		assertThat(marge.bean1.getPerson()).isEqualTo("homer");
		assertThat(marge.getAge()).isEqualTo(40);
	}

	@Test
	void beanWithFactoryMethod() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				homer(Bean1Impl) {
					person = "homer"
					age = 45
				}
				def marge = marge(Bean4) {
					person = "marge"
				}
				marge.factoryMethod = "getInstance"
			}
			""");

		Bean4 marge = (Bean4) appCtx.getBean("marge");
		assertThat(marge.getPerson()).isEqualTo("marge");
	}

	@Test
	void beanWithFactoryMethodUsingClosureArgs() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				homer(Bean1Impl) {
					person = "homer"
					age = 45
				}
				marge(Bean4) { bean ->
					bean.factoryMethod = "getInstance"
					person = "marge"
				}
			}
			""");

		Bean4 marge = (Bean4) appCtx.getBean("marge");
		assertThat(marge.getPerson()).isEqualTo("marge");
	}

	@Test
	void beanWithFactoryMethodWithConstructorArgs() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				beanFactory(Bean1FactoryWithArgs){}

				homer(beanFactory:"newInstance", "homer") {
					age = 45
				}
				//Test with no closure body
				marge(beanFactory:"newInstance", "marge")

				//Test more verbose method
				mcBain("mcBain") { bean ->
					bean.factoryBean="beanFactory"
					bean.factoryMethod="newInstance"
				}
			}
			""");

		Bean1 homer = (Bean1) appCtx.getBean("homer");

		assertThat(homer.getPerson()).isEqualTo("homer");
		assertThat(homer.getAge()).isEqualTo(45);

		assertThat(((Bean1)appCtx.getBean("marge")).getPerson()).isEqualTo("marge");
		assertThat(((Bean1)appCtx.getBean("mcBain")).getPerson()).isEqualTo("mcBain");
	}

	@Test
	void getBeanDefinitions() {
		GenericApplicationContext appCtx = new GenericApplicationContext();
		GroovyBeanDefinitionReader reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
			package org.springframework.context.groovy
			beans {
				jeff(Bean1Impl) {
					person = 'jeff'
				}
				graeme(Bean1Impl) {
					person = 'graeme'
				}
				guillaume(Bean1Impl) {
					person = 'guillaume'
				}
			}
			""").getBytes()));

		appCtx.refresh();

		assertThat(reader.getRegistry().getBeanDefinitionCount()).as("beanDefinitions was the wrong size").isEqualTo(3);
		assertThat(reader.getRegistry().getBeanDefinition("jeff")).as("beanDefinitions did not contain jeff").isNotNull();
		assertThat(reader.getRegistry().getBeanDefinition("guillaume")).as("beanDefinitions did not contain guillaume").isNotNull();
		assertThat(reader.getRegistry().getBeanDefinition("graeme")).as("beanDefinitions did not contain graeme").isNotNull();
	}

	@Test
	void beanWithFactoryBean() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				myFactory(Bean1Factory)

				homer(myFactory) { bean ->
					bean.factoryMethod = "newInstance"
					person = "homer"
					age = 45
				}
			}
			""");

		Bean1 homer = (Bean1) appCtx.getBean("homer");

		assertThat(homer.getPerson()).isEqualTo("homer");
	}

	@Test
	void beanWithFactoryBeanAndMethod() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				myFactory(Bean1Factory)

				homer(myFactory:"newInstance") { bean ->
					person = "homer"
					age = 45
				}
			}
			""");

		Bean1 homer = (Bean1) appCtx.getBean("homer");
		assertThat(homer.getPerson()).isEqualTo("homer");
	}

	@Test
	void loadExternalBeans() {
		GenericApplicationContext appCtx = new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext.groovy");

		assertThat(appCtx.containsBean("foo")).isTrue();
		assertThat(appCtx.getBean("foo")).isEqualTo("hello");

		appCtx.close();
	}

	@Test
	void loadExternalBeansWithExplicitRefresh() {
		GenericGroovyApplicationContext appCtx = new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext.groovy");

		assertThat(appCtx.containsBean("foo")).isTrue();
		assertThat(appCtx.getBean("foo")).isEqualTo("hello");

		appCtx.close();
	}

	@Test
	void holyGrailWiring() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				quest(HolyGrailQuest)

				knights(KnightsOfTheRoundTable, "Bedivere") {
					quest = ref("quest")
				}
			}
			""");

		KnightsOfTheRoundTable knights = (KnightsOfTheRoundTable) appCtx.getBean("knights");
		knights.embarkOnQuest();
	}

	@Test
	void abstractBeanSpecifyingClass() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				abstractKnight(KnightsOfTheRoundTable) { bean ->
					bean.'abstract' = true
					leader = "King Arthur"
				}

				lancelot("lancelot") { bean ->
					bean.parent = ref("abstractKnight")
				}

				abstractPerson(Bean1Impl) { bean ->
					bean.'abstract'=true
					age = 45
				}
				homerBean { bean ->
					bean.parent = ref("abstractPerson")
					person = "homer"
				}
			}
			""");

		KnightsOfTheRoundTable lancelot = (KnightsOfTheRoundTable) appCtx.getBean("lancelot");
		assertThat(lancelot.leader).isEqualTo("King Arthur");
		assertThat(lancelot.name).isEqualTo("lancelot");

		Bean1 homerBean = (Bean1) appCtx.getBean("homerBean");

		assertThat(homerBean.getAge()).isEqualTo(45);
		assertThat(homerBean.getPerson()).isEqualTo("homer");
	}

	@Test
	void groovyBeanDefinitionReaderWithScript() throws Exception {
		String script = """
			def appCtx = new org.springframework.context.support.GenericGroovyApplicationContext()
			appCtx.reader.beans {
				quest(org.springframework.context.groovy.HolyGrailQuest)

				knights(org.springframework.context.groovy.KnightsOfTheRoundTable, "Bedivere") {
					quest = quest
				}
			}
			appCtx.refresh()
			return appCtx
			""";

		GenericGroovyApplicationContext appCtx = (GenericGroovyApplicationContext) new GroovyShell().evaluate(script);

		KnightsOfTheRoundTable knights = (KnightsOfTheRoundTable) appCtx.getBean("knights");
		knights.embarkOnQuest();
	}

	// test for GRAILS-5057
	@Test
	void registerBeans() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				personA(AdvisedPersonImpl) {
					name = "Bob"
				}
			}
			""");

		assertThat(((AdvisedPerson)appCtx.getBean("personA")).getName()).isEqualTo("Bob");

		appCtx = new GenericApplicationContext();
		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				personA(AdvisedPersonImpl) {
					name = "Fred"
				}
			}
			""");

		assertThat(((AdvisedPerson)appCtx.getBean("personA")).getName()).isEqualTo("Fred");
	}

	@Test
	void listOfBeansAsConstructorArg() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				someotherbean(SomeOtherClass, new File('somefile.txt'))
				someotherbean2(SomeOtherClass, new File('somefile.txt'))

				somebean(SomeClass, [someotherbean, someotherbean2])
			}
			""");

		assertThat(appCtx.containsBean("someotherbean")).isTrue();
		assertThat(appCtx.containsBean("someotherbean2")).isTrue();
		assertThat(appCtx.containsBean("somebean")).isTrue();
	}

	@Test
	void beanWithListAndMapConstructor() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				bart(Bean1Impl) {
					person = "bart"
					age = 11
				}
				lisa(Bean1Impl) {
					person = "lisa"
					age = 9
				}

				beanWithList(Bean5, [bart, lisa])

				// test runtime references both as ref() and as plain name
				beanWithMap(Bean6, [bart:bart, lisa:ref('lisa')])
			}
			""");

		Bean5 beanWithList = (Bean5) appCtx.getBean("beanWithList");
		assertThat(beanWithList.people).extracting(Bean1::getPerson).containsExactly("bart", "lisa");

		Bean6 beanWithMap = (Bean6) appCtx.getBean("beanWithMap");
		assertThat(beanWithMap.peopleByName).hasSize(2);
		assertThat(beanWithMap.peopleByName.get("lisa").getAge()).isEqualTo(9);
		assertThat(beanWithMap.peopleByName.get("bart").getPerson()).isEqualTo("bart");
	}

	@Test
	void anonymousInnerBeanViaBeanMethod() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				bart(Bean1Impl) {
					person = "bart"
					age = 11
				}
				lisa(Bean1Impl) {
					person = "lisa"
					age = 9
				}
				marge(Bean2) {
					person = "marge"
					bean1 =  bean(Bean1Impl) {
						person = "homer"
						age = 45
						props = [overweight:true, height:"1.8m"]
						children = ["bart", "lisa"]
					}
					children = [bart, lisa]
				}
			}
			""");

		Bean2 marge = (Bean2) appCtx.getBean("marge");
		assertThat(marge.bean1.getPerson()).isEqualTo("homer");
	}

	@Test
	void anonymousInnerBeanViaBeanMethodWithConstructorArgs() {
		GenericApplicationContext appCtx = new GenericApplicationContext();

		loadGroovyDsl(appCtx, """
			package org.springframework.context.groovy
			beans {
				bart(Bean1Impl) {
					person = "bart"
					age = 11
				}
				lisa(Bean1Impl) {
					person = "lisa"
					age = 9
				}
				marge(Bean2) {
					person = "marge"
					bean3 =  bean(Bean3, "homer", lisa) {
						person = "homer"
						age = 45
					}
					children = [bart, lisa]
				}
			}
			""");

		Bean2 marge = (Bean2) appCtx.getBean("marge");

		assertThat(marge.bean3.getPerson()).isEqualTo("homer");
		assertThat(marge.bean3.bean1.getPerson()).isEqualTo("lisa");
	}

	private static void loadGroovyDsl(GenericApplicationContext context, String script) {
		new GroovyBeanDefinitionReader(context).loadBeanDefinitions(new ByteArrayResource((script).getBytes()));
		context.refresh();
	}

}

class HolyGrailQuest {
	void start() {
		/* no-op */
	}
}

class KnightsOfTheRoundTable {
	String name;
	String leader;

	KnightsOfTheRoundTable(String n) {
		this.name = n;
	}

	HolyGrailQuest quest;

	void embarkOnQuest() {
		quest.start();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLeader() {
		return leader;
	}

	public void setLeader(String leader) {
		this.leader = leader;
	}

	public HolyGrailQuest getQuest() {
		return quest;
	}

	public void setQuest(HolyGrailQuest quest) {
		this.quest = quest;
	}
}

// simple bean
interface Bean1 {
	String getPerson();
	void setPerson(String person);
	int getAge();
	void setAge(int age);
	Properties getProps();
	void setProps(Properties props);
	List<String> getChildren();
	void setChildren(List<String> children);
}

class Bean1Impl implements Bean1 {
	String person;
	int age;
	Properties props = new Properties();
	List<String> children = new ArrayList<>();
	@Override
	public String getPerson() {
		return person;
	}
	@Override
	public void setPerson(String person) {
		this.person = person;
	}
	@Override
	public int getAge() {
		return age;
	}
	@Override
	public void setAge(int age) {
		this.age = age;
	}
	@Override
	public Properties getProps() {
		return props;
	}
	@Override
	public void setProps(Properties props) {
		this.props.putAll(props);
	}
	@Override
	public List<String> getChildren() {
		return children;
	}
	@Override
	public void setChildren(List<String> children) {
		this.children = children;
	}
}

// bean referencing other bean
class Bean2 {
	int age;
	Bean1 bean1;
	Bean3 bean3;
	String person;
	Bean1 parent;
	List<Bean1> children = new ArrayList<>();
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public String getPerson() {
		return person;
	}
	public void setPerson(String person) {
		this.person = person;
	}
	public Bean1 getParent() {
		return parent;
	}
	public void setParent(Bean1 parent) {
		this.parent = parent;
	}
	public Bean1 getBean1() {
		return bean1;
	}
	public void setBean1(Bean1 bean1) {
		this.bean1 = bean1;
	}
	public Bean3 getBean3() {
		return bean3;
	}
	public void setBean3(Bean3 bean3) {
		this.bean3 = bean3;
	}
	public List<Bean1> getChildren() {
		return children;
	}
	public void setChildren(List<Bean1> children) {
		this.children = children;
	}
}

// bean with constructor args
class Bean3 {
	Bean3(String person, Bean1 bean1) {
		this.person = person;
		this.bean1 = bean1;
	}
	String person;
	Bean1 bean1;
	int age;
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public String getPerson() {
		return person;
	}
	public void setPerson(String person) {
		this.person = person;
	}
	public Bean1 getBean1() {
		return bean1;
	}
	public void setBean1(Bean1 bean1) {
		this.bean1 = bean1;
	}
}

// bean with factory method
class Bean4 {
	private Bean4() {}
	static Bean4 getInstance() {
		return new Bean4();
	}
	String person;
	public String getPerson() {
		return person;
	}
	public void setPerson(String person) {
		this.person = person;
	}
}

// bean with List-valued constructor arg
class Bean5 {
	Bean5(List<Bean1> people) {
		this.people = people;
	}
	List<Bean1> people;
}

// bean with Map-valued constructor arg
class Bean6 {
	Bean6(Map<String, Bean1> peopleByName) {
		this.peopleByName = peopleByName;
	}
	Map<String, Bean1> peopleByName;
}

// a factory bean
class Bean1Factory {
	Bean1 newInstance() {
		return new Bean1Impl();
	}
}

class ScopeTestBean {
}

class TestScope implements Scope {

	int instanceCount;

	@Override
	public Object remove(String name) {
		// do nothing
		return null;
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
	}

	@Override
	public String getConversationId() {
		return null;
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		instanceCount++;
		return objectFactory.getObject();
	}

	@Override
	public Object resolveContextualObject(String s) {
		return null;
	}
}

class BirthdayCardSender {
	List<AdvisedPerson> peopleSentCards = new ArrayList<>();

	public void onBirthday(AdvisedPerson person) {
		peopleSentCards.add(person);
	}
}

interface AdvisedPerson {
	void birthday();
	int getAge();
	void setAge(int age);
	String getName();
	void setName(String name);
}

@Component("person")
class AdvisedPersonImpl implements AdvisedPerson {
	int age;
	String name;

	@Override
	public void birthday() {
		++age;
	}

	@Override
	public int getAge() {
		return age;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}

class SomeClass {
	public SomeClass(List<SomeOtherClass> soc) {}
}

class SomeOtherClass {
	public SomeOtherClass(File f) {}
}

// a factory bean that takes arguments
class Bean1FactoryWithArgs {
	Bean1 newInstance(String name) {
		Bean1 bean = new Bean1Impl();
		bean.setPerson(name);
		return bean;
	}
}
