/*
Copyright © 2010, 2011 Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
“Eclipse Public License 1.0” located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/

import java.io.File;

public class TestXADiskNative {

    public static void main(String args[]) {
        try {
            File f = new File("C:\\test");
            String paths[] = new String[2];
            for (int i = 0; i < 2; i++) {
                paths[i] = f.getAbsolutePath() + i;
            }
            boolean success = true;
            while(success) {
                success = forceDirectories(paths);
            }
            System.out.println("Success? " + success);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static native boolean forceDirectories(String directoryPaths[]);

    static {
        System.loadLibrary("xadisk");
    }
}
