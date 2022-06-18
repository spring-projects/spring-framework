package org.springframework.beans.factory.config;

/**
 * Bean life cycle Notice
 *
 * @author Zen Huifer
 */
public interface BeanLifecycleNotice {


	void notice(BeanLifecycleEnum lifecycleEnum, BeanLifecycleEvent event);

	/**
	 * Bean life cycle type
	 */
	enum BeanLifecycleEnum {
		created,
		destroyed,
	}

	/**
	 * Bean life cycle event
	 */
	class BeanLifecycleEvent {
		/**
		 * event name
		 */
		private String beanName;


		/**
		 * bean instance
		 */
		private Object bean;



		public BeanLifecycleEvent(String beanName, Object bean) {
			this.beanName = beanName;
			this.bean = bean;
		}

		public String getBeanName() {
			return beanName;
		}

		public void setBeanName(String beanName) {
			this.beanName = beanName;
		}

		public Object getBean() {
			return bean;
		}

		public void setBean(Object bean) {
			this.bean = bean;
		}
	}

}
