package org.opennms.core.wsman.openwsman;

import org.opennms.core.wsman.WSManClientFactory;
import org.opennms.core.wsman.WSManEndpoint;

public class OpenWSManClientFactory implements WSManClientFactory {

    @Override
    public OpenWSManClient getClient(WSManEndpoint endpoint) {
        return new OpenWSManClient(endpoint);
    }
}
