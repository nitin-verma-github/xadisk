/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.virtual;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.xadisk.filesystem.DurableDiskSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;

class VirtualViewDirectory {

    //1. not all instances of locked files may end-up in the below maps, so never rely on these maps in those directions.
    //2. below maps are populated whenever files/dirs are created/deleted/moved-in in the transaction. Adding to the map even for existence/permission
    //check is risky because if after checking, the operation throws exception the locks are released, but it the entry would remain in this map.
    //3. a locked file/dir in the map does not imply read/write permission, it simply means that a change was made in that file/dir. So, we do not
    //depend upon the entries in the map for permission checks.
    private final HashMap<String, LockedFileInfo> lockedFilesInfo = new HashMap<String, LockedFileInfo>(20);
    private final HashMap<String, LockedFileInfo> lockedDirsInfo = new HashMap<String, LockedFileInfo>(20);
    private final File pointsToPhysicalDirectory;
    private final HashMap<String, VirtualViewFile> virtualViewFiles = new HashMap<String, VirtualViewFile>(20);
    private final TransactionVirtualView owningView;
    private File virtualDirName;
    private final NativeXAFileSystem xaFileSystem;
    private final DurableDiskSession diskSession;

    VirtualViewDirectory(File virtualDirName, File pointsToPhysicalDirectory, TransactionVirtualView owningView,
            NativeXAFileSystem xaFileSystem, DurableDiskSession diskSession) {
        this.owningView = owningView;
        this.virtualDirName = virtualDirName;
        this.pointsToPhysicalDirectory = pointsToPhysicalDirectory;
        this.xaFileSystem = xaFileSystem;
        this.diskSession = diskSession;
    }

    void createFile(String fileName, boolean isDirectory)
            throws FileAlreadyExistsException {
        if (fileExists(fileName) || dirExists(fileName)) {
            throw new FileAlreadyExistsException(fileName);
        }
        if (!isWritable()) {
            //throw new InsufficientPermissionOnFileException();
        }
        if (isDirectory) {
            lockedDirsInfo.put(fileName, new LockedFileInfo(null, true));
        } else {
            lockedFilesInfo.put(fileName, new LockedFileInfo(null, true));
        }
    }

    void moveDirectoryInto(String dirName, File pointsToPhysicalDir)
            throws FileAlreadyExistsException {
        if (fileExists(dirName) || dirExists(dirName)) {
            throw new FileAlreadyExistsException(dirName);
        }
        if (!isWritable()) {
            //throw new InsufficientPermissionOnFileException();
        }
        lockedDirsInfo.put(dirName, new LockedFileInfo(pointsToPhysicalDir, true));
    }

    void moveFileInto(String fileName, File pointsToPhysicalFile)
            throws FileAlreadyExistsException {
        if (fileExists(fileName) || dirExists(fileName)) {
            throw new FileAlreadyExistsException(fileName);
        }
        if (!isWritable()) {
            //throw new InsufficientPermissionOnFileException();
        }
        lockedFilesInfo.put(fileName, new LockedFileInfo(pointsToPhysicalFile, true));
    }

    void deleteFile(String fileName) throws FileNotExistsException {
        if (!fileExists(fileName)) {
            throw new FileNotExistsException(virtualDirName.getAbsolutePath() + File.separator + fileName);
        }
        if (!isWritable()) {
            //throw new InsufficientPermissionOnFileException();
        }
        lockedFilesInfo.put(fileName, new LockedFileInfo(null, false));
    }

    void deleteDir(String fileName) throws FileNotExistsException {
        if (!dirExists(fileName)) {
            throw new FileNotExistsException(virtualDirName.getAbsolutePath() + File.separator + fileName);
        }
        if (!this.isWritable()) {
            //throw new InsufficientPermissionOnFileException();
        }
        lockedDirsInfo.put(fileName, new LockedFileInfo(null, false));
    }

    boolean fileExists(String file) {
        LockedFileInfo lockedFileInfo = lockedFilesInfo.get(file);
        if (lockedFileInfo != null) {
            return lockedFileInfo.isExisting();
        } else {
            if (pointsToPhysicalDirectory == null) {
                return false;
            } else {
                return new File(pointsToPhysicalDirectory, file).isFile();
            }
        }
    }

    boolean dirExists(String file) {
        LockedFileInfo lockedDirInfo = lockedDirsInfo.get(file);
        if (lockedDirInfo != null) {
            return lockedDirInfo.isExisting();
        } else {
            if (pointsToPhysicalDirectory == null) {
                return false;
            } else {
                return new File(pointsToPhysicalDirectory, file).isDirectory();
            }
        }
    }

    boolean isWritable() {
        if (pointsToPhysicalDirectory != null) {
            return pointsToPhysicalDirectory.canWrite();
        }
        return true;
    }

    private File getPhysicalPath(String name, boolean isDirectory) {
        //this method assumes the existence check has been done.
        LockedFileInfo lockedInfo;
        if (isDirectory) {
            lockedInfo = lockedDirsInfo.get(name);
        } else {
            lockedInfo = lockedFilesInfo.get(name);
        }
        if (lockedInfo != null) {
            File pointsToPhysical = lockedInfo.getPointsToPhysical();
            return pointsToPhysical;
        } else {
            if (pointsToPhysicalDirectory == null) {
                return null;
            } else {
                return new File(pointsToPhysicalDirectory, name);
            }
        }
    }

    File pointsToPhysicalFile(String file) throws FileNotExistsException {
        if (!fileExists(file)) {
            throw new FileNotExistsException(virtualDirName.getAbsolutePath() + File.separator + file);
        }
        return getPhysicalPath(file, false);
    }

    File pointsToPhysicalDirectory(String file) throws FileNotExistsException {
        if (!dirExists(file)) {
            throw new FileNotExistsException(virtualDirName.getAbsolutePath() + File.separator + file);
        }
        return getPhysicalPath(file, true);
    }

    private void updateFileDirExistence(Set<String> allFilesDirs, HashMap<String, LockedFileInfo> lockedFilesDirsInfo) {
        for (Entry<String, LockedFileInfo> entry : lockedFilesDirsInfo.entrySet()) {
            LockedFileInfo fileDirInfo = entry.getValue();
            if (fileDirInfo.isExisting()) {
                //adding does not mean that the file was not found from physical dir above.
                allFilesDirs.add(entry.getKey());
            } else {
                allFilesDirs.remove(entry.getKey());
            }
        }
    }

    String[] listFilesAndDirectories() {
        Set<String> allFilesDirs = new HashSet<String>();
        if (pointsToPhysicalDirectory != null) {
            allFilesDirs.addAll(Arrays.asList(pointsToPhysicalDirectory.list()));
        }
        updateFileDirExistence(allFilesDirs, lockedFilesInfo);
        updateFileDirExistence(allFilesDirs, lockedDirsInfo);
        return allFilesDirs.toArray(new String[0]);
    }

    private boolean isPermissionAvailable(String name, boolean isDirectory, boolean writePermission) {
        LockedFileInfo lockedInfo;
        if (isDirectory) {
            lockedInfo = lockedDirsInfo.get(name);
        } else {
            lockedInfo = lockedFilesInfo.get(name);
        }
        if (lockedInfo != null) {
            if (!lockedInfo.isExisting()) {
                return false;
            }
            File pointsToPhysical = lockedInfo.getPointsToPhysical();
            if (pointsToPhysical != null) {
                if (writePermission) {
                    return pointsToPhysical.canWrite();
                } else {
                    return pointsToPhysical.canRead();
                }
            } else {
                return true;
            }
        } else {
            if (pointsToPhysicalDirectory == null) {
                return false;
            } else {
                if (writePermission) {
                    return new File(pointsToPhysicalDirectory, name).canWrite();
                } else {
                    return new File(pointsToPhysicalDirectory, name).canRead();
                }
            }
        }
    }

    boolean isFileWritable(String fileName) {
        return isPermissionAvailable(fileName, false, true);
    }

    boolean isFileReadable(String fileName) {
        return isPermissionAvailable(fileName, false, false);
    }

    boolean isDirWritable(String fileName) {
        return isPermissionAvailable(fileName, true, true);
    }

    boolean isDirReadable(String fileName) {
        return isPermissionAvailable(fileName, true, false);
    }

    File getPointsToPhysicalDirectory() {
        return pointsToPhysicalDirectory;
    }

    private boolean isVirtualFileBeingWritten(String fileName) {
        VirtualViewFile vvf = virtualViewFiles.get(fileName);
        if (vvf == null) {
            return false;
        }
        return vvf.isBeingWritten();
    }

    private boolean isVirtualFileBeingRead(String fileName) {
        VirtualViewFile vvf = virtualViewFiles.get(fileName);
        if (vvf == null) {
            return false;
        }
        return vvf.isBeingRead();
    }

    boolean isNormalFileBeingReadOrWritten(String fileName) {
        return isVirtualFileBeingRead(fileName) || isVirtualFileBeingWritten(fileName);
    }

    VirtualViewFile getVirtualViewFile(String fileName) throws FileNotExistsException {
        VirtualViewFile vvf = virtualViewFiles.get(fileName);
        if (vvf != null) {
            return vvf;
        }
        File pointingToPhysicalFile = pointsToPhysicalFile(fileName);
        File virtualFileName = new File(virtualDirName.getAbsolutePath(), fileName);
        if (pointingToPhysicalFile != null) {
            vvf = new VirtualViewFile(virtualFileName, pointingToPhysicalFile.length(), owningView, pointingToPhysicalFile,
                    pointingToPhysicalFile.length(), xaFileSystem, diskSession);
            vvf.setMappedToThePhysicalFileTill(pointingToPhysicalFile.length());
            vvf.setMappedToPhysicalFile(pointingToPhysicalFile);
        } else {
            vvf = new VirtualViewFile(virtualFileName, 0, owningView, xaFileSystem, diskSession);
            vvf.setMappedToThePhysicalFileTill(-1);
        }
        virtualViewFiles.put(fileName, vvf);
        return vvf;
    }

    VirtualViewFile removeVirtualViewFile(String filename) {
        return virtualViewFiles.remove(filename);
    }

    void addVirtualViewFile(String filename, VirtualViewFile vvf) {
        virtualViewFiles.put(filename, vvf);
    }

    void propagateMoveCall(File targetPath) {
        this.virtualDirName = targetPath;
        for (String fileName : virtualViewFiles.keySet()) {
            VirtualViewFile vvf = (VirtualViewFile) virtualViewFiles.get(fileName);
            vvf.propagatedAncestorMoveCall(new File(targetPath.getAbsolutePath(), fileName));
        }
    }

    private class LockedFileInfo {

        private File pointsToPhysical;
        private boolean existing;

        public LockedFileInfo(File pointsToPhysical, boolean existing) {
            this.pointsToPhysical = pointsToPhysical;
            this.existing = existing;
        }

        public File getPointsToPhysical() {
            return pointsToPhysical;
        }

        public void setPointsToPhysical(File pointsToPhysical) {
            this.pointsToPhysical = pointsToPhysical;
        }

        public boolean isExisting() {
            return existing;
        }

        public void setExisting(boolean existing) {
            this.existing = existing;
        }
    }
}
