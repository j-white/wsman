package org.opennms.core.wsman.cxf;

import org.opennms.core.wsman.AbstractWSManClientDeviceIT;
import org.opennms.core.wsman.WSManClientFactory;

public class CXFWSManClientDeviceIT extends AbstractWSManClientDeviceIT {
    @Override
    public WSManClientFactory getFactory() {
        return new CXFWSManClientFactory();
    }
}
