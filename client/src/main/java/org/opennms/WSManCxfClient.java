package org.opennms;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

public class WSManCxfClient {

    private String hostname;
    private int port;
    private String url;
    private String protocol;
    private String username;
    private String password;

    public WSManCxfClient(String hostname, int port, String url, String protocol, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.url = url;
        this.protocol = protocol;
        this.username = username;
        this.password = password;
    }
 
    public EnumerationOperations getEnumerator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        username,
                        password.toCharArray());
            }
        });

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(EnumerationOperations.class);
        factory.setAddress(String.format("%s://%s:%d%s", protocol, hostname, port, url));

        // Force the client to use SOAP v1.2, as per:
        // R13.1-1: A service shall at least receive and send SOAP 1.2 SOAP Envelopes.
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        EnumerationOperations enumerator = factory.create(EnumerationOperations.class);
        
        Client cxfClient = ClientProxy.getClient(enumerator);
        
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

        // Setup auth
        Map<String, Object> requestContext = cxfClient.getRequestContext();
        requestContext.put(BindingProvider.USERNAME_PROPERTY, username);
        requestContext.put(BindingProvider.PASSWORD_PROPERTY, password);

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

        // Namespace stuff, used to make it easier to compare the requests to those generated by OpenWSMan
        Map<String, String> nsMap = new HashMap<>();
        nsMap.put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing");
        nsMap.put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration");
        nsMap.put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd");
        cxfClient.getRequestContext().put("soap.env.ns.map", nsMap);

        // The iDrac card I'm working work doesn't support the "http://www.w3.org/2005/08/addressing"
        // namespace for WS-Addressing that CXF uses by default, so we need to change this to "http://schemas.xmlsoap.org/ws/2004/08/addressing"
        Map<String, String> outTransformMap = Collections.singletonMap("{http://www.w3.org/2005/08/addressing}*", "{http://schemas.xmlsoap.org/ws/2004/08/addressing}*");
        org.apache.cxf.interceptor.transform.TransformOutInterceptor transformOutInterceptor =
            new org.apache.cxf.interceptor.transform.TransformOutInterceptor();
        transformOutInterceptor.setOutTransformElements(outTransformMap);
        cxfClient.getOutInterceptors().add(transformOutInterceptor);

        return enumerator;
    }
}
