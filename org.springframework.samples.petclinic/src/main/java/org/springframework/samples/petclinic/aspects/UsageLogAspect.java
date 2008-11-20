package org.springframework.samples.petclinic.aspects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Sample AspectJ annotation-style aspect that saves
 * every owner name requested to the clinic.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@Aspect
public class UsageLogAspect {

	private int historySize = 100;

	// Of course saving all names is not suitable for
	// production use, but this is a simple example.
	private List<String> namesRequested = new ArrayList<String>(this.historySize);


	public synchronized void setHistorySize(int historySize) {
		this.historySize = historySize;
		this.namesRequested = new ArrayList<String>(historySize);
	}

	@Before("execution(* *.findOwners(String)) && args(name)")
	public synchronized void logNameRequest(String name) {
		// Not the most efficient implementation,
		// but we're aiming to illustrate the power of
		// @AspectJ AOP, not write perfect code here :-)
		if (this.namesRequested.size() > this.historySize) {
			this.namesRequested.remove(0);
		}
		this.namesRequested.add(name);
	}

	public synchronized List<String> getNamesRequested() {
		return Collections.unmodifiableList(this.namesRequested);
	}

}
