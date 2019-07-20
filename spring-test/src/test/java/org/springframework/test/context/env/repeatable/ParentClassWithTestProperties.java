package org.springframework.test.context.env.repeatable;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "inherited = 12345")
public class ParentClassWithTestProperties {
}
