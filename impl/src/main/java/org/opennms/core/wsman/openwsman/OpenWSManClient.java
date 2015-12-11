package org.opennms.core.wsman.openwsman;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.opennms.core.wsman.WSManClient;
import org.opennms.core.wsman.WSManEndpoint;
import org.opennms.core.wsman.WSManException;
import org.openwsman.Client;
import org.openwsman.ClientOptions;
import org.openwsman.Filter;
import org.openwsman.XmlDoc;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

import org.openwsman.OpenWSManConstants;

public class OpenWSManClient implements WSManClient {

    private final WSManEndpoint m_endpoint;

    public OpenWSManClient(WSManEndpoint endpoint) {
        m_endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
    }

    private Client getClient() {
        URL url = m_endpoint.getUrl();
        
        Client client = new Client(url.getHost(), url.getPort(), url.getPath(), url.getProtocol(), m_endpoint.getUsername(), m_endpoint.getPassword());
        if (m_endpoint.isBasicAuth()) {
            client.transport().set_auth_method(OpenWSManConstants.BASIC_AUTH_STR);
        }

        if (m_endpoint.isStrictSSL()) {
            // Disable SSL cert check
            client.transport().set_verify_host(0);
            client.transport().set_verify_peer(0);
        }

        return client;
    }

    private ClientOptions getClientOptions() {
        ClientOptions options = new ClientOptions();
        //options.set_dump_request();
        return options;
    }

    @Override
    public String enumerateWithWQLFilter(String wql, String resourceUri) {
        final Client client = getClient();
        final ClientOptions options = getClientOptions();

        Filter filter = new Filter();
        filter.wql(wql);

        XmlDoc result = client.enumerate(options, filter, resourceUri);
        if ((result == null) || result.isFault()) {
            throw new WSManException("Enumeration failed: " + ((result != null) ? result.fault().reason() : "?"));
        } else {
            return result.context();
        }
    }

    @Override
    public List<Node> pull(String contextId, String resourceUri) {
        final Client client = getClient();
        final ClientOptions options = getClientOptions();

        XmlDoc result = client.pull(options, null, resourceUri, contextId);
        if ((result == null) || result.isFault()) {
            throw new WSManException("Pull failed: " + ((result != null) ? result.fault().reason() : "?"));
        } else {
            // We need to return a list of Nodes from the SOAP message body
            // OpenWSMan gives us the whole SOAP response in their own XML wrappers
            // so we re-encode it, and parse it out again
            String xml = result.encode("UTF-8");
            try {
                // Parse th SOAP response
                MessageFactory factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                SOAPMessage message;
                message = factory.createMessage(
                        new MimeHeaders(),
                        new ByteArrayInputStream(xml.getBytes(Charset
                                .forName("UTF-8"))));
                
                // Now grab the items from the body
                SOAPBody body = message.getSOAPBody();
                NodeList returnList = body.getElementsByTagName("wsen:Items");

                // Re-encode the items so they are "disconnected" from the DOM tree
                List<Node> nodes = Lists.newLinkedList();
                for (int i = 0; i < returnList.getLength(); i++) {
                    String innerXml = innerXml(returnList.item(0)).trim();
                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                            .parse(new InputSource(new StringReader(innerXml)));
                    nodes.add(doc);
                }
                return nodes;
            } catch (IOException | SOAPException | SAXException | ParserConfigurationException e) {
                throw new WSManException("Failed to parse OpenWSMan's XML output.", e);
            }
        }
    }

    public static String innerXml(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS)node.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
           sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        return sb.toString(); 
    }
}
