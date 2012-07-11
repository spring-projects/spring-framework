package org.springframework.transaction.interceptor;

import org.springframework.transaction.annotation.Transactional;

@Transactional(value="theTransactionManager")
public class SubClass extends BaseClass {

}
