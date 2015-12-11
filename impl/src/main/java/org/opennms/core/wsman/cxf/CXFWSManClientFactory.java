package org.opennms.core.wsman.cxf;

import org.opennms.core.wsman.WSManClientFactory;
import org.opennms.core.wsman.WSManEndpoint;

public class CXFWSManClientFactory implements WSManClientFactory {

    @Override
    public CXFWSManClient getClient(WSManEndpoint endpoint) {
        return new CXFWSManClient(endpoint);
    }
}
