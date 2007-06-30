/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.transport.http;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class CommonsHTTPTransportSender extends AbstractHandler implements
        TransportSender {

    protected static final String PROXY_HOST_NAME = "proxy_host";

    protected static final String PROXY_PORT = "proxy_port";

    int soTimeout = HTTPConstants.DEFAULT_SO_TIMEOUT;

    /**
     * proxydiscription
     */
    protected TransportOutDescription proxyOutSetting = null;

    private static final Log log = LogFactory
            .getLog(CommonsHTTPTransportSender.class);

    protected String httpVersion = HTTPConstants.HEADER_PROTOCOL_11;

    private boolean chunked = false;

    int connectionTimeout = HTTPConstants.DEFAULT_CONNECTION_TIMEOUT;

    public void cleanup(MessageContext msgContext) throws AxisFault {
        HttpMethod httpMethod = (HttpMethod) msgContext
                .getProperty(HTTPConstants.HTTP_METHOD);

        if (httpMethod != null) {
            httpMethod.releaseConnection();
        }
    }

    public void init(ConfigurationContext confContext,
                     TransportOutDescription transportOut) throws AxisFault {

        // <parameter name="PROTOCOL">HTTP/1.0</parameter> or
        // <parameter name="PROTOCOL">HTTP/1.1</parameter> is
        // checked
        Parameter version = transportOut
                .getParameter(HTTPConstants.PROTOCOL_VERSION);
        if (version != null) {
            if (HTTPConstants.HEADER_PROTOCOL_11.equals(version.getValue())) {
                httpVersion = HTTPConstants.HEADER_PROTOCOL_11;

                Parameter transferEncoding = transportOut
                        .getParameter(HTTPConstants.HEADER_TRANSFER_ENCODING);

                if ((transferEncoding != null)
                        && HTTPConstants.HEADER_TRANSFER_ENCODING_CHUNKED
                        .equals(transferEncoding.getValue())) {
                    chunked = true;
                }
            } else if (HTTPConstants.HEADER_PROTOCOL_10.equals(version
                    .getValue())) {
                httpVersion = HTTPConstants.HEADER_PROTOCOL_10;
            } else {
                throw new AxisFault("Parameter "
                        + HTTPConstants.PROTOCOL_VERSION
                        + " Can have values only HTTP/1.0 or HTTP/1.1");
            }
        }

        // Get the timeout values from the configuration
        try {
            Parameter tempSoTimeoutParam = transportOut
                    .getParameter(HTTPConstants.SO_TIMEOUT);
            Parameter tempConnTimeoutParam = transportOut
                    .getParameter(HTTPConstants.CONNECTION_TIMEOUT);

            if (tempSoTimeoutParam != null) {
                soTimeout = Integer.parseInt((String) tempSoTimeoutParam
                        .getValue());
            }

            if (tempConnTimeoutParam != null) {
                connectionTimeout = Integer
                        .parseInt((String) tempConnTimeoutParam.getValue());
            }
        } catch (NumberFormatException nfe) {

            // If there's a problem log it and use the default values
            log.error("Invalid timeout value format: not a number", nfe);
        }
    }

    public void stop() {
        // Any code that , need to invoke when sender stop
    }

    public InvocationResponse invoke(MessageContext msgContext)
            throws AxisFault {
        try {
            OMOutputFormat format = new OMOutputFormat();
            // if (!msgContext.isDoingMTOM())
            msgContext.setDoingMTOM(HTTPTransportUtils.doWriteMTOM(msgContext));
            msgContext.setDoingSwA(HTTPTransportUtils.doWriteSwA(msgContext));
            msgContext.setDoingREST(HTTPTransportUtils.isDoingREST(msgContext));
            format.setSOAP11(msgContext.isSOAP11());
            format.setDoOptimize(msgContext.isDoingMTOM());
            format.setDoingSWA(msgContext.isDoingSwA());
            format.setCharSetEncoding(HTTPTransportUtils
                    .getCharSetEncoding(msgContext));

            Object mimeBoundaryProperty = msgContext
                    .getProperty(Constants.Configuration.MIME_BOUNDARY);
            if (mimeBoundaryProperty != null) {
                format.setMimeBoundary((String) mimeBoundaryProperty);
            }

            TransportOutDescription transportOut = msgContext.getConfigurationContext().
                    getAxisConfiguration().getTransportOut(Constants.TRANSPORT_HTTP);

            //if a parameter has set been set, we will omit the SOAP action for SOAP 1.2 
            if (transportOut != null) {
                if (!msgContext.isSOAP11()) {
                    Parameter param = transportOut.getParameter(HTTPConstants.OMIT_SOAP_12_ACTION);
                    Object parameterValue = null;
                    if (param != null) {
                        parameterValue = param.getValue();
                    }

                    if (parameterValue != null && JavaUtils.isTrueExplicitly(parameterValue)) {
                        //Check whether user has already overridden this.
                        Object propertyValue = msgContext.getProperty(Constants.Configuration.DISABLE_SOAP_ACTION);
                        if (propertyValue == null | !JavaUtils.isFalseExplicitly(propertyValue)) {
                            msgContext.setProperty(Constants.Configuration.DISABLE_SOAP_ACTION,
                                    Boolean.TRUE);
                        }
                    }
                }
            }

            // Transport URL can be different from the WSA-To. So processing
            // that now.
            EndpointReference epr = null;
            String transportURL = (String) msgContext
                    .getProperty(Constants.Configuration.TRANSPORT_URL);

            if (transportURL != null) {
                epr = new EndpointReference(transportURL);
            } else if (msgContext.getTo() != null
                    && !msgContext.getTo().hasAnonymousAddress()) {
                epr = msgContext.getTo();
            }

            // Check for the REST behavior, if you desire rest behavior
            // put a <parameter name="doREST" value="true"/> at the
            // server.xml/client.xml file
            // ######################################################
            // Change this place to change the wsa:toepr
            // epr = something
            // ######################################################

            if (epr != null) {
                if (!epr.hasNoneAddress()) {
                    writeMessageWithCommons(msgContext, epr, format);
                }
            }
            else {
                if (msgContext.getProperty(MessageContext.TRANSPORT_OUT) != null) {
                    sendUsingOutputStream(msgContext, format);
                } else {
                    throw new AxisFault(
                    "Both the TO and MessageContext.TRANSPORT_OUT property are Null, No where to send");
                }
            }

            TransportUtils.setResponseWritten(msgContext, true);
            
        } catch (XMLStreamException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        } catch (FactoryConfigurationError e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        }
        return InvocationResponse.CONTINUE;
    }

    /**
     * Send a message (which must be a response) via the OutputStream sitting in the
     * MessageContext TRANSPORT_OUT property.  Since this class is used for both requests and
     * responses, we split the logic - this method always gets called when we're
     * writing to the HTTP response stream, and sendUsingCommons() is used for requests.
     *
     * @param msgContext the active MessageContext
     * @param format output formatter for our message
     * @throws AxisFault if a general problem arises
     * @throws XMLStreamException if there's a problem writing
     */
    private void sendUsingOutputStream(MessageContext msgContext,
                                       OMOutputFormat format) throws AxisFault, XMLStreamException {

        

        OutputStream out = (OutputStream) msgContext
                .getProperty(MessageContext.TRANSPORT_OUT);

        // I Don't think we need this check.. Content type needs to be set in
        // any case. (thilina)
        // if (msgContext.isServerSide()) {
        OutTransportInfo transportInfo = (OutTransportInfo) msgContext
                .getProperty(Constants.OUT_TRANSPORT_INFO);

        if (transportInfo == null) throw new AxisFault("No transport info in MessageContext");

        ServletBasedOutTransportInfo servletBasedOutTransportInfo = null;
        if (transportInfo instanceof ServletBasedOutTransportInfo) {
            servletBasedOutTransportInfo =
                    (ServletBasedOutTransportInfo) transportInfo;
            List customHeaders = (List) msgContext.getProperty(HTTPConstants.HTTP_HEADERS);
            if (customHeaders != null) {
                Iterator iter = customHeaders.iterator();
                while (iter.hasNext()) {
                    Header header = (Header) iter.next();
                    if (header != null) {
                        servletBasedOutTransportInfo
                                .addHeader(header.getName(), header.getValue());
                    }
                }
            }
        }

        format.setAutoCloseWriter(true);

        MessageFormatter messageFormatter = TransportUtils.getMessageFormatter(msgContext);
        if (messageFormatter == null) throw new AxisFault("No MessageFormatter in MessageContext");

        transportInfo.setContentType(
                messageFormatter.getContentType(msgContext, format, findSOAPAction(msgContext)));

        Object gzip = msgContext.getOptions().getProperty(HTTPConstants.MC_GZIP_RESPONSE);
        if (gzip != null && JavaUtils.isTrueExplicitly(gzip)) {
            if (servletBasedOutTransportInfo != null)
                servletBasedOutTransportInfo.addHeader(HTTPConstants.HEADER_CONTENT_ENCODING,
                                                       HTTPConstants.COMPRESSION_GZIP);
            try {
                out = new GZIPOutputStream(out);
                out.write(messageFormatter.getBytes(msgContext, format));
                ((GZIPOutputStream) out).finish();
                out.flush();
            } catch (IOException e) {
                throw new AxisFault("Could not compress response");
            }
        } else {
            messageFormatter.writeTo(msgContext, format, out, false);
        }
    }

    private void writeMessageWithCommons(MessageContext messageContext,
                                         EndpointReference toEPR, OMOutputFormat format)
            throws AxisFault {
        try {
            URL url = new URL(toEPR.getAddress());

            // select the Message Sender depending on the REST status
            AbstractHTTPSender sender;

            sender = new HTTPSender();

            if (messageContext.getProperty(HTTPConstants.CHUNKED) != null) {
                chunked = JavaUtils.isTrueExplicitly(messageContext
                        .getProperty(HTTPConstants.CHUNKED));
            }

            if (messageContext.getProperty(HTTPConstants.HTTP_PROTOCOL_VERSION) != null) {
                httpVersion = (String) messageContext
                        .getProperty(HTTPConstants.HTTP_PROTOCOL_VERSION);
            }
            // Following order needed to be preserved because,
            // HTTP/1.0 does not support chunk encoding
            sender.setChunked(chunked);
            sender.setHttpVersion(httpVersion);
            sender.setFormat(format);

            sender.send(messageContext, url, findSOAPAction(messageContext));
        } catch (MalformedURLException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        } catch (HttpException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        } catch (IOException e) {
            log.debug(e);
            throw AxisFault.makeFault(e);
        }
    }

    private static String findSOAPAction(MessageContext messageContext) {
        String soapActionString = null;

        Object disableSoapAction = messageContext.getOptions().getProperty(
                Constants.Configuration.DISABLE_SOAP_ACTION);

        if (!JavaUtils.isTrueExplicitly(disableSoapAction)) {
            // first try to get the SOAP action from message context
            soapActionString = messageContext.getSoapAction();
            if ((soapActionString == null) || (soapActionString.length() == 0)) {
                // now let's try to get WSA action
                soapActionString = messageContext.getWSAAction();
                if (messageContext.getAxisOperation() != null
                        && ((soapActionString == null) || (soapActionString
                        .length() == 0))) {
                    // last option is to get it from the axis operation
                    soapActionString = messageContext.getAxisOperation()
                            .getInputAction();
                }
            }
        }

        //Since action is optional for SOAP 1.2 we can return null here.
        if (soapActionString == null && messageContext.isSOAP11()) {
            soapActionString = "\"\"";
        }

        return soapActionString;
    }
}
