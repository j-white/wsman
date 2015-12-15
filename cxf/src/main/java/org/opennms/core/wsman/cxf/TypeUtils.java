package org.opennms.core.wsman.cxf;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.opennms.core.wsman.WSManException;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

import schemas.dmtf.org.wbem.wsman.v1.AnyListType;

public class TypeUtils {

    protected static String getContextIdFrom(EnumerateResponse response) {
        // A valid response must always include the EnumerationContext element
        return getContextIdFrom(response.getEnumerationContext());
    }

    protected static String getContextIdFrom(PullResponse response) {
        if (response.getEnumerationContext() == null) {
            // The PullResponse will not contain an EnumerationContext if EndOfSequence is set
            return null;
        }
        return getContextIdFrom(response.getEnumerationContext());
    }

    protected static String getContextIdFrom(EnumerationContextType context) {
        // The content of the EnumerationContext should contain a single string, the context id
        if (context.getContent() == null) {
            throw new WSManException(String.format("EnumerationContext %s has no content.", context));
        }

        if (context.getContent().size() == 0) {
            // The EnumerationContext can be empty if we issue an optimized enumeration
            // and all of the records are immediately returned
            return null;
        } else if (context.getContent().size() == 1) {
            Object content = context.getContent().get(0);
            if (content instanceof String) {
                return (String)content;
            } else {
                throw new WSManException(String.format("Unsupported EnumerationContext content: %s", content));
            }
        } else {
            throw new WSManException(String.format("EnumerationContext contains too many elements, expected: 1 actual: %d",
                    context.getContent().size()));
        }
    }

    protected static boolean getItemsFrom(EnumerateResponse response, List<Node> items) {
        boolean endOfSequence = false;
        for (Object object : response.getAny()) {
            if (object instanceof JAXBElement) {
                JAXBElement<?> el = (JAXBElement<?>)object;
                if ("Items".equals(el.getName().getLocalPart())) {
                    Object value = el.getValue();
                    if (value instanceof AnyListType) {
                        AnyListType itemList = (AnyListType)value;
                        items.addAll(itemList.getAny().stream()
                                .filter(o -> o instanceof org.w3c.dom.Node)
                                .map(o -> (Node)o)
                                .collect(Collectors.toList()));
                    }
                } else if ("EndOfSequence".equals(el.getName().getLocalPart())) {
                    endOfSequence = true;
                } else {
                    throw new WSManException(String.format("Unsupported element in EnumerateResponse: %s", object));
                }
            } else if (object instanceof Node) {
                Node node = (Node)object;
                if ("EndOfSequence".equals(node.getLocalName())) {
                    endOfSequence = true;
                } else {
                    throw new WSManException(String.format("Unsupported node in EnumerateResponse: %s", node));
                }
            } else {
                throw new WSManException(String.format("Unsupported element in EnumerateResponse: %s, with type: %s",
                        object, object != null ? object.getClass() : null));
            }
        }
        return endOfSequence;
    }

    protected static String getElementTypeFromResourceUri(String resourceUri) {
        String elementType = null;
        try {
            URI uri = new URI(resourceUri);
            String path = uri.getPath();
            elementType = path.substring(path.lastIndexOf('/') + 1);
        } catch (Throwable t) {
            throw new WSManException("t", t);
        }
        return elementType;
    }
}
