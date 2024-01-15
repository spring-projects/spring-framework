package org.bean;

public class RelationDemo {

	private BeanDemo beanDemo;
	private String relation;
	private String name;

	private EventDemo eventDemo;

	public BeanDemo getBeanDemo() {
		return beanDemo;
	}

	public void setBeanDemo(BeanDemo beanDemo) {
		this.beanDemo = beanDemo;
	}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EventDemo getEventDemo() {
		return eventDemo;
	}

	public void setEventDemo(EventDemo eventDemo) {
		this.eventDemo = eventDemo;
	}
}
