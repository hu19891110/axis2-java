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

package org.apache.axis2.deployment;

import java.io.File;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axis2.AbstractTestCase;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.phaseresolver.PhaseException;

public class MessageFormatterDeploymentTest extends AbstractTestCase {
    /**
     * Constructor.
     */
    public MessageFormatterDeploymentTest(String testName) {
        super(testName);
    }

    public void testBuilderSelection() throws AxisFault {
        String repositoryName  ="target/test-resources/deployment";
        File repo = new File(repositoryName);
        String xmlFile = "test-resources/deployment/messageFormatterTest/axis2.xml";
        File xml = new File(xmlFile);
        FileSystemConfigurator fsc = new FileSystemConfigurator(repo.getAbsolutePath(), xml.getAbsolutePath());
        AxisConfiguration axisConfig = fsc.getAxisConfiguration();
        String className = axisConfig.getMessageBuilder("application/soap+xml");
        assertEquals(className,"org.apache.axis2.transport.http.SOAPMessageFormatter");
    }
    
    public void testBuilderSelectionInvalidEntry() {
        String repositoryName  ="target/test-resources/deployment";
        File repo = new File(repositoryName);
        String xmlFile = "test-resources/deployment/messageFormatterTest/bad-axis2.xml";
        File xml = new File(xmlFile);
        FileSystemConfigurator fsc = new FileSystemConfigurator(repo.getAbsolutePath(), xml.getAbsolutePath());
        AxisFault testFault=null;
        try {
			fsc.getAxisConfiguration();
		} catch (AxisFault e) {
			testFault = e;
		}
		assertNotNull(testFault);
    }
}
