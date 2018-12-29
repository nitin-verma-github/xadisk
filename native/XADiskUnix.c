/*
Copyright © 2010, 2011 Nitin Verma (project owner for XADisk http://xadisk.java.net/). All rights reserved.

This source code is being made available to the public under the terms specified in the license
"Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
*/

#include<jni.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

JNIEXPORT jboolean JNICALL Java_org_xadisk_filesystem_DurableDiskSession_forceDirectories
(JNIEnv *env, jclass callingMethod, jobjectArray dirPathStrings) {

    int totalDirectories = (*env) -> GetArrayLength(env, dirPathStrings);
    int i;

    for (i = 0; i < totalDirectories; i++) {
        jstring dirPathString = (*env)->GetObjectArrayElement(env, dirPathStrings, i);

        const char *dirPath = (*env)->GetStringUTFChars(
                env, dirPathString, NULL);

        int dir = open(dirPath, 0);

        if (dir == -1) {
            printf("XADisk Error [Native Module] Directory %s does not exist.\n", dirPath);
            return JNI_FALSE;
        }

        if (fsync(dir) == -1) {
            printf("XADisk Error [Native Module] Directory flush failed for %s.\n", dirPath);
            close(dir);
            return JNI_FALSE;
        }
        close(dir);
        (*env)->ReleaseStringUTFChars(env, dirPathString, dirPath);
    }
    return JNI_TRUE;
}

