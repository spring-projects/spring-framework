package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.startup.DefaultTilesInitializer;

/**
 * @author Torsten Krah
 */
class SpringTilesInitializer extends DefaultTilesInitializer {

    private TilesConfigurer tilesConfigurer;
    private final String contextId;

    public SpringTilesInitializer(TilesConfigurer tilesConfigurer, final String contextId) {
        super();
        this.tilesConfigurer = tilesConfigurer;
        this.contextId = contextId;
    }

    @Override
    protected AbstractTilesContainerFactory createContainerFactory(ApplicationContext context) {
        return new SpringTilesContainerFactory(this.tilesConfigurer);
    }

    @Override
    protected String getContainerKey(ApplicationContext applicationContext) {
        return this.contextId;
    }
}
