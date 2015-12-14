package org.opennms.core.wsman;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

public interface WSManClient {

    /**
     * Retrieves a single element using the provided selectors.
     *
     * @param selectors
     * @param resourceUri
     * @return
     */
    Node get(Map<String, String> selectors, String resourceUri);

    /**
     * Starts a new enumeration session with a filter.
     *
     * @param dialect
     * @param filter
     * @param resourceUri
     * @return context id
     */
    public String enumerateWithFilter(String dialect, String filter, String resourceUri);

    /**
     * Pulls the objects from an existing enumeration session.
     *
     * @param contextId
     * @param resourceUri
     * @return
     */
    public List<Node> pull(String contextId, String resourceUri);

    /**
     * Optimized version of the enumerate and pull operations.
     *
     * The implementation should attempt to consolidate the calls, using optimized enumeration.
     *
     * @param dialect
     * @param filter
     * @param resourceUri
     * @return
     */
    public List<Node> enumerateAndPullUsingFilter(String dialect, String filter, String resourceUri);
}
