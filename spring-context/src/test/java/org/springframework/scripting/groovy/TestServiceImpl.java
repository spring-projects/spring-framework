package org.springframework.scripting.groovy;

@Log
public class TestServiceImpl implements TestService{
    @Override
    public String sayHello() {
        throw new TestException("TestServiceImpl");
    }
}
