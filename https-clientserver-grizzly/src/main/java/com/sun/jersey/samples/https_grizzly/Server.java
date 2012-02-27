/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 * 
 * Portions Copyright 2012 Talend
 */

package com.sun.jersey.samples.https_grizzly;
        
//import com.sun.jersey.samples.https_grizzly.auth.SecurityServletFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.servlet.ServletHandler;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.http.server.ServerConfiguration;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

public class Server {

    private static HttpServer webServer;

    public static final URI BASE_URI = getBaseURI();
    public static final String CONTENT = "CXF HTTPS EXAMPLE\n";

    private static URI getBaseURI() {
        return UriBuilder.fromUri("https://localhost/").port(getPort(4463)).build();
    }

    private static int getPort(int defaultPort) {
        String port = System.getProperty("jersey.test.port");
        if (null != port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
            }
        }
        return defaultPort;
    }

    protected static void startServer() {
    	// add CXF resource servlet
        ServletHandler cxfAdapter = new ServletHandler();
        cxfAdapter.setContextPath("/");
        cxfAdapter.setServletInstance(new org.apache.cxf.transport.servlet.CXFServlet());
        cxfAdapter.addServletListener("org.springframework.web.context.ContextLoaderListener");
        cxfAdapter.addContextParameter("contextConfigLocation", "beans.xml");

        // add security servlet filter (to handle http basic authentication)
        // commented-out because by default this sample uses a CXF JAX-RS filter (see beans.xml)
//      cxfAdapter.addFilter(new SecurityServletFilter(), "AuthorizationFilter", null);
              
        // Grizzly ssl configuration
        SSLContextConfigurator sslContext = new SSLContextConfigurator();
        
        // set up security context
        sslContext.setKeyStoreFile("./keystore_server"); // contains server keypair
        sslContext.setKeyStorePass("asdfgh");
        sslContext.setTrustStoreFile("./truststore_server"); // contains client certificate
        sslContext.setTrustStorePass("asdfgh");

        try {

            webServer = new HttpServer();
            NetworkListener listener = new NetworkListener("grizzly", 
            		getBaseURI().getHost(),
            		getBaseURI().getPort());
            listener.setSecure(true);
            listener.setSSLEngineConfig(
            		new SSLEngineConfigurator(sslContext).setClientMode(false).setNeedClientAuth(true));
            webServer.addListener(listener);
            
            ServerConfiguration config = webServer.getServerConfiguration();
            config.addHttpHandler(cxfAdapter, getBaseURI().getPath());            
            
            // start Grizzly embedded server //
            System.out.println("CXF app started. Try out " + BASE_URI + "\nHit CTRL + C to stop it...");
            webServer.start();

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    protected static void stopServer() {
        webServer.stop();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        startServer();

        System.in.read();
    }
}

