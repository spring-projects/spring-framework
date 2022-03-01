package springtest;

import org.springframework.stereotype.Component;

@Component
public class Frog implements Animal {

    @Override
    public void printName() {
        System.out.println("I am a frog  gua gua gua ");
    }
}
