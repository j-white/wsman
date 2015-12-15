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

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.Action;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.Addressing;

import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate;
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerateResponse;
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull;
import org.xmlsoap.schemas.ws._2004._09.enumeration.PullResponse;

//TODO: Remove me and use generated bindings?
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@BindingType(value="http://www.w3.org/2003/05/soap/bindings/HTTP/")
//FIXME: We should make addressing required, but we disable it since we can't currently
// mock the RelatesTo and MessageID headers in tests
@Addressing(required = false, enabled = true)
@WebService(targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/enumeration")
@XmlSeeAlso({schemas.dmtf.org.wbem.wsman.v1.AttributableEmpty.class})
public interface EnumerationOperations {

        @WebResult(name = "EnumerateResponse", 
            targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/enumeration", 
            partName = "body")
        @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate",
                output = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse")
        @WebMethod(operationName = "Enumerate")
        public EnumerateResponse enumerate(Enumerate enumerate);

        @WebResult(name = "PullResponse", 
            targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/enumeration", 
            partName = "body")
        @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/Pull",
            output = "http://schemas.xmlsoap.org/ws/2004/09/enumeration/PullResponse")
        @WebMethod(operationName = "Pull")
        public PullResponse pull(Pull pull);
}