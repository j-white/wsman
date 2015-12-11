package org.opennms.core.wsman;

public interface WSManClientFactory {
    public WSManClient getClient(WSManEndpoint endpoint);
}
