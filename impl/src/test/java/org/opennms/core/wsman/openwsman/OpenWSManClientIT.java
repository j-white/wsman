package org.opennms.core.wsman.openwsman;

import org.junit.Test;
import org.opennms.core.wsman.AbstractWSManClientIT;
import org.opennms.core.wsman.WSManClientFactory;

public class OpenWSManClientIT extends AbstractWSManClientIT {
    @Override
    public WSManClientFactory getFactory() {
        return new OpenWSManClientFactory();
    }

    @Test
    public void canEnumerateAndPullUsingWQLFilter() throws InterruptedException {
        // Pass. Not yet implemented
    }
}
