package spring.lh.annotation.aop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestMain {
	private static final Log log = LogFactory.getLog(TestMain.class);

	public static void main(String[] args) {
		log.info(IsOfImpl.class.isInterface());
	}
}
