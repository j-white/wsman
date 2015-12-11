package org.opennms.core.wsman;

import java.util.List;

import org.w3c.dom.Node;

public interface WSManClient {

    /**
     * Starts a new enumeration session using a WQL query
     * as a filter
     *
     * @param wql 
     * @param resourceUri
     * @return context id
     */
    public String enumerateWithWQLFilter(String wql, String resourceUri);

    /**
     * Pulls the objects from an existing enumeration session.
     *
     * @param contextId enumeration context id
     * @param resourceUri
     * @return
     */
    public List<Node> pull(String contextId, String resourceUri);
}
