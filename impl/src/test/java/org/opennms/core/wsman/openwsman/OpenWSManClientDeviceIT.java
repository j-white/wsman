package org.opennms.core.wsman.openwsman;

import org.opennms.core.wsman.AbstractWSManClientDeviceIT;
import org.opennms.core.wsman.WSManClientFactory;

public class OpenWSManClientDeviceIT extends AbstractWSManClientDeviceIT {
    @Override
    public WSManClientFactory getFactory() {
        return new OpenWSManClientFactory();
    }
}
