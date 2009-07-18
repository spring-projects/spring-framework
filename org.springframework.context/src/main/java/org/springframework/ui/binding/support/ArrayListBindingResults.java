/**
 * 
 */
package org.springframework.ui.binding.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;

class ArrayListBindingResults implements BindingResults {

	private List<BindingResult> results;

	public ArrayListBindingResults() {
		results = new ArrayList<BindingResult>();
	}

	public ArrayListBindingResults(int size) {
		results = new ArrayList<BindingResult>(size);
	}

	public void add(BindingResult result) {
		results.add(result);
	}

	// implementing Iterable

	public Iterator<BindingResult> iterator() {
		return results.iterator();
	}

	// implementing BindingResults

	public BindingResults successes() {
		ArrayListBindingResults results = new ArrayListBindingResults();
		for (BindingResult result : this) {
			if (!result.isFailure()) {
				results.add(result);
			}
		}
		return results;
	}

	public BindingResults failures() {
		ArrayListBindingResults results = new ArrayListBindingResults();
		for (BindingResult result : this) {
			if (result.isFailure()) {
				results.add(result);
			}
		}
		return results;
	}

	public BindingResult get(int index) {
		return results.get(index);
	}

	public List<String> properties() {
		List<String> properties = new ArrayList<String>(results.size());
		for (BindingResult result : this) {
			properties.add(result.getProperty());
		}
		return properties;
	}

	public int size() {
		return results.size();
	}

	public String toString() {
		return "[BindingResults = " + results.toString() + "]";
	}
}