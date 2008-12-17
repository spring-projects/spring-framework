import org.springframework.beans.factory.FactoryBean

class TestFactoryBean implements FactoryBean {

	public boolean isSingleton() {
		true
	}

	public Class getObjectType() {
		String.class
	}

	public Object getObject() {
		"test"
	}

}
