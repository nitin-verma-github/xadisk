package org.xadisk.filesystem.exceptions;

import org.xadisk.bridge.proxies.interfaces.XAFileSystem;
import org.xadisk.filesystem.TransactionInformation;

/**
 * This exception is an unchecked exception thrown when a transaction is not able to
 * continue its work and is left in an inconsistent incomplete state. That is, the
 * transaction did not commit or rollback and will require an administrative
 * intervention for resolving inconsistency and then marking the transaction as
 * complete. A transaction can fail either during routine XADisk operations or
 * during the recovery phase of XADisk. For the routine cases, the
 * transaction will keep holding the locks (in-memory) over files/directories until
 * it is marked complete.
 *
 * <p> If a transaction fails during recovery (when xadisk is committing or rolling-back
 * the transactions running prior to the reboot), the recovery process will wait until
 * all these failed transactions are marked complete.
 *
 * <p> A failed transaction can be marked complete using
 * {@link XAFileSystem#declareTransactionAsComplete(byte[])}. The identifier for such
 * transaction can be obtained either from {@link #getTransactionIdentifier()}, or
 * {@link XAFileSystem#getIdentifiersForFailedTransactions()}.
 * 
 * <p> This exception is not expected to occur in general, and indicates a severe problem.
 *
 * @since 1.2.2
 */
public class TransactionFailedException extends XASystemException {

    private static final long serialVersionUID = 1L;
    private byte[] transactionIdentifier;

    public TransactionFailedException(Throwable cause, TransactionInformation xid) {
        super(cause);
        this.transactionIdentifier = xid.getBytes();
    }

    /**
     * Returns a byte-array identifier with which the transaction can be identified
     * to the XADisk instance for tasks like marking the transaction as complete.
     * @return the transaction identifier in byte-array form.
     */
    public byte[] getTransactionIdentifier() {
        return transactionIdentifier;
    }

    @Override
    public String getMessage() {
        return "The transaction has failed and has not completed commit or rollback. The "
                + "file-system data operated on by the transaction may be in inconsistent "
                + "state. This exception is not expected to occur in general, and "
                + "indicates a severe problem.";
    }
}
