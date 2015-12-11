package org.opennms.core.wsman.cxf;

import org.opennms.core.wsman.AbstractWSManClientIT;
import org.opennms.core.wsman.WSManClientFactory;

public class CXFWSManClientIT extends AbstractWSManClientIT {
    @Override
    public WSManClientFactory getFactory() {
        return new CXFWSManClientFactory();
    }
}
