package org.opennms.core.wsman.cxf;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.opennms.core.wsman.WSManClient;
import org.opennms.core.wsman.WSManEndpoint;
import org.opennms.core.wsman.WSManEndpoint.WSManVersion;
import org.opennms.core.wsman.WSManException;
import org.opennms.wsman.Enumerate;
import org.opennms.wsman.FilterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.ItemListType;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

public class CXFWSManClient implements WSManClient {
    private final static Logger LOG = LoggerFactory.getLogger(CXFWSManClient.class);

    private final WSManEndpoint m_endpoint;

    public CXFWSManClient(WSManEndpoint endpoint) {
        m_endpoint = Objects.requireNonNull(endpoint, "endpoint cannot be null");
    }

    public EnumerationOperations getEnumerator() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(EnumerationOperations.class);
        factory.setAddress(m_endpoint.getUrl().toExternalForm());

        // Force the client to use SOAP v1.2, as per:
        // R13.1-1: A service shall at least receive and send SOAP 1.2 SOAP Envelopes.
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        EnumerationOperations enumerator = factory.create(EnumerationOperations.class);
        
        Client cxfClient = ClientProxy.getClient(enumerator);
        Map<String, Object> requestContext = cxfClient.getRequestContext();

        // Add static name-space mappings to make visualizing the XML clearer
        Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing");
        nsMap.put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration");
        nsMap.put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd");
        cxfClient.getRequestContext().put("soap.env.ns.map", nsMap);

        if (!m_endpoint.isStrictSSL()) {
            LOG.debug("Disabling strict SSL checking.");
            // Accept all certs
            HTTPConduit http = (HTTPConduit) cxfClient.getConduit();
            TrustManager[] simpleTrustManager = new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };
            TLSClientParameters tlsParams = new TLSClientParameters();
            tlsParams.setTrustManagers(simpleTrustManager);
            tlsParams.setDisableCNCheck(true);
            http.setTlsClientParameters(tlsParams);
        }

        // Setup auth
        if (m_endpoint.isBasicAuth()) {
            LOG.debug("Enabling basic authentication.");
            //TODO: This authenticator shouldn't be global
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            m_endpoint.getUsername(),
                            m_endpoint.getPassword().toCharArray());
                }
            });
            requestContext.put(BindingProvider.USERNAME_PROPERTY, m_endpoint.getUsername());
            requestContext.put(BindingProvider.PASSWORD_PROPERTY, m_endpoint.getPassword());
        }

        // Set reply-to
        AddressingProperties maps = new AddressingProperties();
        EndpointReferenceType ref = new EndpointReferenceType();
        AttributedURIType add = new AttributedURIType();
        add.setValue("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous");
        ref.setAddress(add);
        maps.setReplyTo(ref);
        maps.setFaultTo(ref);
        requestContext.put("javax.xml.ws.addressing.context", maps);

        // Add WS-Man ResourceURI to the header
        AddResourecURIInterceptor interceptor = new AddResourecURIInterceptor("http://schemas.dmtf.org/wbem/wscim/1/*");
        cxfClient.getOutInterceptors().add(interceptor);

        if (m_endpoint.getServerVersion() == WSManVersion.WSMAN_1_0) {
            // WS-Man 1.0 does not support the W3C WS-Addressing, so we need to change the namespace
            // "http://www.w3.org/2005/08/addressing" becomes "http://schemas.xmlsoap.org/ws/2004/08/addressing"
            Map<String, String> outTransformMap = Collections.singletonMap("{http://www.w3.org/2005/08/addressing}*", "{http://schemas.xmlsoap.org/ws/2004/08/addressing}*");
            org.apache.cxf.interceptor.transform.TransformOutInterceptor transformOutInterceptor =
                new org.apache.cxf.interceptor.transform.TransformOutInterceptor();
            transformOutInterceptor.setOutTransformElements(outTransformMap);
            cxfClient.getOutInterceptors().add(transformOutInterceptor);
            
        }

        return enumerator;
    }

    public EnumerateResponse enumerate(Enumerate enumeration) {
        return getEnumerator().enumerate(enumeration);
    }

    @Override
    public String enumerate(String wql) {
        // Enumerate
        FilterType filter = new FilterType();
        filter.setDialect("http://schemas.microsoft.com/wbem/wsman/1/WQL");
        filter.getContent().add(wql);
        Enumerate msg = new Enumerate();
        msg.setFilter(filter);

        EnumerateResponse enumResponse = getEnumerator().enumerate(msg);
        if (enumResponse == null) {
            throw new WSManException("Enumeration failed. See logs for details.");
        }

        String contextId = (String)enumResponse.getEnumerationContext().getContent().get(0);
        return contextId;
    }

    @Override
    public List<Node> pull(String contextId) {
        EnumerationContextType enumContext = new EnumerationContextType();
        enumContext.getContent().add(contextId);
        Pull pull = new Pull();
        pull.setEnumerationContext(enumContext);

        PullResponse pullResponse = getEnumerator().pull(pull);
        if (pullResponse == null) {
            throw new WSManException(String.format("Pull failed for context id: %s. See logs for details.", contextId));
        }

        ItemListType itemList = pullResponse.getItems();
        return itemList.getAny().stream()
            .filter(o -> o instanceof org.w3c.dom.Node)
            .map(o -> (Node)o)
            .collect(Collectors.toList());
    }
}
