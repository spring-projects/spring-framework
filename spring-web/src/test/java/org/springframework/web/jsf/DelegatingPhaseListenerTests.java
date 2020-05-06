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

package org.springframework.web.jsf;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 */
public class DelegatingPhaseListenerTests {

	private final MockFacesContext facesContext = new MockFacesContext();

	private final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

	@SuppressWarnings("serial")
	private final DelegatingPhaseListenerMulticaster delPhaseListener = new DelegatingPhaseListenerMulticaster() {
		@Override
		protected ListableBeanFactory getBeanFactory(FacesContext facesContext) {
			return beanFactory;
		}
	};

	@Test
	public void beforeAndAfterPhaseWithSingleTarget() {
		TestListener target = new TestListener();
		beanFactory.addBean("testListener", target);

		assertThat((Object) delPhaseListener.getPhaseId()).isEqualTo(PhaseId.ANY_PHASE);
		PhaseEvent event = new PhaseEvent(facesContext, PhaseId.INVOKE_APPLICATION, new MockLifecycle());

		delPhaseListener.beforePhase(event);
		assertThat(target.beforeCalled).isTrue();

		delPhaseListener.afterPhase(event);
		assertThat(target.afterCalled).isTrue();
	}

	@Test
	public void beforeAndAfterPhaseWithMultipleTargets() {
		TestListener target1 = new TestListener();
		TestListener target2 = new TestListener();
		beanFactory.addBean("testListener1", target1);
		beanFactory.addBean("testListener2", target2);

		assertThat((Object) delPhaseListener.getPhaseId()).isEqualTo(PhaseId.ANY_PHASE);
		PhaseEvent event = new PhaseEvent(facesContext, PhaseId.INVOKE_APPLICATION, new MockLifecycle());

		delPhaseListener.beforePhase(event);
		assertThat(target1.beforeCalled).isTrue();
		assertThat(target2.beforeCalled).isTrue();

		delPhaseListener.afterPhase(event);
		assertThat(target1.afterCalled).isTrue();
		assertThat(target2.afterCalled).isTrue();
	}


	@SuppressWarnings("serial")
	public static class TestListener implements PhaseListener {

		boolean beforeCalled = false;
		boolean afterCalled = false;

		@Override
		public PhaseId getPhaseId() {
			return PhaseId.ANY_PHASE;
		}

		@Override
		public void beforePhase(PhaseEvent arg0) {
			beforeCalled = true;
		}

		@Override
		public void afterPhase(PhaseEvent arg0) {
			afterCalled = true;
		}
	}

}
