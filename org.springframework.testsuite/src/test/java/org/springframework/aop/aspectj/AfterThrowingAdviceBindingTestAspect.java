/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.aop.aspectj;

/**
 * Aspect used as part of before advice binding tests.
 * @author Adrian Colyer
 */
public class AfterThrowingAdviceBindingTestAspect {

	// collaborator interface that makes it easy to test this aspect is 
	// working as expected through mocking.
	public interface AfterThrowingAdviceBindingCollaborator {
		void noArgs();
		void oneThrowable(Throwable t);
		void oneRuntimeException(RuntimeException re);
		void noArgsOnThrowableMatch();
		void noArgsOnRuntimeExceptionMatch();
	}
	
	protected AfterThrowingAdviceBindingCollaborator collaborator = null;
	
	public void setCollaborator(AfterThrowingAdviceBindingCollaborator aCollaborator) {
		this.collaborator = aCollaborator;
	}
	
	public void noArgs() {
		this.collaborator.noArgs();
	}
	
	public void oneThrowable(Throwable t) {
		this.collaborator.oneThrowable(t);
	}
	
	public void oneRuntimeException(RuntimeException ex) {
		this.collaborator.oneRuntimeException(ex);
	}
	
	public void noArgsOnThrowableMatch() {
		this.collaborator.noArgsOnThrowableMatch();
	}
	
	public void noArgsOnRuntimeExceptionMatch() {
		this.collaborator.noArgsOnRuntimeExceptionMatch();
	}

}
