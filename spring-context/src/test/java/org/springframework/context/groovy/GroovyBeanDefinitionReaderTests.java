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
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericGroovyApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class GroovyBeanDefinitionReaderTests {

	@Test
	void importSpringXml() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
			beans {
				importBeans "classpath:org/springframework/context/groovy/test.xml"
			}
			""").getBytes()));

		appCtx.refresh();

		var foo = appCtx.getBean("foo");
		assertThat(foo).isEqualTo("hello");
	}

	@Test
	void importBeansFromGroovy() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
			beans {
				importBeans "classpath:org/springframework/context/groovy/applicationContext.groovy"
			}
			""").getBytes()));

		appCtx.refresh();

		var foo = appCtx.getBean("foo");
		assertThat(foo).isEqualTo("hello");
	}

	@Test
	void singletonPropertyOnBeanDefinition() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy;
		beans {
			singletonBean(Bean1) { bean ->
				bean.singleton = true
			}
			nonSingletonBean(Bean1) { bean ->
				bean.singleton = false
			}
			unSpecifiedScopeBean(Bean1)
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat(appCtx.isSingleton("singletonBean")).as("singletonBean should have been a singleton").isTrue();
		assertThat(appCtx.isSingleton("nonSingletonBean")).as("nonSingletonBean should not have been a singleton").isFalse();
		assertThat(appCtx.isSingleton("unSpecifiedScopeBean")).as("unSpecifiedScopeBean should not have been a singleton").isTrue();
	}

	@Test
	void inheritPropertiesFromAbstractBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy;
		beans {
			myB(Bean1){
				person = "wombat"
			}

			myAbstractA(Bean2){ bean ->
				bean."abstract" = true
				age = 10
				bean1 = myB
			}
			myConcreteB {
				it.parent = myAbstractA
			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean2 bean = (Bean2) appCtx.getBean("myConcreteB");
		assertThat(bean.age).isEqualTo(10);
		assertThat(bean.bean1).isNotNull();
	}

	@Test
	void contextComponentScanSpringTag() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		beans {
			xmlns context:"http://www.springframework.org/schema/context"

			context.'component-scan'( 'base-package':"org.springframework.context.groovy" )
		}
		""").getBytes()));
		appCtx.refresh();

		var p = appCtx.getBean("person");
		assertThat(p).isInstanceOf(AdvisedPerson.class);
	}

	@Test
	void useSpringNamespaceAsMethod() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy;
		beans {
			xmlns aop:"http://www.springframework.org/schema/aop"

			fred(AdvisedPerson) {
				name = "Fred"
				age = 45
			}
			birthdayCardSenderAspect(BirthdayCardSender)

			aop {
				config("proxy-target-class":true) {
					aspect( id:"sendBirthdayCard",ref:"birthdayCardSenderAspect" ) {
						after method:"onBirthday", pointcut: "execution(void org.springframework.context.groovy.AdvisedPerson.birthday()) and this(person)"
					}
				}
			}
		}
		""").getBytes()));

		appCtx.refresh();

		AdvisedPerson fred = (AdvisedPerson) appCtx.getBean("fred");
		assertThat(fred).isInstanceOf(SpringProxy.class);
		fred.birthday();

		BirthdayCardSender birthDaySender = (BirthdayCardSender) appCtx.getBean("birthdayCardSenderAspect");

		assertThat(birthDaySender.peopleSentCards).hasSize(1);
		assertThat(birthDaySender.peopleSentCards.get(0).getName()).isEqualTo("Fred");
	}

	@SuppressWarnings("unchecked")
	@Test
	void useTwoSpringNamespaces() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		TestScope scope = new TestScope();
		appCtx.getBeanFactory().registerScope("test", scope);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			xmlns aop:"http://www.springframework.org/schema/aop"
			xmlns util:"http://www.springframework.org/schema/util"
			bean1(Bean1) { bean ->
				bean.scope = "test"
				aop.'scoped-proxy'()
			}
			util.list(id: 'foo') {
				value 'one'
				value 'two'
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat((List<String>)appCtx.getBean("foo")).containsExactly("one", "two");

		assertThat(appCtx.getBean("bean1")).isNotNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();

		// should only be true because bean not initialized until proxy called
		assertThat(scope.instanceCount).isEqualTo(2);

		appCtx = new GenericApplicationContext();
		reader = new GroovyBeanDefinitionReader(appCtx);
		appCtx.getBeanFactory().registerScope("test", scope);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		import java.util.ArrayList
		beans {
			xmlns aop:"http://www.springframework.org/schema/aop",
				util:"http://www.springframework.org/schema/util"
			bean1(Bean1) { bean ->
				bean.scope = "test"
				aop.'scoped-proxy'()
			}
			util.list(id: 'foo') {
				value 'one'
				value 'two'
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat((List<String>)appCtx.getBean("foo")).containsExactly("one", "two");

		assertThat(appCtx.getBean("bean1")).isNotNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();
		assertThat(((Bean1)appCtx.getBean("bean1")).getPerson()).isNull();

		// should only be true because bean not initialized until proxy called
		assertThat(scope.instanceCount).isEqualTo(4);
	}

	@Test
	void springAopSupport() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			xmlns aop:"http://www.springframework.org/schema/aop"

			fred(AdvisedPerson) {
				name = "Fred"
				age = 45
			}
			birthdayCardSenderAspect(BirthdayCardSender)

			aop.config("proxy-target-class":true) {
				aspect( id:"sendBirthdayCard",ref:"birthdayCardSenderAspect" ) {
					after method:"onBirthday", pointcut: "execution(void org.springframework.context.groovy.AdvisedPerson.birthday()) and this(person)"
				}
			}
		}
		""").getBytes()));

		appCtx.refresh();

		AdvisedPerson fred = (AdvisedPerson) appCtx.getBean("fred");
		assertThat(fred).isInstanceOf(SpringProxy.class);
		fred.birthday();

		BirthdayCardSender birthDaySender = (BirthdayCardSender) appCtx.getBean("birthdayCardSenderAspect");

		assertThat(birthDaySender.peopleSentCards).hasSize(1);
		assertThat(birthDaySender.peopleSentCards.get(0).getName()).isEqualTo("Fred");
	}

	@Test
	void springScopedProxyBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		TestScope scope = new TestScope();
		appCtx.getBeanFactory().registerScope("test", scope);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			xmlns aop:"http://www.springframework.org/schema/aop"
			scopedList(Bean1) { bean ->
				bean.scope = "test"
				aop.'scoped-proxy'()
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat(appCtx.getBean("scopedList")).isNotNull();
		assertThat(((Bean1)appCtx.getBean("scopedList")).getPerson()).isNull();
		assertThat(((Bean1)appCtx.getBean("scopedList")).getPerson()).isNull();

		// should only be true because bean not initialized until proxy called
		assertThat(scope.instanceCount).isEqualTo(2);
	}

	@Test
	void springNamespaceBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			xmlns util: 'http://www.springframework.org/schema/util'
			util.list(id: 'foo') {
				value 'one'
				value 'two'
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat((List<String>)appCtx.getBean("foo")).contains("one", "two");
	}

	@Test
	void namedArgumentConstructor() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			holyGrail(HolyGrailQuest)
			knights(KnightOfTheRoundTable, "Camelot", leader:"lancelot", quest: holyGrail)
		}
		""").getBytes()));
		appCtx.refresh();

		KnightOfTheRoundTable knights = (KnightOfTheRoundTable) appCtx.getBean("knights");
		HolyGrailQuest quest = (HolyGrailQuest) appCtx.getBean("holyGrail");

		assertThat(knights.getName()).isEqualTo("Camelot");
		assertThat(knights.leader).isEqualTo("lancelot");
		assertThat(knights.quest).isEqualTo(quest);
	}

	@Test
	void abstractBeanDefinition() {
		var appCtx = new GenericGroovyApplicationContext();
		appCtx.getReader().loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			abstractBean {
				leader = "Lancelot"
			}
			quest(HolyGrailQuest)
			knights(KnightOfTheRoundTable, "Camelot") { bean ->
				bean.parent = abstractBean
				quest = quest
			}
		}
		""").getBytes()));
		appCtx.refresh();

		KnightOfTheRoundTable knights = (KnightOfTheRoundTable) appCtx.getProperty("knights");
		assertThat(knights).isNotNull();
		assertThatExceptionOfType(org.springframework.beans.factory.BeanIsAbstractException.class).isThrownBy(() ->
			appCtx.getProperty("abstractBean"));
		assertThat(knights.leader).isEqualTo("Lancelot");
	}

	@Test
	void abstractBeanDefinitionWithClass() {
		var appCtx = new GenericGroovyApplicationContext();
		appCtx.getReader().loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			abstractBean(KnightOfTheRoundTable) { bean ->
				bean.'abstract' = true
				leader = "Lancelot"
			}
			quest(HolyGrailQuest)
			knights("Camelot") { bean ->
				bean.parent = abstractBean
				quest = quest
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThatExceptionOfType(org.springframework.beans.factory.BeanIsAbstractException.class).isThrownBy(() ->
			appCtx.getProperty("abstractBean"));

		KnightOfTheRoundTable knights = (KnightOfTheRoundTable) appCtx.getProperty("knights");
		assertThat(knights).isNotNull();
		assertThat(knights.leader).isEqualTo("Lancelot");
	}

	@Test
	void scopes() {
		var appCtx = new GenericGroovyApplicationContext();
		appCtx.getReader().loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			myBean(ScopeTestBean) { bean ->
				bean.scope = "prototype"
			}
			myBean2(ScopeTestBean)
		}
		""").getBytes()));
		appCtx.refresh();

		var b1 = appCtx.getProperty("myBean");
		var b2 = appCtx.getProperty("myBean");

		assertThat(b1).isNotSameAs(b2);

		b1 = appCtx.getProperty("myBean2");
		b2 = appCtx.getProperty("myBean2");

		assertThat(b1).isEqualTo(b2);
	}

	@Test
	void simpleBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			bean1(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:"true", height:"1.8m"]
				children = ["bart", "lisa"]
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat(appCtx.containsBean("bean1")).isTrue();
		Bean1 bean1 = (Bean1) appCtx.getBean("bean1");

		assertThat(bean1.person).isEqualTo("homer");
		assertThat(bean1.age).isEqualTo(45);
		assertThat(bean1.props.getProperty("overweight")).isEqualTo("true");
		assertThat(bean1.props.getProperty("height")).isEqualTo("1.8m");
		assertThat(bean1.children).containsExactly("bart", "lisa");

	}

	@Test
	void beanWithParentRef() {
		var parentAppCtx = new GenericApplicationContext();
		var parentBeanReader = new GroovyBeanDefinitionReader(parentAppCtx);
		parentBeanReader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			homer(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]
			}
		}
		""").getBytes()));
		parentAppCtx.refresh();

		var appCtx = new GenericApplicationContext(parentAppCtx);
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			bart(Bean2) {
				person = "bart"
				parent = ref("homer", true)
			}
		}
		""").getBytes()));
		appCtx.refresh();

		assertThat(appCtx.containsBean("bart")).isTrue();
		Bean2 bart = (Bean2) appCtx.getBean("bart");
		assertThat(bart.parent.person).isEqualTo("homer");
	}

	@Test
	void withAnonymousInnerBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9
			}
			marge(Bean2) {
				person = "marge"
				bean1 =  { Bean1 b ->
							person = "homer"
							age = 45
							props = [overweight:true, height:"1.8m"]
							children = ["bart", "lisa"] }
				children = [bart, lisa]
			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean2 marge = (Bean2) appCtx.getBean("marge");
		assertThat(marge.bean1.person).isEqualTo("homer");
	}

	@Test
	void withUntypedAnonymousInnerBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			homer(Bean1Factory)
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
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
		""").getBytes()));
		appCtx.refresh();

		Bean2 marge = (Bean2) appCtx.getBean("marge");
		assertThat(marge.bean1.person).isEqualTo("homer");
	}

	@Test
	void beanReferences() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			homer(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]
			}
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9
			}
			marge(Bean2) {
				person = "marge"
				bean1 = homer
				children = [bart, lisa]
			}
		}
		""").getBytes()));
		appCtx.refresh();

		var homer = appCtx.getBean("homer");
		Bean2 marge = (Bean2) appCtx.getBean("marge");
		Bean1 bart = (Bean1) appCtx.getBean("bart");
		Bean1 lisa = (Bean1) appCtx.getBean("lisa");

		assertThat(marge.bean1).isEqualTo(homer);
		assertThat(marge.children).hasSize(2);

		assertThat(marge.children).containsExactlyInAnyOrder(bart, lisa);
	}

	@Test
	void beanWithConstructor() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			marge(Bean3, "marge", homer) {
				age = 40
			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean3 marge = (Bean3) appCtx.getBean("marge");
		assertThat(marge.person).isEqualTo("marge");
		assertThat(marge.bean1.person).isEqualTo("homer");
		assertThat(marge.age).isEqualTo(40);
	}

	@Test
	void beanWithFactoryMethod() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			def marge = marge(Bean4) {
				person = "marge"
			}
			marge.factoryMethod = "getInstance"
		}
		""").getBytes()));
		appCtx.refresh();

		Bean4 marge = (Bean4) appCtx.getBean("marge");
		assertThat(marge.person).isEqualTo("marge");
	}

	@Test
	void beanWithFactoryMethodUsingClosureArgs() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			marge(Bean4) { bean ->
				bean.factoryMethod = "getInstance"
				person = "marge"
			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean4 marge = (Bean4) appCtx.getBean("marge");
		assertThat(marge.person).isEqualTo("marge");
	}

	@Test
	void beanWithFactoryMethodWithConstructorArgs() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			beanFactory(Bean1FactoryWithArgs){}

			homer(beanFactory:"newInstance", "homer") {
				age = 45
			}
			//Test with no closure body
			marge(beanFactory:"newInstance", "marge")

			//Test more verbose method
			mcBain("mcBain"){
				bean ->
				bean.factoryBean="beanFactory"
				bean.factoryMethod="newInstance"

			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean1 homer = (Bean1) appCtx.getBean("homer");

		assertThat(homer.person).isEqualTo("homer");
		assertThat(homer.age).isEqualTo(45);

		assertThat(((Bean1)appCtx.getBean("marge")).person).isEqualTo("marge");

		assertThat(((Bean1)appCtx.getBean("mcBain")).person).isEqualTo("mcBain");
	}

	@Test
	void getBeanDefinitions() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			jeff(Bean1) {
				person = 'jeff'
			}
			graeme(Bean1) {
				person = 'graeme'
			}
			guillaume(Bean1) {
				person = 'guillaume'
			}
		}
		""").getBytes()));

		assertThat(reader.getRegistry().getBeanDefinitionCount()).as("beanDefinitions was the wrong size").isEqualTo(3);
		assertThat(reader.getRegistry().getBeanDefinition("jeff")).as("beanDefinitions did not contain jeff").isNotNull();
		assertThat(reader.getRegistry().getBeanDefinition("guillaume")).as("beanDefinitions did not contain guillaume").isNotNull();
		assertThat(reader.getRegistry().getBeanDefinition("graeme")).as("beanDefinitions did not contain graeme").isNotNull();
	}

	@Test
	void beanWithFactoryBean() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			myFactory(Bean1Factory)

			homer(myFactory) { bean ->
				bean.factoryMethod = "newInstance"
				person = "homer"
				age = 45
			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean1 homer = (Bean1) appCtx.getBean("homer");

		assertThat(homer.person).isEqualTo("homer");
	}

	@Test
	void beanWithFactoryBeanAndMethod() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			myFactory(Bean1Factory)

			homer(myFactory:"newInstance") { bean ->
				person = "homer"
				age = 45
			}
		}
		""").getBytes()));

		appCtx.refresh();

		Bean1 homer = (Bean1) appCtx.getBean("homer");
		assertThat(homer.person).isEqualTo("homer");
	}

	@Test
	void loadExternalBeans() {
		var appCtx = new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext.groovy");

		assertThat(appCtx.containsBean("foo")).isTrue();
		var foo = appCtx.getBean("foo");
		assertThat(foo).isEqualTo("hello");
	}

	@Test
	void loadExternalBeansWithExplicitRefresh() {
		var appCtx = new GenericGroovyApplicationContext();
		appCtx.load("org/springframework/context/groovy/applicationContext.groovy");
		appCtx.refresh();

		assertThat(appCtx.containsBean("foo")).isTrue();
		var foo = appCtx.getBean("foo");
		assertThat(foo).isEqualTo("hello");
	}

	@Test
	void holyGrailWiring() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			quest(HolyGrailQuest)

			knight(KnightOfTheRoundTable, "Bedivere") {
				quest = ref("quest")
			}
		}
		""").getBytes()));

		appCtx.refresh();

		KnightOfTheRoundTable knight = (KnightOfTheRoundTable) appCtx.getBean("knight");
		knight.embarkOnQuest();
	}

	@Test
	void abstractBeanSpecifyingClass() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			abstractKnight(KnightOfTheRoundTable) { bean ->
				bean.'abstract' = true
				leader = "King Arthur"
			}

			lancelot("lancelot") { bean ->
				bean.parent = ref("abstractKnight")
			}

			abstractPerson(Bean1) { bean ->
				bean.'abstract'=true
				age = 45
			}
			homerBean { bean ->
				bean.parent = ref("abstractPerson")
				person = "homer"
			}
		}
		""").getBytes()));
		appCtx.refresh();

		KnightOfTheRoundTable lancelot = (KnightOfTheRoundTable) appCtx.getBean("lancelot");
		assertThat(lancelot.leader).isEqualTo("King Arthur");
		assertThat(lancelot.name).isEqualTo("lancelot");

		Bean1 homerBean = (Bean1) appCtx.getBean("homerBean");

		assertThat(homerBean.age).isEqualTo(45);
		assertThat(homerBean.person).isEqualTo("homer");
	}

	@Test
	void groovyBeanDefinitionReaderWithScript() throws Exception {
		var script = """
def appCtx = new org.springframework.context.support.GenericGroovyApplicationContext()
appCtx.reader.beans {
quest(org.springframework.context.groovy.HolyGrailQuest) {}

knight(org.springframework.context.groovy.KnightOfTheRoundTable, "Bedivere") { quest = quest }
}
appCtx.refresh()
return appCtx
""";
		GenericGroovyApplicationContext appCtx = (GenericGroovyApplicationContext) new GroovyShell().evaluate(script);

		KnightOfTheRoundTable knight = (KnightOfTheRoundTable) appCtx.getBean("knight");
		knight.embarkOnQuest();
	}

	// test for GRAILS-5057
	@Test
	void registerBeans() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			personA(AdvisedPerson) {
				name = "Bob"
			}
		}
		""").getBytes()));

		appCtx.refresh();
		assertThat(((AdvisedPerson)appCtx.getBean("personA")).name).isEqualTo("Bob");

		appCtx = new GenericApplicationContext();
		reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			personA(AdvisedPerson) {
				name = "Fred"
			}
		}
		""").getBytes()));

		appCtx.refresh();
		assertThat(((AdvisedPerson)appCtx.getBean("personA")).name).isEqualTo("Fred");
	}

	@Test
	void listOfBeansAsConstructorArg() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);

		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			someotherbean(SomeOtherClass, new File('somefile.txt'))
			someotherbean2(SomeOtherClass, new File('somefile.txt'))

			somebean(SomeClass,  [someotherbean, someotherbean2])
		}
		""").getBytes()));

		assertThat(appCtx.containsBean("someotherbean")).isTrue();
		assertThat(appCtx.containsBean("someotherbean2")).isTrue();
		assertThat(appCtx.containsBean("somebean")).isTrue();
	}

	@Test
	void beanWithListAndMapConstructor() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9
			}

			beanWithList(Bean5, [bart, lisa])

			// test runtime references both as ref() and as plain name
			beanWithMap(Bean6, [bart:bart, lisa:ref('lisa')])
		}
		""").getBytes()));
		appCtx.refresh();

		Bean5 beanWithList = (Bean5) appCtx.getBean("beanWithList");
		assertThat(beanWithList.people).hasSize(2);
		assertThat(beanWithList.people.get(0).person).isEqualTo("bart");

		Bean6 beanWithMap = (Bean6) appCtx.getBean("beanWithMap");
		assertThat(beanWithMap.peopleByName.get("lisa").age).isEqualTo(9);
		assertThat(beanWithMap.peopleByName.get("bart").person).isEqualTo("bart");
	}

	@Test
	void anonymousInnerBeanViaBeanMethod() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
				person = "lisa"
				age = 9
			}
			marge(Bean2) {
				person = "marge"
				bean1 =  bean(Bean1) {
					person = "homer"
					age = 45
					props = [overweight:true, height:"1.8m"]
					children = ["bart", "lisa"]
				}
				children = [bart, lisa]
			}
		}
		""").getBytes()));
		appCtx.refresh();

		Bean2 marge = (Bean2) appCtx.getBean("marge");
		assertThat(marge.bean1.person).isEqualTo("homer");
	}

	@Test
	void anonymousInnerBeanViaBeanMethodWithConstructorArgs() {
		var appCtx = new GenericApplicationContext();
		var reader = new GroovyBeanDefinitionReader(appCtx);
		reader.loadBeanDefinitions(new ByteArrayResource(("""
		package org.springframework.context.groovy
		beans {
			bart(Bean1) {
				person = "bart"
				age = 11
			}
			lisa(Bean1) {
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
		""").getBytes()));
		appCtx.refresh();

		Bean2 marge = (Bean2) appCtx.getBean("marge");

		assertThat(marge.bean3.person).isEqualTo("homer");
		assertThat(marge.bean3.bean1.person).isEqualTo("lisa");
	}

}

class HolyGrailQuest {
	void start() { System.out.println("lets begin"); }
}

class KnightOfTheRoundTable {
	String name;
	String leader;

	KnightOfTheRoundTable(String n) {
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
class Bean1 {
	String person;
	int age;
	Properties props = new Properties();
	List<String> children = new ArrayList<>();
	public String getPerson() {
		return person;
	}
	public void setPerson(String person) {
		this.person = person;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public Properties getProps() {
		return props;
	}
	public void setProps(Properties props) {
		this.props.putAll(props);
	}
	public List<String> getChildren() {
		return children;
	}
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
		return new Bean1();
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
		return "mock";
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		instanceCount++;
		return objectFactory.getObject();
	}

	@Override
	public Object resolveContextualObject(String s) {
		return null;  // noop
	}
}

class BirthdayCardSender {
	List<AdvisedPerson> peopleSentCards = new ArrayList<>();

	public void onBirthday(AdvisedPerson person) {
		peopleSentCards.add(person);
	}
}

@Component(value = "person")
class AdvisedPerson {
	int age;
	String name;

	public void birthday() {
		++age;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getName() {
		return name;
	}

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
		Bean1 bean = new Bean1();
		bean.person = name;
		return bean;
	}
}
