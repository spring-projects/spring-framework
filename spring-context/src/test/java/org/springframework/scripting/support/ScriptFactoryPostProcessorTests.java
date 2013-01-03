/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scripting.support;

import static org.mockito.Mockito.mock;
import junit.framework.TestCase;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.groovy.GroovyScriptFactory;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ScriptFactoryPostProcessorTests extends TestCase {

	private static final String MESSAGE_TEXT = "Bingo";

	private static final String MESSENGER_BEAN_NAME = "messenger";

	private static final String PROCESSOR_BEAN_NAME = "processor";

	private static final String CHANGED_SCRIPT = "package org.springframework.scripting.groovy\n" +
			"import org.springframework.scripting.Messenger\n" +
			"class GroovyMessenger implements Messenger {\n" +
			"  private String message = \"Bingo\"\n" +
			"  public String getMessage() {\n" +
			// quote the returned message (this is the change)...
			"    return \"'\"  + this.message + \"'\"\n" +
			"  }\n" +
			"  public void setMessage(String message) {\n" +
			"    this.message = message\n" +
			"  }\n" +
			"}";

	private static final String EXPECTED_CHANGED_MESSAGE_TEXT = "'" + MESSAGE_TEXT + "'";

	private static final int DEFAULT_SECONDS_TO_PAUSE = 1;

	private static final String DELEGATING_SCRIPT = "inline:package org.springframework.scripting;\n" +
			"class DelegatingMessenger implements Messenger {\n" +
			"  private Messenger wrappedMessenger;\n" +
			"  public String getMessage() {\n" +
			"    return this.wrappedMessenger.getMessage()\n" +
			"  }\n" +
			"  public void setMessenger(Messenger wrappedMessenger) {\n" +
			"    this.wrappedMessenger = wrappedMessenger\n" +
			"  }\n" +
			"}";


	public void testDoesNothingWhenPostProcessingNonScriptFactoryTypeBeforeInstantiation() throws Exception {
		assertNull(new ScriptFactoryPostProcessor().postProcessBeforeInstantiation(getClass(), "a.bean"));
	}

	public void testThrowsExceptionIfGivenNonAbstractBeanFactoryImplementation() throws Exception {
		try {
			new ScriptFactoryPostProcessor().setBeanFactory(mock(BeanFactory.class));
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalStateException expected) {
		}
	}

	public void testChangeScriptWithRefreshableBeanFunctionality() throws Exception {
		BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
		BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
		ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
		ctx.refresh();

		Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertEquals(MESSAGE_TEXT, messenger.getMessage());
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		source.setScript(CHANGED_SCRIPT);
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		// the updated script surrounds the message in quotes before returning...
		assertEquals(EXPECTED_CHANGED_MESSAGE_TEXT, refreshedMessenger.getMessage());
	}

	public void testChangeScriptWithNoRefreshableBeanFunctionality() throws Exception {
		BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(false);
		BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
		ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
		ctx.refresh();

		Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertEquals(MESSAGE_TEXT, messenger.getMessage());
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		source.setScript(CHANGED_SCRIPT);
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertEquals("Script seems to have been refreshed (must not be as no refreshCheckDelay set on ScriptFactoryPostProcessor)",
				MESSAGE_TEXT, refreshedMessenger.getMessage());
	}

	public void testRefreshedScriptReferencePropagatesToCollaborators() throws Exception {
		BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
		BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();
		BeanDefinitionBuilder collaboratorBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultMessengerService.class);
		collaboratorBuilder.addPropertyReference(MESSENGER_BEAN_NAME, MESSENGER_BEAN_NAME);

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
		ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
		final String collaboratorBeanName = "collaborator";
		ctx.registerBeanDefinition(collaboratorBeanName, collaboratorBuilder.getBeanDefinition());
		ctx.refresh();

		Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertEquals(MESSAGE_TEXT, messenger.getMessage());
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		source.setScript(CHANGED_SCRIPT);
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		// the updated script surrounds the message in quotes before returning...
		assertEquals(EXPECTED_CHANGED_MESSAGE_TEXT, refreshedMessenger.getMessage());
		// ok, is this change reflected in the reference that the collaborator has?
		DefaultMessengerService collaborator = (DefaultMessengerService) ctx.getBean(collaboratorBeanName);
		assertEquals(EXPECTED_CHANGED_MESSAGE_TEXT, collaborator.getMessage());
	}

	public void testReferencesAcrossAContainerHierarchy() throws Exception {
		GenericApplicationContext businessContext = new GenericApplicationContext();
		businessContext.registerBeanDefinition("messenger", BeanDefinitionBuilder.rootBeanDefinition(StubMessenger.class).getBeanDefinition());
		businessContext.refresh();

		BeanDefinitionBuilder scriptedBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
		scriptedBeanBuilder.addConstructorArgValue(DELEGATING_SCRIPT);
		scriptedBeanBuilder.addPropertyReference("messenger", "messenger");

		GenericApplicationContext presentationCtx = new GenericApplicationContext(businessContext);
		presentationCtx.registerBeanDefinition("needsMessenger", scriptedBeanBuilder.getBeanDefinition());
		presentationCtx.registerBeanDefinition("scriptProcessor", createScriptFactoryPostProcessor(true));
		presentationCtx.refresh();
	}

	public void testScriptHavingAReferenceToAnotherBean() throws Exception {
		// just tests that the (singleton) script-backed bean is able to be instantiated with references to its collaborators
		new ClassPathXmlApplicationContext("org/springframework/scripting/support/groovyReferences.xml");
	}

	public void testForRefreshedScriptHavingErrorPickedUpOnFirstCall() throws Exception {
		BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
		BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();
		BeanDefinitionBuilder collaboratorBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultMessengerService.class);
		collaboratorBuilder.addPropertyReference(MESSENGER_BEAN_NAME, MESSENGER_BEAN_NAME);

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
		ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
		final String collaboratorBeanName = "collaborator";
		ctx.registerBeanDefinition(collaboratorBeanName, collaboratorBuilder.getBeanDefinition());
		ctx.refresh();

		Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertEquals(MESSAGE_TEXT, messenger.getMessage());
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		// needs The Sundays compiler; must NOT throw any exception here...
		source.setScript("I keep hoping you are the same as me, and I'll send you letters and come to your house for tea");
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		try {
			refreshedMessenger.getMessage();
			fail("Must have thrown an Exception (invalid script)");
		}
		catch (FatalBeanException expected) {
			assertTrue(expected.contains(ScriptCompilationException.class));
		}
	}

	public void testPrototypeScriptedBean() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("messenger", BeanDefinitionBuilder.rootBeanDefinition(StubMessenger.class).getBeanDefinition());

		BeanDefinitionBuilder scriptedBeanBuilder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
		scriptedBeanBuilder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		scriptedBeanBuilder.addConstructorArgValue(DELEGATING_SCRIPT);
		scriptedBeanBuilder.addPropertyReference("messenger", "messenger");

		final String BEAN_WITH_DEPENDENCY_NAME = "needsMessenger";
		ctx.registerBeanDefinition(BEAN_WITH_DEPENDENCY_NAME, scriptedBeanBuilder.getBeanDefinition());
		ctx.registerBeanDefinition("scriptProcessor", createScriptFactoryPostProcessor(true));
		ctx.refresh();

		Messenger messenger1 = (Messenger) ctx.getBean(BEAN_WITH_DEPENDENCY_NAME);
		Messenger messenger2 = (Messenger) ctx.getBean(BEAN_WITH_DEPENDENCY_NAME);
		assertNotSame(messenger1, messenger2);
	}

	private static StaticScriptSource getScriptSource(GenericApplicationContext ctx) throws Exception {
		ScriptFactoryPostProcessor processor = (ScriptFactoryPostProcessor) ctx.getBean(PROCESSOR_BEAN_NAME);
		BeanDefinition bd = processor.scriptBeanFactory.getBeanDefinition("scriptedObject.messenger");
		return (StaticScriptSource) bd.getConstructorArgumentValues().getIndexedArgumentValue(0, StaticScriptSource.class).getValue();
	}

	private static BeanDefinition createScriptFactoryPostProcessor(boolean isRefreshable) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ScriptFactoryPostProcessor.class);
		if (isRefreshable) {
			builder.addPropertyValue("defaultRefreshCheckDelay", new Long(1));
		}
		return builder.getBeanDefinition();
	}

	private static BeanDefinition createScriptedGroovyBean() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
		builder.addConstructorArgValue("inline:package org.springframework.scripting;\n" +
				"class GroovyMessenger implements Messenger {\n" +
				"  private String message = \"Bingo\"\n" +
				"  public String getMessage() {\n" +
				"    return this.message\n" +
				"  }\n" +
				"  public void setMessage(String message) {\n" +
				"    this.message = message\n" +
				"  }\n" +
				"}");
		builder.addPropertyValue("message", MESSAGE_TEXT);
		return builder.getBeanDefinition();
	}

	private static void pauseToLetRefreshDelayKickIn(int secondsToPause) {
		try {
			Thread.sleep(secondsToPause * 1000);
		}
		catch (InterruptedException ignored) {
		}
	}


	public static class DefaultMessengerService {

		private Messenger messenger;

		public void setMessenger(Messenger messenger) {
			this.messenger = messenger;
		}

		public String getMessage() {
			return this.messenger.getMessage();
		}
	}

}
