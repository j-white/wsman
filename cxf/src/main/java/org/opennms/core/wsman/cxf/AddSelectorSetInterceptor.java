/*
 * Copyright 2015, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.core.wsman.cxf;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import schemas.dmtf.org.wbem.wsman.v1.ObjectFactory;
import schemas.dmtf.org.wbem.wsman.v1.SelectorSetType;
import schemas.dmtf.org.wbem.wsman.v1.SelectorType;

public class AddSelectorSetInterceptor extends AbstractSoapInterceptor {

    private final Map<String, String> m_selectors;

    public AddSelectorSetInterceptor(Map<String, String> selectors) {
        super(Phase.POST_LOGICAL);
        addAfter(SoapPreProtocolOutInterceptor.class.getName());
        m_selectors = Objects.requireNonNull(selectors, "selector cannot be null");
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        List<Header> headers = message.getHeaders();
        
        ObjectFactory factory = new ObjectFactory();
        SelectorSetType selectorSetType = factory.createSelectorSetType();

        for (Entry<String, String> selectorEntry : m_selectors.entrySet()) {
            SelectorType selector = factory.createSelectorType();
            selector.setName(selectorEntry.getKey());
            selector.getContent().add(selectorEntry.getValue());
            selectorSetType.getSelector().add(selector);
        }

        JAXBElement<SelectorSetType> el = factory.createSelectorSet(selectorSetType);

        Header header;
        try {
            header = new Header(el.getName(), el, new JAXBDataBinding(el.getValue().getClass()));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

        headers.add(header);
        message.put(Header.HEADER_LIST, headers);
    }
}
