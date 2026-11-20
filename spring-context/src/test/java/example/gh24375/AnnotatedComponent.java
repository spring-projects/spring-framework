/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.gh24375;

import org.springframework.stereotype.Component;

@Component
@EnclosingAnnotation(nested2 = @NestedAnnotation)
public class AnnotatedComponent {
}
