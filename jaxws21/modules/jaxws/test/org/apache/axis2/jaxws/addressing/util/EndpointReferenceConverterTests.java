/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis2.jaxws.addressing.util;

import java.io.StringReader;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.addressing.EndpointReferenceHelper;
import org.apache.axis2.addressing.AddressingConstants.Final;
import org.apache.axis2.addressing.AddressingConstants.Submission;
import org.apache.axis2.jaxws.addressing.SubmissionEndpointReference;
import org.apache.axis2.jaxws.addressing.util.EndpointReferenceConverter;
import org.custommonkey.xmlunit.XMLTestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * This class uses the JAX-WS Dispatch API to test sending and receiving
 * messages using SOAP 1.2.
 */
public class EndpointReferenceConverterTests extends XMLTestCase {
    private static final OMFactory OMF = OMAbstractFactory.getOMFactory();
    private static final QName ELEMENT200508 =
        new QName(Final.WSA_NAMESPACE, "EndpointReference", "wsa");
    private static final QName ELEMENT200408 =
        new QName(Submission.WSA_NAMESPACE, "EndpointReference", "wsa");
    
    private static final String EPR200508 =
    "<wsa:EndpointReference xmlns:axis2=\"http://ws.apache.org/namespaces/axis2\" xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" axis2:AttrExt=\"123456789\">"+
        "<wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>"+
        "<wsa:ReferenceParameters xmlns:fabrikam=\"http://example.com/fabrikam\">"+
            "<fabrikam:CustomerKey>123456789</fabrikam:CustomerKey>"+
            "<fabrikam:ShoppingCart>ABCDEFG</fabrikam:ShoppingCart>"+
        "</wsa:ReferenceParameters>"+
        "<wsa:Metadata>"+
            "<axis2:MetaExt axis2:AttrExt=\"123456789\">123456789</axis2:MetaExt>"+
        "</wsa:Metadata>"+
        "<axis2:EPRExt axis2:AttrExt=\"123456789\">123456789</axis2:EPRExt>"+
    "</wsa:EndpointReference>";
    
    private static final String EPR200408 =
    "<wsa:EndpointReference xmlns:axis2=\"http://ws.apache.org/namespaces/axis2\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" axis2:AttrExt=\"123456789\">"+
        "<wsa:Address>http://schemas.xmlsoap.org/ws/2004/08/addressing/anonymous</wsa:Address>"+
        "<wsa:ReferenceParameters xmlns:fabrikam=\"http://example.com/fabrikam\">"+
            "<fabrikam:CustomerKey>123456789</fabrikam:CustomerKey>"+
            "<fabrikam:ShoppingCart>ABCDEFG</fabrikam:ShoppingCart>"+
        "</wsa:ReferenceParameters>"+
        "<wsa:PortType>axis2:Jane</wsa:PortType>"+
        "<wsa:ServiceName PortName=\"Fred\">axis2:John</wsa:ServiceName>"+
        "<axis2:EPRExt axis2:AttrExt=\"123456789\">123456789</axis2:EPRExt>"+
    "</wsa:EndpointReference>";
    
    public EndpointReferenceConverterTests(String name) {
        super(name);
    }
    
    public void test200508ConversionStartingFromAxis2() throws Exception {
        XMLStreamReader parser =
            XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(EPR200508));
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        OMElement omElement = builder.getDocumentElement();
        
        org.apache.axis2.addressing.EndpointReference axis2EPR =
            EndpointReferenceHelper.fromOM(omElement);
        W3CEndpointReference jaxwsEPR =
            EndpointReferenceConverter.convertFromAxis2(axis2EPR, W3CEndpointReference.class);
        assertXMLEqual(EPR200508, jaxwsEPR.toString());
     
        org.apache.axis2.addressing.EndpointReference axis2Result =
            EndpointReferenceConverter.convertToAxis2(jaxwsEPR);
        OMElement eprElement =
            EndpointReferenceHelper.toOM(OMF, axis2Result, ELEMENT200508, Final.WSA_NAMESPACE);
        assertXMLEqual(EPR200508, eprElement.toString());
    }
    
    public void test200508ConversionStartingFromJAXWS() throws Exception {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        dbfac.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document jaxwsDoc = docBuilder.parse(new InputSource(new StringReader(EPR200508)));
        Source source = new DOMSource(jaxwsDoc);
        
        W3CEndpointReference jaxwsEPR = new W3CEndpointReference(source);
        org.apache.axis2.addressing.EndpointReference axis2EPR =
            EndpointReferenceConverter.convertToAxis2(jaxwsEPR);
        OMElement eprElement =
            EndpointReferenceHelper.toOM(OMF, axis2EPR, ELEMENT200508, Final.WSA_NAMESPACE);
        assertXMLEqual(EPR200508, eprElement.toString());

        W3CEndpointReference jaxwsResult =
            EndpointReferenceConverter.convertFromAxis2(axis2EPR, W3CEndpointReference.class);
        assertXMLEqual(EPR200508, jaxwsResult.toString());
    }
    
    public void test200408ConversionStartingFromAxis2() throws Exception {
        XMLStreamReader parser =
            XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(EPR200408));
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        OMElement omElement = builder.getDocumentElement();
        
        org.apache.axis2.addressing.EndpointReference axis2EPR =
            EndpointReferenceHelper.fromOM(omElement);
        SubmissionEndpointReference jaxwsEPR =
            EndpointReferenceConverter.convertFromAxis2(axis2EPR, SubmissionEndpointReference.class);
        assertXMLEqual(EPR200408, jaxwsEPR.toString());
     
        org.apache.axis2.addressing.EndpointReference axis2Result =
            EndpointReferenceConverter.convertToAxis2(jaxwsEPR);
        OMElement eprElement =
            EndpointReferenceHelper.toOM(OMF, axis2Result, ELEMENT200408, Submission.WSA_NAMESPACE);
        assertXMLEqual(EPR200408, eprElement.toString());
    }
    
    public void test200408ConversionStartingFromJAXWS() throws Exception {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        dbfac.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document jaxwsDoc = docBuilder.parse(new InputSource(new StringReader(EPR200408)));
        Source source = new DOMSource(jaxwsDoc);
        
        SubmissionEndpointReference jaxwsEPR = new SubmissionEndpointReference(source);
        org.apache.axis2.addressing.EndpointReference axis2EPR =
            EndpointReferenceConverter.convertToAxis2(jaxwsEPR);
        OMElement eprElement =
            EndpointReferenceHelper.toOM(OMF, axis2EPR, ELEMENT200408, Submission.WSA_NAMESPACE);
        assertXMLEqual(EPR200408, eprElement.toString());

        SubmissionEndpointReference jaxwsResult =
            EndpointReferenceConverter.convertFromAxis2(axis2EPR, SubmissionEndpointReference.class);
        assertXMLEqual(EPR200408, jaxwsResult.toString());
    }
    
    public void testFailures() throws Exception {
        try {
            EndpointReferenceConverter.convertFromAxis2(null, W3CEndpointReference.class);
            fail("Expected a failure.");
        }
        catch (Exception e) {
            //pass
        }

        try {
            EndpointReferenceConverter.convertFromAxis2(null, SubmissionEndpointReference.class);
            fail("Expected a failure.");
        }
        catch (Exception e) {
            //pass
        }

        try {
            EndpointReferenceConverter.convertFromAxis2(null, null);
            fail("Expected a failure.");
        }
        catch (Exception e) {
            //pass
        }

        try {
            EndpointReferenceConverter.convertToAxis2(null);
            fail("Expected a failure.");
        }
        catch (Exception e) {
            //pass
        }
    }
}
