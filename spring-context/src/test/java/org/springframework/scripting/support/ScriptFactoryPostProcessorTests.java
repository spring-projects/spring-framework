/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.scripting.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.groovy.GroovyScriptFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
@EnabledForTestGroups(LONG_RUNNING)
class ScriptFactoryPostProcessorTests {

	private static final String MESSAGE_TEXT = "Bingo";

	private static final String MESSENGER_BEAN_NAME = "messenger";

	private static final String PROCESSOR_BEAN_NAME = "processor";

	// quote the returned message (this is the change)...
	private static final String CHANGED_SCRIPT = """
			package org.springframework.scripting.groovy
			import org.springframework.scripting.Messenger
			class GroovyMessenger implements Messenger {
				private String message = "Bingo"
				public String getMessage() {
					return "'"  + this.message + "'"
				}
				public void setMessage(String message) {
					this.message = message
				}
			}""";

	private static final String EXPECTED_CHANGED_MESSAGE_TEXT = "'" + MESSAGE_TEXT + "'";

	private static final int DEFAULT_SECONDS_TO_PAUSE = 1;

	private static final String DELEGATING_SCRIPT = """
			inline:package org.springframework.scripting;
			class DelegatingMessenger implements Messenger {
				private Messenger wrappedMessenger;
				public String getMessage() {
					return this.wrappedMessenger.getMessage()
				}
				public void setMessenger(Messenger wrappedMessenger) {
					this.wrappedMessenger = wrappedMessenger
				}
			}""";


	@Test
	void testDoesNothingWhenPostProcessingNonScriptFactoryTypeBeforeInstantiation() {
		assertThat(new ScriptFactoryPostProcessor().postProcessBeforeInstantiation(getClass(), "a.bean")).isNull();
	}

	@Test
	void testThrowsExceptionIfGivenNonAbstractBeanFactoryImplementation() {
		assertThatIllegalStateException().isThrownBy(() ->
				new ScriptFactoryPostProcessor().setBeanFactory(mock()));
	}

	@Test
	void testChangeScriptWithRefreshableBeanFunctionality() {
		BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(true);
		BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
		ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
		ctx.refresh();

		Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		source.setScript(CHANGED_SCRIPT);
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		// the updated script surrounds the message in quotes before returning...
		assertThat(refreshedMessenger.getMessage()).isEqualTo(EXPECTED_CHANGED_MESSAGE_TEXT);
	}

	@Test
	void testChangeScriptWithNoRefreshableBeanFunctionality() {
		BeanDefinition processorBeanDefinition = createScriptFactoryPostProcessor(false);
		BeanDefinition scriptedBeanDefinition = createScriptedGroovyBean();

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition(PROCESSOR_BEAN_NAME, processorBeanDefinition);
		ctx.registerBeanDefinition(MESSENGER_BEAN_NAME, scriptedBeanDefinition);
		ctx.refresh();

		Messenger messenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		source.setScript(CHANGED_SCRIPT);
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertThat(refreshedMessenger.getMessage()).as("Script seems to have been refreshed (must not be as no refreshCheckDelay set on ScriptFactoryPostProcessor)").isEqualTo(MESSAGE_TEXT);
	}

	@Test
	void testRefreshedScriptReferencePropagatesToCollaborators() {
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
		assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		source.setScript(CHANGED_SCRIPT);
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		// the updated script surrounds the message in quotes before returning...
		assertThat(refreshedMessenger.getMessage()).isEqualTo(EXPECTED_CHANGED_MESSAGE_TEXT);
		// ok, is this change reflected in the reference that the collaborator has?
		DefaultMessengerService collaborator = (DefaultMessengerService) ctx.getBean(collaboratorBeanName);
		assertThat(collaborator.getMessage()).isEqualTo(EXPECTED_CHANGED_MESSAGE_TEXT);
	}

	@Test
	@SuppressWarnings("resource")
	void testReferencesAcrossAContainerHierarchy() {
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

	@Test
	@SuppressWarnings("resource")
	void testScriptHavingAReferenceToAnotherBean() {
		// just tests that the (singleton) script-backed bean is able to be instantiated with references to its collaborators
		new ClassPathXmlApplicationContext("org/springframework/scripting/support/groovyReferences.xml");
	}

	@Test
	void testForRefreshedScriptHavingErrorPickedUpOnFirstCall() {
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
		assertThat(messenger.getMessage()).isEqualTo(MESSAGE_TEXT);
		// cool; now let's change the script and check the refresh behaviour...
		pauseToLetRefreshDelayKickIn(DEFAULT_SECONDS_TO_PAUSE);
		StaticScriptSource source = getScriptSource(ctx);
		// needs The Sundays compiler; must NOT throw any exception here...
		source.setScript("I keep hoping you are the same as me, and I'll send you letters and come to your house for tea");
		Messenger refreshedMessenger = (Messenger) ctx.getBean(MESSENGER_BEAN_NAME);
		assertThatExceptionOfType(FatalBeanException.class).isThrownBy(refreshedMessenger::getMessage)
			.matches(ex -> ex.contains(ScriptCompilationException.class));
	}

	@Test
	@SuppressWarnings("resource")
	void testPrototypeScriptedBean() {
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
		assertThat(messenger2).isNotSameAs(messenger1);
	}

	private static StaticScriptSource getScriptSource(GenericApplicationContext ctx) {
		ScriptFactoryPostProcessor processor = (ScriptFactoryPostProcessor) ctx.getBean(PROCESSOR_BEAN_NAME);
		BeanDefinition bd = processor.scriptBeanFactory.getBeanDefinition("scriptedObject.messenger");
		return (StaticScriptSource) bd.getConstructorArgumentValues().getIndexedArgumentValue(0, StaticScriptSource.class).getValue();
	}

	private static BeanDefinition createScriptFactoryPostProcessor(boolean isRefreshable) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ScriptFactoryPostProcessor.class);
		if (isRefreshable) {
			builder.addPropertyValue("defaultRefreshCheckDelay", 1L);
		}
		return builder.getBeanDefinition();
	}

	private static BeanDefinition createScriptedGroovyBean() {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(GroovyScriptFactory.class);
		builder.addConstructorArgValue("""
				inline:package org.springframework.scripting;
				class GroovyMessenger implements Messenger {
					private String message = "Bingo"
					public String getMessage() {
						return this.message
					}
					public void setMessage(String message) {
						this.message = message
					}
				}""");
		builder.addPropertyValue("message", MESSAGE_TEXT);
		return builder.getBeanDefinition();
	}

	private static void pauseToLetRefreshDelayKickIn(int secondsToPause) {
		try {
			Thread.sleep(secondsToPause * 1000L);
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
