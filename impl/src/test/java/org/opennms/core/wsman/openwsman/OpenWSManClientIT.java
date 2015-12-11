package org.opennms.core.wsman.openwsman;

import org.opennms.core.wsman.AbstractWSManClientIT;
import org.opennms.core.wsman.WSManClientFactory;

public class OpenWSManClientIT extends AbstractWSManClientIT {
    @Override
    public WSManClientFactory getFactory() {
        return new OpenWSManClientFactory();
    }
}
