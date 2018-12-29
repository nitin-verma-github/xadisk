/*
Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/


package org.xadisk.examples.jca;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import org.xadisk.additional.XAFileOutputStreamWrapper;
import org.xadisk.bridge.proxies.interfaces.XAFileOutputStream;
import org.xadisk.connector.outbound.XADiskConnection;
import org.xadisk.connector.outbound.XADiskConnectionFactory;
import org.xadisk.filesystem.exceptions.XAApplicationException;

/**
 * This example is applicable only for JavaEE applications (Web Applications, Session Beans). XADisk
 * can be used in the same way in Session Beans as in this Servlet.
 */
/**
 * This is a very basic example which
 *      - looks up a connection factory pointing to an XADisk instance. Such XADisk instance could be running inside
 *              the same JVM or a remote JVM.
 *      - retrieves a connection to the target XADisk instance.
 *      - performs some file-operations on the XADisk instance as part of a global transaction.
 */
/**
 * How to run this example:
 *
 * 1) Change the various constants used in the code below according to your environment: xadiskConnectionFactory and testFile.
 * 2) Put this Servlet class inside a Web-Application.
 * 3) Deploy XADisk as a Resource Adapter in the JavaEE (5.0 or above) server (independent of where is the
 *          XADisk instance this Servlet is connecting to).
 * 4) Configure a connection-factory according to the XADisk instance you want to invoke.
 * 5) Deploy the Web-Application and invoke this Servlet.
 */
/**
 * Please refer to the XADisk JavaDoc and User Guide for knowing more about using XADisk.
 */
public class ServletBasic extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    //the test-file used by the application logic.
    private static final String testFile = "C:\\orders3.csv";
    //the connection-factory pointing to the target XADisk instance.
    private static final String xadiskConnectionFactory = "xadiskcf";

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            out.println("<html><head><title>Servlet ServletBasic</title></head><body>");
            runApplicationModule1(out);
            out.println("</body></html>");
        } catch (Exception e) {
            out.println("Servel Encountered an Exception...<br><br>");
            e.printStackTrace(out);
            out.println("</body></html>");
        }
    }

    private void runApplicationModule1(PrintWriter out) throws Exception {
        File businessData = new File(testFile);
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        XADiskConnection connection = null;

        try {
            out.println("Executing the application-module1...");

            out.println("Starting the global transaction...<br>");
            utx.begin();

            out.println("Looking up a Connection Factory instance for XADisk...<br>");
            XADiskConnectionFactory cf1 = (XADiskConnectionFactory) new InitialContext().lookup(xadiskConnectionFactory);

            out.println("Retrieveing a Connection to interact with XADisk....<br>");
            connection = cf1.getConnection();

            out.println("Will do some operations on the file-system via XADisk...<br>");

            if (!connection.fileExists(businessData)) {
                connection.createFile(businessData, false);
            }

            XAFileOutputStream xaFOS = connection.createXAFileOutputStream(businessData, false);
            XAFileOutputStreamWrapper wrapperOS = new XAFileOutputStreamWrapper(xaFOS);
            wrapperOS.write((System.currentTimeMillis() + ", Coffee Beans, 5, 100, Street #11, Moon - 311674 \n").getBytes());
            wrapperOS.close();

            out.println("[We could also have done some work on database, JMS etc and all of the work "
                    + "will then become part of the same global transaction.]<br>");

            connection.setPublishFileStateChangeEventsOnCommit(true);
            connection.close();

            out.println("Committing the global transaction now...<br>");

            utx.commit();

            out.println("The application-module1 completed successfully.");
        } catch (XAApplicationException xaae) {
            out.println("The application-module1 could not execute successfully.");
            xaae.printStackTrace(out);
            if (connection != null) {
                connection.close();
            }
            out.println("Rolling back the global transaction...<br>");
            utx.rollback();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }
}
