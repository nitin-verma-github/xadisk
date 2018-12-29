/*
 Copyright © 2010-2011, Nitin Verma (project owner for XADisk https://xadisk.dev.java.net/). All rights reserved.

 This source code is being made available to the public under the terms specified in the license
 "Eclipse Public License 1.0" located at http://www.opensource.org/licenses/eclipse-1.0.php.
 */
package org.xadisk.filesystem.virtual;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.xadisk.filesystem.DurableDiskSession;
import org.xadisk.filesystem.NativeSession;
import org.xadisk.filesystem.NativeXAFileSystem;
import org.xadisk.filesystem.TransactionInformation;
import org.xadisk.filesystem.exceptions.DirectoryNotEmptyException;
import org.xadisk.filesystem.exceptions.FileAlreadyExistsException;
import org.xadisk.filesystem.exceptions.FileNotExistsException;
import org.xadisk.filesystem.exceptions.FileUnderUseException;
import org.xadisk.filesystem.utilities.MiscUtils;

public class TransactionVirtualView {

    private final TransactionInformation owningTransaction;
    private final HashSet<File> filesWithLatestViewOnDisk = new HashSet<File>(5);
    private final HashSet<VirtualViewFile> viewFilesWithLatestViewOnDisk = new HashSet<VirtualViewFile>(5);
    private final HashSet<VirtualViewFile> viewFilesUsingBackupDir = new HashSet<VirtualViewFile>(5);
    private boolean transactionAlreadyDeclaredHeavyWrite = false;
    private final NativeSession owningSession;
    private final HashMap<File, VirtualViewDirectory> virtualViewDirs = new HashMap<File, VirtualViewDirectory>(10);
    private final NativeXAFileSystem xaFileSystem;
    private final DurableDiskSession diskSession;

    public TransactionVirtualView(TransactionInformation owningTransaction, NativeSession owningSession, NativeXAFileSystem xaFileSystem,
            DurableDiskSession diskSession) {
        this.owningTransaction = owningTransaction;
        this.owningSession = owningSession;
        this.xaFileSystem = xaFileSystem;
        this.diskSession = diskSession;
    }

    public void createFile(File f, boolean isDirectory)
            throws FileAlreadyExistsException, FileNotExistsException {
        if (f.getParentFile() == null) {
            throw new FileNotExistsException("<parent directory of the input file is null>");
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        parentVVD.createFile(f.getName(), isDirectory);
        viewFilesWithLatestViewOnDisk.remove(new VirtualViewFile(f, 0, this, xaFileSystem, diskSession));
        filesWithLatestViewOnDisk.remove(f);
    }

    public boolean deleteFile(File f)
            throws DirectoryNotEmptyException, FileNotExistsException, FileUnderUseException {
        if (f.getParentFile() == null) {
            throw new FileNotExistsException("<parent directory of the input file is null>");
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        if (parentVVD.isNormalFileBeingReadOrWritten(f.getName())) {
            throw new FileUnderUseException(f.getAbsolutePath(), false);
        }
        if (parentVVD.dirExists(f.getName())) {
            if (getVirtualViewDirectory(f).listFilesAndDirectories().length > 0) {
                throw new DirectoryNotEmptyException(f.getAbsolutePath());
            }
            parentVVD.deleteDir(f.getName());
            virtualViewDirs.remove(f);
            return true;
        }
        if (parentVVD.fileExists(f.getName())) {
            parentVVD.deleteFile(f.getName());
            VirtualViewFile vvfIfAny = parentVVD.removeVirtualViewFile(f.getName());
            if (vvfIfAny != null) {
                vvfIfAny.propagatedDeleteCall();
            }
            return false;
        }
        throw new FileNotExistsException(f.getAbsolutePath());
    }

    public boolean isNormalFileBeingReadOrWritten(File f) {
        try {
            if (MiscUtils.isRootPath(f)) {
                return false;
            }
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.isNormalFileBeingReadOrWritten(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public boolean fileExists(File f) {
        return fileExistsAndIsNormal(f) || fileExistsAndIsDirectory(f);
    }

    public boolean fileExistsAndIsNormal(File f) {
        try {
            if (MiscUtils.isRootPath(f)) {
                return false;//a root can never be a file.
            }
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.fileExists(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public boolean fileExistsAndIsDirectory(File f) {
        try {
            if (MiscUtils.isRootPath(f)) {
                return f.isDirectory(); //f may be a root.
            }
            VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
            return parentVVD.dirExists(f.getName());
        } catch (FileNotExistsException fne) {
            return false;
        }
    }

    public String[] listFiles(File dir) throws FileNotExistsException {
        VirtualViewDirectory vvd = getVirtualViewDirectory(dir);
        return vvd.listFilesAndDirectories();
    }

    public boolean isDirectoryWritable(File f) throws FileNotExistsException {
        if (MiscUtils.isRootPath(f)) {
            return f.canWrite();
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        return parentVVD.isDirWritable(f.getName());
    }

    public boolean isNormalFileWritable(File f) throws FileNotExistsException {
        if (MiscUtils.isRootPath(f)) {
            return false;
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        return parentVVD.isFileWritable(f.getName());
    }

    public boolean isDirectoryReadable(File f) throws FileNotExistsException {
        if (MiscUtils.isRootPath(f)) {
            return f.canRead();
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        return parentVVD.isDirReadable(f.getName());
    }

    public boolean isNormalFileReadable(File f) throws FileNotExistsException {
        if (MiscUtils.isRootPath(f)) {
            return false;
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        return parentVVD.isFileReadable(f.getName());
    }

    public VirtualViewFile getVirtualViewFile(File f) throws FileNotExistsException {
        if (MiscUtils.isRootPath(f)) {
            throw new FileNotExistsException(f.getAbsolutePath());
        }
        VirtualViewDirectory parentVVD = getVirtualViewDirectory(f.getParentFile());
        return parentVVD.getVirtualViewFile(f.getName());
    }

    //make this move atomic in itself.
    public void moveNormalFile(File src, File dest)
            throws FileAlreadyExistsException, FileNotExistsException, FileUnderUseException {
        if (src.getParentFile() == null) {
            throw new FileNotExistsException("<parent directory of the source file is null>");
        }
        if (dest.getParentFile() == null) {
            throw new FileNotExistsException("<parent directory of the destination file is null>");
        }
        VirtualViewDirectory srcParentVVD = getVirtualViewDirectory(src.getParentFile());
        VirtualViewDirectory destParentVVD = getVirtualViewDirectory(dest.getParentFile());
        if (srcParentVVD.isNormalFileBeingReadOrWritten(src.getName())) {
            throw new FileUnderUseException(src.getAbsolutePath(), false);
        }

        File srcPointingToPhysicalFile = srcParentVVD.pointsToPhysicalFile(src.getName());

        VirtualViewFile vvfSource = (VirtualViewFile) srcParentVVD.removeVirtualViewFile(src.getName());

        if (vvfSource != null) {
            boolean success = false;
            try {
                createFile(dest, false);
                destParentVVD.addVirtualViewFile(dest.getName(), vvfSource);
                vvfSource.propagatedMoveCall(dest);
                if (vvfSource.isUsingHeavyWriteOptimization()) {
                    VirtualViewFile sourceDummyVVF = new VirtualViewFile(src, -1, this, xaFileSystem, diskSession);
                    sourceDummyVVF.markDeleted();
                    viewFilesWithLatestViewOnDisk.add(sourceDummyVVF);
                    filesWithLatestViewOnDisk.add(dest);
                }
                success = true;
            } finally {
                if (!success) {
                    //to rollback the remove operation.
                    srcParentVVD.addVirtualViewFile(src.getName(), vvfSource);
                } else {
                    //we don't need to rollback this; this was never done in the first place.
                    srcParentVVD.deleteFile(src.getName());
                }
            }
        } else {
            viewFilesWithLatestViewOnDisk.remove(new VirtualViewFile(dest, 0, this, xaFileSystem, diskSession));
            filesWithLatestViewOnDisk.remove(dest);
            destParentVVD.moveFileInto(dest.getName(), srcPointingToPhysicalFile);
            srcParentVVD.deleteFile(src.getName());
        }
    }

    public void moveDirectory(File src, File dest)
            throws FileAlreadyExistsException, FileNotExistsException {
        if (src.getParentFile() == null) {
            throw new FileNotExistsException("<parent directory of the source directory is null>");
        }
        if (dest.getParentFile() == null) {
            throw new FileNotExistsException("<parent directory of the destination directory is null>");
        }
        VirtualViewDirectory srcParentVVD = getVirtualViewDirectory(src.getParentFile());
        VirtualViewDirectory destParentVVD = getVirtualViewDirectory(dest.getParentFile());
        if (destParentVVD.fileExists(dest.getName()) || destParentVVD.dirExists(dest.getName())) {
            throw new FileAlreadyExistsException(dest.getAbsolutePath());
        }
        if (!srcParentVVD.dirExists(src.getName())) {
            throw new FileNotExistsException(src.getAbsolutePath());
        }
        destParentVVD.moveDirectoryInto(dest.getName(), srcParentVVD.pointsToPhysicalDirectory(src.getName()));
        srcParentVVD.deleteDir(src.getName());
        updateDescendantVVDsWithPrefix(src, dest);
        updateVVDWithPath(src, dest);
    }

    private void updateVVDWithPath(File oldPath, File newPath) {
        VirtualViewDirectory vvd = virtualViewDirs.remove(oldPath);
        if (vvd != null) {
            virtualViewDirs.put(newPath, vvd);
            vvd.propagateMoveCall(newPath);
        }
    }

    private void updateDescendantVVDsWithPrefix(File ancestorOldName, File ancestorNewName) {
        File dirs[] = virtualViewDirs.keySet().toArray(new File[0]);
        for (File dirName : dirs) {
            ArrayList<String> stepsToDescendToVVD = MiscUtils.isDescedantOf(dirName, ancestorOldName);
            if (stepsToDescendToVVD != null) {
                StringBuilder newPathForVVD = new StringBuilder(ancestorNewName.getAbsolutePath());
                for (int j = stepsToDescendToVVD.size() - 1; j >= 0; j--) {
                    newPathForVVD.append(File.separator).append(stepsToDescendToVVD.get(j));
                }
                updateVVDWithPath(dirName, new File(newPathForVVD.toString()));
            }
        }
    }

    private VirtualViewDirectory getVirtualViewDirectory(File f) throws FileNotExistsException {
        VirtualViewDirectory vvd = virtualViewDirs.get(f);
        if (vvd != null) {
            return vvd;
        }
        VirtualViewDirectory ancestorOfTruth = null;;
        File childDirectory = f;
        ArrayList<String> pathSteps = new ArrayList<String>(10);

        if (MiscUtils.isRootPath(f)) {
            ancestorOfTruth = null;
        } else {
            File ancestor = f.getParentFile();
            pathSteps.add(childDirectory.getName());
            while (true) {
                pathSteps.add(ancestor.getName());
                ancestorOfTruth = virtualViewDirs.get(ancestor);
                if (ancestorOfTruth != null) {
                    break;
                }
                childDirectory = ancestor;
                if (MiscUtils.isRootPath(ancestor)) {
                    break;
                }
                ancestor = ancestor.getParentFile();
            }
        }
        if (ancestorOfTruth == null) {
            if (!f.isDirectory()) {
                throw new FileNotExistsException(f.getAbsolutePath());
            }
            vvd = new VirtualViewDirectory(f, f, this, xaFileSystem, diskSession);
            virtualViewDirs.put(f, vvd);
            return vvd;
        }
        if (!ancestorOfTruth.dirExists(childDirectory.getName())) {
            throw new FileNotExistsException(f.getAbsolutePath());
        }

        File childDirectoryPhysicalPath = ancestorOfTruth.pointsToPhysicalDirectory(childDirectory.getName());
        if (childDirectoryPhysicalPath != null) {
            StringBuilder physicalPathForVVD = new StringBuilder(childDirectoryPhysicalPath.getAbsolutePath());
            for (int i = pathSteps.size() - 3; i >= 0; i--) {
                physicalPathForVVD.append(File.separator).append(pathSteps.get(i));
            }
            File physicalDir = new File(physicalPathForVVD.toString());
            if (!physicalDir.isDirectory()) {
                throw new FileNotExistsException(f.getAbsolutePath());
            }
            vvd = new VirtualViewDirectory(f, physicalDir, this, xaFileSystem, diskSession);
        } else {
            vvd = new VirtualViewDirectory(f, null, this, xaFileSystem, diskSession);
        }
        virtualViewDirs.put(f, vvd);
        return vvd;
    }

    void beingUsedInHeavyWriteMode(VirtualViewFile vvf) throws IOException {
        if (!transactionAlreadyDeclaredHeavyWrite) {
            owningSession.declareTransactionUsingUndoLogs();
            transactionAlreadyDeclaredHeavyWrite =
                    true;
        }

        viewFilesWithLatestViewOnDisk.add(vvf);
        filesWithLatestViewOnDisk.add(vvf.getFileName());
    }

    void hasCreatedFileInBackDir(VirtualViewFile vvf) {
        viewFilesUsingBackupDir.add(vvf);
    }

    public HashSet<VirtualViewFile> getViewFilesUsingBackupDir() {
        return viewFilesUsingBackupDir;
    }

    public HashSet<File> getFilesWithLatestViewOnDisk() {
        return filesWithLatestViewOnDisk;
    }

    public HashSet<VirtualViewFile> getViewFilesWithLatestViewOnDisk() {
        return viewFilesWithLatestViewOnDisk;
    }

    TransactionInformation getOwningTransaction() {
        return owningTransaction;
    }

    NativeSession getOwningSession() {
        return owningSession;
    }
}
