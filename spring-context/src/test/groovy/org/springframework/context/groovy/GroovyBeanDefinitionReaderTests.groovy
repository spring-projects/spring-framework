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

package org.springframework.context.groovy

import org.springframework.aop.SpringProxy
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.Scope
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.GenericGroovyApplicationContext
import org.springframework.stereotype.Component

/**
 * @author Jeff Brown
 */
class GroovyBeanDefinitionReaderTests extends GroovyTestCase {

	void testImportSpringXml() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
			importBeans "classpath:org/springframework/context/groovy/test.xml"
		}

		appCtx.refresh()

		def foo = appCtx.getBean("foo")
		assertEquals "hello", foo
	}

	void testImportBeansFromGroovy() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
			importBeans "classpath:org/springframework/context/groovy/applicationContext.groovy"
		}

		appCtx.refresh()

		def foo = appCtx.getBean("foo")
		assertEquals "hello", foo
	}

	void testSingletonPropertyOnBeanDefinition() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			singletonBean(Bean1) { bean ->
				bean.singleton = true
			}
			nonSingletonBean(Bean1) { bean ->
				bean.singleton = false
			}
			unSpecifiedScopeBean(Bean1)
		}
		appCtx.refresh()

		assertTrue 'singletonBean should have been a singleton', appCtx.isSingleton('singletonBean')
		assertFalse 'nonSingletonBean should not have been a singleton', appCtx.isSingleton('nonSingletonBean')
		assertTrue 'unSpecifiedScopeBean should not have been a singleton', appCtx.isSingleton('unSpecifiedScopeBean')
	}

	void testInheritPropertiesFromAbstractBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
			myB(Bean1){
				person = "wombat"
			}

			myAbstractA(Bean2){ bean ->
				bean.'abstract' = true
				age = 10
				bean1 = myB
			}
			myConcreteB {
				it.parent = myAbstractA
			}
		}

		appCtx.refresh()

		def bean = appCtx.getBean("myConcreteB")
		assertEquals 10, bean.age
		assertNotNull bean.bean1
	}

	void testContextComponentScanSpringTag() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
			xmlns context:"http://www.springframework.org/schema/context"

			context.'component-scan'( 'base-package' :" org.springframework.context.groovy" )
		}

		appCtx.refresh()

		def p = appCtx.getBean("person")
		assertTrue(p instanceof AdvisedPerson)
		assertNotNull p
	}

	void testUseSpringNamespaceAsMethod() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
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

		appCtx.refresh()

		def fred = appCtx.getBean("fred")
		assertTrue (fred instanceof SpringProxy)
		fred.birthday()

		BirthdayCardSender birthDaySender = appCtx.getBean("birthdayCardSenderAspect")

		assertEquals 1, birthDaySender.peopleSentCards.size()
		assertEquals "Fred", birthDaySender.peopleSentCards[0].name
	}

	void testUseTwoSpringNamespaces() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		TestScope scope = new TestScope()
		appCtx.getBeanFactory().registerScope("test", scope)

		reader.beans {
			xmlns aop:"http://www.springframework.org/schema/aop"
			xmlns util:"http://www.springframework.org/schema/util"
			scopedList(ArrayList) { bean ->
				bean.scope = "test"
				aop.'scoped-proxy'()
			}
			util.list(id: 'foo') {
				value 'one'
				value 'two'
			}
		}
		appCtx.refresh()

		assert ['one', 'two'] == appCtx.getBean("foo")

		assertNotNull appCtx.getBean("scopedList")
		assertNotNull appCtx.getBean("scopedList").size()
		assertNotNull appCtx.getBean("scopedList").size()

		// should only be true because bean not initialized until proxy called
		assertEquals 2, scope.instanceCount

		appCtx = new GenericApplicationContext()
		reader = new GroovyBeanDefinitionReader(appCtx)
		appCtx.getBeanFactory().registerScope("test", scope)

		reader.beans {
			xmlns aop:"http://www.springframework.org/schema/aop",
				  util:"http://www.springframework.org/schema/util"
			scopedList(ArrayList) { bean ->
				bean.scope = "test"
				aop.'scoped-proxy'()
			}
			util.list(id: 'foo') {
				value 'one'
				value 'two'
			}
		}
		appCtx.refresh()

		assert ['one', 'two'] == appCtx.getBean("foo")

		assertNotNull appCtx.getBean("scopedList")
		assertNotNull appCtx.getBean("scopedList").size()
		assertNotNull appCtx.getBean("scopedList").size()

		// should only be true because bean not initialized until proxy called
		assertEquals 4, scope.instanceCount
	}

	void testSpringAopSupport() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
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

		appCtx.refresh()

		def fred = appCtx.getBean("fred")
		assertTrue (fred instanceof SpringProxy)
		fred.birthday()

		BirthdayCardSender birthDaySender = appCtx.getBean("birthdayCardSenderAspect")

		assertEquals 1, birthDaySender.peopleSentCards.size()
		assertEquals "Fred", birthDaySender.peopleSentCards[0].name
	}

	void testSpringScopedProxyBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		TestScope scope = new TestScope()
		appCtx.getBeanFactory().registerScope("test", scope)
		reader.beans {
			xmlns aop:"http://www.springframework.org/schema/aop"
			scopedList(ArrayList) { bean ->
				bean.scope = "test"
				aop.'scoped-proxy'()
			}
		}
		appCtx.refresh()

		assertNotNull appCtx.getBean("scopedList")
		assertNotNull appCtx.getBean("scopedList").size()
		assertNotNull appCtx.getBean("scopedList").size()

		// should only be true because bean not initialized until proxy called
		assertEquals 2, scope.instanceCount
	}

	void testSpringNamespaceBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			xmlns util: 'http://www.springframework.org/schema/util'
			util.list(id: 'foo') {
				value 'one'
				value 'two'
			}
		}
		appCtx.refresh()

		assert ['one', 'two'] == appCtx.getBean('foo')
	}

	void testNamedArgumentConstructor() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			holyGrail(HolyGrailQuest)
			knights(KnightOfTheRoundTable, "Camelot", leader:"lancelot", quest: holyGrail)
		}
		appCtx.refresh()

		KnightOfTheRoundTable knights = appCtx.getBean("knights")
		HolyGrailQuest quest = appCtx.getBean("holyGrail")

		assertEquals "Camelot", knights.name
		assertEquals "lancelot", knights.leader
		assertEquals quest, knights.quest
	}

	void testAbstractBeanDefinition() {
		def appCtx = new GenericGroovyApplicationContext()
		appCtx.reader.beans {
			abstractBean {
				leader = "Lancelot"
			}
			quest(HolyGrailQuest)
			knights(KnightOfTheRoundTable, "Camelot") { bean ->
				bean.parent = abstractBean
				quest = quest
			}
		}
		appCtx.refresh()

		def knights = appCtx.knights
		assert knights
		shouldFail(org.springframework.beans.factory.BeanIsAbstractException) {
			appCtx.abstractBean
		}
		assertEquals "Lancelot", knights.leader
	}

	void testAbstractBeanDefinitionWithClass() {
		def appCtx = new GenericGroovyApplicationContext()
		appCtx.reader.beans {
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
		appCtx.refresh()

		shouldFail(org.springframework.beans.factory.BeanIsAbstractException) {
			appCtx.abstractBean
		}
		def knights = appCtx.knights
		assert knights
		assertEquals "Lancelot", knights.leader
	}

	void testScopes() {
		def appCtx = new GenericGroovyApplicationContext()
		appCtx.reader.beans {
			myBean(ScopeTest) { bean ->
				bean.scope = "prototype"
			}
			myBean2(ScopeTest)
		}
		appCtx.refresh()

		def b1 = appCtx.myBean
		def b2 = appCtx.myBean

		assert b1 != b2

		b1 = appCtx.myBean2
		b2 = appCtx.myBean2

		assertEquals b1, b2
	}

	void testSimpleBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			bean1(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]
			}
		}
		appCtx.refresh()

		assert appCtx.containsBean("bean1")
		def bean1 = appCtx.getBean("bean1")

		assertEquals "homer", bean1.person
		assertEquals 45, bean1.age
		assertEquals true, bean1.props?.overweight
		assertEquals "1.8m", bean1.props?.height
		assertEquals(["bart", "lisa"], bean1.children)

	}

	void testBeanWithParentRef() {
		def parentAppCtx = new GenericApplicationContext()
		def parentBeanReader = new GroovyBeanDefinitionReader(parentAppCtx)
		parentBeanReader.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
				props = [overweight:true, height:"1.8m"]
				children = ["bart", "lisa"]
			}
		}
		parentAppCtx.refresh()

		def appCtx = new GenericApplicationContext(parentAppCtx)
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			bart(Bean2) {
				person = "bart"
				parent = ref("homer", true)
			}
		}
		appCtx.refresh()

		assert appCtx.containsBean("bart")
		def bart = appCtx.getBean("bart")
		assertEquals "homer",bart.parent?.person
	}

	void testWithAnonymousInnerBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def marge = appCtx.getBean("marge")
		assertEquals "homer", marge.bean1.person
	}

	void testWithUntypedAnonymousInnerBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def marge = appCtx.getBean("marge")
		assertEquals "homer", marge.bean1.person
	}

	void testBeanReferences() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def homer = appCtx.getBean("homer")
		def marge = appCtx.getBean("marge")
		def bart = appCtx.getBean("bart")
		def lisa = appCtx.getBean("lisa")

		assertEquals homer, marge.bean1
		assertEquals 2, marge.children.size()

		assertTrue marge.children.contains(bart)
		assertTrue marge.children.contains(lisa)
	}

	void testBeanWithConstructor() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			marge(Bean3, "marge", homer) {
				age = 40
			}
		}
		appCtx.refresh()

		def marge = appCtx.getBean("marge")
		assertEquals "marge", marge.person
		assertEquals "homer", marge.bean1.person
		assertEquals 40, marge.age
	}

	void testBeanWithFactoryMethod() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			def marge = marge(Bean4) {
				person = "marge"
			}
			marge.factoryMethod = "getInstance"
		}
		appCtx.refresh()

		def marge = appCtx.getBean("marge")

		assert "marge", marge.person
	}

	void testBeanWithFactoryMethodUsingClosureArgs() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			homer(Bean1) {
				person = "homer"
				age = 45
			}
			marge(Bean4) { bean ->
				bean.factoryMethod = "getInstance"
				person = "marge"
			}
		}
		appCtx.refresh()

		def marge = appCtx.getBean("marge")
		assert "marge", marge.person
	}

	void testBeanWithFactoryMethodWithConstructorArgs() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def homer = appCtx.getBean("homer")

		assert "homer", homer.person
		assert 45, homer.age

		assert "marge", appCtx.getBean("marge").person

		assert "mcBain", appCtx.getBean("mcBain").person
	}

	void testGetBeanDefinitions() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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

		junit.framework.TestCase.assertEquals 'beanDefinitions was the wrong size', 3, reader.registry.beanDefinitionCount
		assertNotNull 'beanDefinitions did not contain jeff', reader.registry.getBeanDefinition('jeff')
		assertNotNull 'beanDefinitions did not contain guillaume', reader.registry.getBeanDefinition('guillaume')
		assertNotNull 'beanDefinitions did not contain graeme', reader.registry.getBeanDefinition('graeme')
	}

	void testBeanWithFactoryBean() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			myFactory(Bean1Factory)

			homer(myFactory) { bean ->
				bean.factoryMethod = "newInstance"
				person = "homer"
				age = 45
			}
		}
		appCtx.refresh()

		def homer = appCtx.getBean("homer")

		assertEquals "homer", homer.person
	}

	void testBeanWithFactoryBeanAndMethod() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			myFactory(Bean1Factory)

			homer(myFactory:"newInstance") { bean ->
				person = "homer"
				age = 45
			}
		}

		appCtx.refresh()

		def homer = appCtx.getBean("homer")
		assertEquals "homer", homer.person
	}

	void testLoadExternalBeans() {
		def appCtx = new GenericGroovyApplicationContext("org/springframework/context/groovy/applicationContext.groovy")

		assert appCtx.containsBean("foo")
		def foo = appCtx.getBean("foo")
	}

	void testLoadExternalBeansWithExplicitRefresh() {
		def appCtx = new GenericGroovyApplicationContext()
		appCtx.load("org/springframework/context/groovy/applicationContext.groovy")
		appCtx.refresh()

		assert appCtx.containsBean("foo")
		def foo = appCtx.getBean("foo")
	}

	void testHolyGrailWiring() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
		quest(HolyGrailQuest)

		knight(KnightOfTheRoundTable, "Bedivere") {
		quest = ref("quest")
		}
		}

		appCtx.refresh()

		def knight = appCtx.getBean("knight")
		knight.embarkOnQuest()
	}

	void testAbstractBeanSpecifyingClass() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
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
		appCtx.refresh()

		def lancelot = appCtx.getBean("lancelot")
		assertEquals "King Arthur", lancelot.leader
		assertEquals "lancelot", lancelot.name

		def homerBean = appCtx.getBean("homerBean")

		assertEquals 45, homerBean.age
		assertEquals "homer", homerBean.person
	}

	void testGroovyBeanDefinitionReaderWithScript() {
		def script = '''
def appCtx = new org.springframework.context.support.GenericGroovyApplicationContext()
appCtx.reader.beans {
quest(org.springframework.context.groovy.HolyGrailQuest) {}

knight(org.springframework.context.groovy.KnightOfTheRoundTable, "Bedivere") { quest = quest }
}
appCtx.refresh()
return appCtx
'''
		def appCtx = new GroovyShell().evaluate(script)

		def knight = appCtx.getBean('knight')
		knight.embarkOnQuest()
	}

	// test for GRAILS-5057
	void testRegisterBeans() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
		   personA(AdvisedPerson) {
			   name = "Bob"
		   }
		}

		appCtx.refresh()
		assertEquals "Bob", appCtx.getBean("personA").name

		appCtx = new GenericApplicationContext()
		reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
			personA(AdvisedPerson) {
				name = "Fred"
			}
		}

		appCtx.refresh()
		assertEquals "Fred", appCtx.getBean("personA").name
	}

	void testListOfBeansAsConstructorArg() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)

		reader.beans {
			someotherbean(SomeOtherClass, new File('somefile.txt'))
			someotherbean2(SomeOtherClass, new File('somefile.txt'))

			somebean(SomeClass,  [someotherbean, someotherbean2])
		}

		assert appCtx.containsBean('someotherbean')
		assert appCtx.containsBean('someotherbean2')
		assert appCtx.containsBean('somebean')
	}

	void testBeanWithListAndMapConstructor() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def beanWithList = appCtx.getBean("beanWithList")
		assertEquals 2, beanWithList.people.size()
		assertEquals "bart", beanWithList.people[0].person

		def beanWithMap = appCtx.getBean("beanWithMap")
		assertEquals 9, beanWithMap.peopleByName.lisa.age
		assertEquals "bart", beanWithMap.peopleByName.bart.person
	}

	void testAnonymousInnerBeanViaBeanMethod() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def marge = appCtx.getBean("marge")
		assertEquals "homer", marge.bean1.person
	}

	void testAnonymousInnerBeanViaBeanMethodWithConstructorArgs() {
		def appCtx = new GenericApplicationContext()
		def reader = new GroovyBeanDefinitionReader(appCtx)
		reader.beans {
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
		appCtx.refresh()

		def marge = appCtx.getBean("marge")

		assertEquals "homer", marge.bean3.person
		assertEquals "lisa", marge.bean3.bean1.person
	}

}

class HolyGrailQuest {
	void start() { println "lets begin" }
}
class KnightOfTheRoundTable {
String name
String leader
KnightOfTheRoundTable(String n) {
	  this.name = n
}
HolyGrailQuest quest

void embarkOnQuest() {
	   quest.start()
}
}

// simple bean
class Bean1 {
	String person
	int age
	Properties props
	List children
}
// bean referencing other bean
class Bean2 {
	int age
	String person
	Bean1 bean1
	Bean3 bean3
	Properties props
	List children
	Bean1 parent
}
// bean with constructor args
class Bean3 {
	Bean3(String person, Bean1 bean1) {
		this.person = person
		this.bean1 = bean1
	}
	String person
	Bean1 bean1
	int age
}
// bean with factory method
class Bean4 {
	private Bean4() {}
	static Bean4 getInstance() {
		return new Bean4()
	}
	String person
}
// bean with List-valued constructor arg
class Bean5 {
	Bean5(List<Bean1> people) {
		this.people = people
	}
	List<Bean1> people
}
// bean with Map-valued constructor arg
class Bean6 {
	Bean6(Map<String, Bean1> peopleByName) {
		this.peopleByName = peopleByName
	}
	Map<String, Bean1> peopleByName
}
// a factory bean
class Bean1Factory {
	Bean1 newInstance() {
		return new Bean1()
	}
}
class ScopeTest {}
class TestScope implements Scope {

	int instanceCount


	public Object remove(String name) {
		 // do nothing
	}

	public void registerDestructionCallback(String name, Runnable callback) {
	}

	public String getConversationId() {
		return "mock"
	}

	public Object get(String name, ObjectFactory<?> objectFactory) {
		instanceCount++
		objectFactory.getObject()

	}

	public Object resolveContextualObject(String s) {
		return null;  // noop
	}
}
class BirthdayCardSender {
List peopleSentCards = []
public void onBirthday(AdvisedPerson person) {
	  peopleSentCards << person
}
}
@Component(value = "person")
public class AdvisedPerson {
int age;
String name;

public void birthday() {
	  ++age;
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
		new Bean1(person:name)
	}
}
