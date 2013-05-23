/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Filetransfer imeplementation for yahoo.
 * @author Damian Minkov
 */
public class FileTransferImpl
    extends AbstractFileTransfer

{
    private ProtocolProviderServiceYahooImpl yahooProvider;
    private String id = null;
    private Contact contact = null;
    private File file = null;
    private int direction = -1;
    private long transferedBytes;

    public FileTransferImpl(ProtocolProviderServiceYahooImpl yahooProvider,
        String id, Contact contact, File file, int direction)
    {
        this.yahooProvider = yahooProvider;
        this.id = id;
        this.contact = contact;
        this.file = file;
        this.direction = direction;
    }

    /**
     * Cancels this file transfer. When this method is called transfer should
     * be interrupted.
     */
    @Override
    public void cancel()
    {
        yahooProvider.getYahooSession().cancelRunningFileTransfer(id);
    }

    /**
     * Returns the number of bytes already transfered through this file transfer.
     *
     * @return the number of bytes already transfered through this file transfer
     */
    @Override
    public long getTransferedBytes()
    {
        return transferedBytes;
    }

    /**
     * Uniquie ID that is identifying the FileTransfer
     * if the request has been accepted.
     *
     * @return the id.
     */
    public String getID()
    {
        return id;
    }

    /**
     * The file transfer direction.
     * @return returns the direction of the file transfer : IN or OUT.
     */
    public int getDirection()
    {
        return direction;
    }

    /**
     * Returns the file that is transfered.
     *
     * @return the file
     */
    public File getLocalFile()
    {
        return file;
    }

    /**
     * Returns the contact that we are transfering files with.
     * @return the contact.
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * @param transferedBytes the transferedBytes to set
     */
    public void setTransferedBytes(long transferedBytes)
    {
        this.transferedBytes = transferedBytes;
    }

}
