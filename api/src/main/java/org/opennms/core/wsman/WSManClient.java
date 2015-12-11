package org.opennms.core.wsman;

import java.util.List;

import org.w3c.dom.Node;

public interface WSManClient {

    /**
     * Starts a new enumeration session.
     *
     * @param wql 
     * @return context id
     */
    public String enumerate(String wql);

    /**
     * Pulls the objects from an existing enumeration session.
     *
     * @param contextId enumeration context id
     * @return
     */
    public List<Node> pull(String contextId);
}
