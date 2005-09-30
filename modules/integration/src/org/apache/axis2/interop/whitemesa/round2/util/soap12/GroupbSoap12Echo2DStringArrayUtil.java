/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis2.interop.whitemesa.round2.util.soap12;

import org.apache.axis2.interop.whitemesa.round2.util.SunRound2ClientUtil;
import org.apache.axis2.om.OMAbstractFactory;
import org.apache.axis2.om.OMElement;
import org.apache.axis2.soap.SOAPBody;
import org.apache.axis2.soap.SOAPEnvelope;
import org.apache.axis2.soap.SOAPFactory;

public class GroupbSoap12Echo2DStringArrayUtil implements SunRound2ClientUtil {

     public SOAPEnvelope getEchoSoapEnvelope() {

            SOAPFactory omfactory = OMAbstractFactory.getSOAP12Factory();
            SOAPEnvelope reqEnv = omfactory.createSOAPEnvelope();
            //reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/envelope/", "soapenv");
            //reqEnv.declareNamespace("http://schemas.xmlsoap.org/wsdl/", "xmlns");
            //reqEnv.declareNamespace("http://schemas.xmlsoap.org/wsdl/soap/", "soap");
            reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema", "xsd");
            reqEnv.declareNamespace("http://schemas.xmlsoap.org/soap/encoding/", "SOAP-ENC");
            reqEnv.declareNamespace("http://soapinterop.org/", "tns");
            reqEnv.declareNamespace("http://soapinterop.org/xsd", "s");
            //reqEnv.declareNamespace("http://schemas.xmlsoap.org/wsdl/", "wsdl");
            reqEnv.declareNamespace("http://www.w3.org/2001/XMLSchema-instance","xsi");

        OMElement operation = omfactory.createOMElement("echo2DStringArray", "http://soapinterop.org/", null);
        SOAPBody body = omfactory.createSOAPBody(reqEnv);
        body.addChild(operation);
        operation.addAttribute("soapenv:encodingStyle", "http://www.w3.org/2003/05/soap-encoding", null);

        OMElement part = omfactory.createOMElement("input2DStringArray", "", null);
        part.addAttribute("xsi:type", "SOAP-ENC:Array", null);
        part.addAttribute("SOAP-ENC:arrayType", "xsd:string[2,2]", null);

        OMElement value0 = omfactory.createOMElement("varString", "", null);
        value0.addAttribute("xsi:type", "xsd:string", null);
        value0.addChild(omfactory.createText("strss fdfing1"));
        OMElement value1 = omfactory.createOMElement("varString", "", null);
        value1.addAttribute("xsi:type", "xsd:string", null);
        value1.addChild(omfactory.createText("sdfsdf25"));
        OMElement value2 = omfactory.createOMElement("varString", "", null);
        value2.addAttribute("xsi:type", "xsd:string", null);
        value2.addChild(omfactory.createText("25dsasd dfasdas23"));
        OMElement value3 = omfactory.createOMElement("varString", "", null);
        value3.addAttribute("xsi:type", "xsd:string", null);
        value3.addChild(omfactory.createText("25dsasd dfasdas23"));

        part.addChild(value0);
        part.addChild(value1);
        part.addChild(value2);
        part.addChild(value3);

        operation.addChild(part);

        //reqEnv.getBody().addChild(method);
        return reqEnv;
    }
}
