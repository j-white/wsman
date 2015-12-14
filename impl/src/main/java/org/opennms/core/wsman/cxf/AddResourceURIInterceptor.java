package org.opennms.core.wsman.cxf;

import java.util.List;
import java.util.Objects;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.phase.Phase;

import schemas.dmtf.org.wbem.wsman.v1.AttributableURI;
import schemas.dmtf.org.wbem.wsman.v1.ObjectFactory;

public class AddResourceURIInterceptor extends AbstractSoapInterceptor {

    private final String m_resourceUri;

    public AddResourceURIInterceptor(String resourceUri) {
        super(Phase.POST_LOGICAL);
        addAfter(SoapPreProtocolOutInterceptor.class.getName());
        m_resourceUri = Objects.requireNonNull(resourceUri, "resourceUri cannot be null");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
       List<Header> headers = message.getHeaders();

        AttributableURI uri = new AttributableURI();
        uri.setValue(m_resourceUri);
        ObjectFactory f = new ObjectFactory();
        JAXBElement<AttributableURI> resourceURI = f.createResourceURI(uri);

        Header header;
        try {
            header = new Header(resourceURI.getName(), resourceURI, new JAXBDataBinding(AttributableURI.class));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        headers.add(header);
        message.put(Header.HEADER_LIST, headers);
    }
}
