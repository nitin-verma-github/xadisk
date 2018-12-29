/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.bridge.proxies.facilitators;

import java.io.Serializable;

public class SerializedMethod implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String className;
    private final String methodName;
    private final String[] parameterTypesNames;

    public SerializedMethod(String className, String methodName, String[] parameterTypesNames) {
        this.className = className;
        this.methodName = methodName;
        this.parameterTypesNames = parameterTypesNames;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getParameterTypesNames() {
        return parameterTypesNames;
    }
}
