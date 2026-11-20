/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable_scoped;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

@Component
@MyScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CustomScopeAnnotationBean {
}
