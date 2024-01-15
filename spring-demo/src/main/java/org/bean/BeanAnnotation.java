package org.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanAnnotation {
	@Bean
	public BeanDemo beanDemo(){
		BeanDemo beanDemo = new BeanDemo();
		beanDemo.setName("bean-annotation-demo");
		beanDemo.setValue("bean-annotation");
		return beanDemo;
	}

	@Bean
	public EventDemo eventDemo(){
		EventDemo eventDemo = new EventDemo();
		eventDemo.setEvent("do-spring-bean-annotation-analysis-event");
		eventDemo.setName("do-spring-bean-annotation");
		return eventDemo;
	}

	@Bean
	public RelationDemo relationDemo(BeanDemo beanDemo, EventDemo eventDemo){
		RelationDemo relationDemo = new RelationDemo();
		relationDemo.setRelation("bean-do-event-relation");
		relationDemo.setName("bean-annotation");
		relationDemo.setBeanDemo(beanDemo);
		relationDemo.setEventDemo(eventDemo);
		return relationDemo;
	}
}
