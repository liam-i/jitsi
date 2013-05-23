/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.dtmf.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.neomedia.format.*;
import org.jitsi.service.protocol.*;
import org.jitsi.util.StringUtils; // Disambiguation

/**
 * Class responsible for sending a DTMF Tone using SIP INFO or using rfc4733.
 *
 * @author JM HEITZ
 * @author Vincent Lucas
 */
public class OperationSetDTMFSipImpl
    implements OperationSetDTMF
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetDTMFSipImpl.class);

    /**
     * The DTMF method used to send tones.
     */
    private DTMFMethod dtmfMethod;

    /**
     * DTMF mode sending DTMF as sip info.
     */
    private final DTMFInfo dtmfModeInfo;

    /**
     * The minimal tone duration.
     */
    private int minimalToneDuration;

    /**
     * The maximal tone duration.
     */
    private int maximalToneDuration;

    /**
     * Constructor.
     *
     * @param pps the SIP Protocol provider service
     */
    public OperationSetDTMFSipImpl(ProtocolProviderServiceSipImpl pps)
    {
        this.dtmfMethod = this.getDTMFMethod(pps);
        this.minimalToneDuration = this.getMinimalToneDuration(pps);
        this.maximalToneDuration = this.getMaximalToneDuration();
        dtmfModeInfo = new DTMFInfo(pps);
    }

    /**
     * Sends the <tt>DTMFTone</tt> <tt>tone</tt> to <tt>callPeer</tt>.
     *
     * @param callPeer the  call peer to send <tt>tone</tt> to.
     * @param tone the DTMF tone to send to <tt>callPeer</tt>.
     *
     * @throws OperationFailedException with code OPERATION_NOT_SUPPORTED if
     * DTMF tones are not supported for <tt>callPeer</tt>.
     *
     * @throws NullPointerException if one of the arguments is null.
     *
     * @throws IllegalArgumentException in case the call peer does not
     * belong to the underlying implementation.
     */
    public synchronized void startSendingDTMF(CallPeer callPeer, DTMFTone tone)
        throws OperationFailedException
    {
        if (callPeer == null || tone == null)
        {
            throw new NullPointerException("Argument is null");
        }
        if (! (callPeer instanceof CallPeerSipImpl))
        {
            throw new IllegalArgumentException();
        }

        CallPeerSipImpl cp = (CallPeerSipImpl) (callPeer);

        // If this account is configured to use SIP-INFO DTMF.
        if(this.dtmfMethod == DTMFMethod.SIP_INFO_DTMF)
        {
            dtmfModeInfo.startSendingDTMF(cp, tone);
        }
        // Else sends DTMF (dtmfMethod defined as RTP or INBAND) via the
        // AudioMediaStream interface.
        else
        {
            DTMFMethod cpDTMFMethod = dtmfMethod;

            // If "auto" DTMF mode selected, automatically selects RTP DTMF is
            // telephon-event is available. Otherwise selects INBAND DMTF.
            if(this.dtmfMethod == DTMFMethod.AUTO_DTMF)
            {
                if(isRFC4733Active(cp))
                {
                    cpDTMFMethod =  DTMFMethod.RTP_DTMF;
                }
                else
                {
                    cpDTMFMethod = DTMFMethod.INBAND_DTMF;
                }
            }

            // If the account is configured to use RTP DTMF method and the call
            // does not manage telephone events. Then, we log it for futur
            // debugging.
            if(this.dtmfMethod == DTMFMethod.RTP_DTMF && !isRFC4733Active(cp))
            {
                logger.debug("RTP DTMF used without telephon-event capacities");
            }

            ((AudioMediaStream)cp.getMediaHandler().getStream(MediaType.AUDIO))
                .startSendingDTMF(
                        tone,
                        cpDTMFMethod,
                        minimalToneDuration,
                        maximalToneDuration);
        }
    }

    /**
     * Stops sending DTMF.
     *
     * @param callPeer the call peer that we'd like to stop sending DTMF to.
     */
    public synchronized void stopSendingDTMF(CallPeer callPeer)
    {
        if (callPeer == null)
        {
            throw new NullPointerException("Argument is null");
        }
        if (! (callPeer instanceof CallPeerSipImpl))
        {
            throw new IllegalArgumentException();
        }

        CallPeerSipImpl cp = (CallPeerSipImpl) (callPeer);

        // If this account is configured to use SIP-INFO DTMF.
        if(this.dtmfMethod == DTMFMethod.SIP_INFO_DTMF)
        {
            dtmfModeInfo.stopSendingDTMF(cp);
        }
        // Else sends DTMF (dtmfMethod defined as RTP or INBAND) via the
        // AudioMediaStream interface.
        else
        {
            DTMFMethod cpDTMFMethod = dtmfMethod;

            // If "auto" DTMF mode selected, automatically selects RTP DTMF is
            // telephon-event is available. Otherwise selects INBAND DMTF.
            if(this.dtmfMethod == DTMFMethod.AUTO_DTMF)
            {
                if(isRFC4733Active(cp))
                {
                    cpDTMFMethod =  DTMFMethod.RTP_DTMF;
                }
                else
                {
                    cpDTMFMethod = DTMFMethod.INBAND_DTMF;
                }
            }

            // If the account is configured to use RTP DTMF method and the call
            // does not manage telephone events. Then, we log it for futur
            // debugging.
            if(this.dtmfMethod == DTMFMethod.RTP_DTMF && !isRFC4733Active(cp))
            {
                logger.debug("RTP DTMF used without telephon-event capacities");
            }

            ((AudioMediaStream)cp.getMediaHandler().getStream(MediaType.AUDIO))
                .stopSendingDTMF(cpDTMFMethod);
        }
    }

    /**
     * Checks whether rfc4733 is negotiated for this call.
     * @param peer the call peer.
     * @return whether we can use rfc4733 in this call.
     */
    private boolean isRFC4733Active(CallPeerSipImpl peer)
    {
        Iterator<MediaFormat> iter =
            peer.getMediaHandler().getStream(MediaType.AUDIO)
                .getDynamicRTPPayloadTypes().values().iterator();
        while (iter.hasNext())
        {
            MediaFormat mediaFormat = iter.next();
            if(mediaFormat.getEncoding().equals(Constants.TELEPHONE_EVENT))
                return true;
        }

        return false;
    }

    /**
     * Returns DTMFInfo mode implementation.
     * @return DTMFInfo mode implementation.
     */
    DTMFInfo getDtmfModeInfo()
    {
        return dtmfModeInfo;
    }

    /**
     * Returns the corresponding DTMF method used for this account.
     *
     * @param pps the SIP Protocol provider service
     *
     * @return the DTMFEnum corresponding to the DTMF method set for this
     * account.
     */
    private DTMFMethod getDTMFMethod(ProtocolProviderServiceSipImpl pps)
    {
        AccountID accountID = pps.getAccountID();

        String dtmfString = accountID.getAccountPropertyString("DTMF_METHOD");


        // Verifies that the DTMF_METHOD property string is correctly set.
        // If not, sets this account to the "auto" DTMF method and corrects the
        // property string.
        if(dtmfString == null
                || (!dtmfString.equals("AUTO_DTMF")
                    && !dtmfString.equals("RTP_DTMF")
                    && !dtmfString.equals("SIP_INFO_DTMF")
                    && !dtmfString.equals("INBAND_DTMF")))
        {
            dtmfString = "AUTO_DTMF";
            accountID.putAccountProperty("DTMF_METHOD", dtmfString);
        }

        if(dtmfString.equals("AUTO_DTMF"))
        {
            return DTMFMethod.AUTO_DTMF;
        }
        else if(dtmfString.equals("RTP_DTMF"))
        {
            return DTMFMethod.RTP_DTMF;
        }
        else if(dtmfString.equals("SIP_INFO_DTMF"))
        {
            return DTMFMethod.SIP_INFO_DTMF;
        }
        else // if(dtmfString.equals(INBAND_DTMF"))
        {
            return DTMFMethod.INBAND_DTMF;
        }
    }

    /**
     * Gets the minimal DTMF tone duration for this account.
     *
     * @param pps the SIP Protocol provider service
     *
     * @return The minimal DTMF tone duration for this account.
     */
    private int getMinimalToneDuration(ProtocolProviderServiceSipImpl pps)
    {
        AccountID accountID = pps.getAccountID();
        String minimalToneDurationString
            = accountID.getAccountPropertyString("DTMF_MINIMAL_TONE_DURATION");
        int minimalToneDuration
            = OperationSetDTMF.DEFAULT_DTMF_MINIMAL_TONE_DURATION;
        // Check if there is a specific value for this account.
        if(!StringUtils.isNullOrEmpty(minimalToneDurationString))
        {
            minimalToneDuration = Integer.valueOf(minimalToneDurationString);
        }
        // Else look at the global property.
        else
        {
            ConfigurationService cfg = SipActivator.getConfigurationService();
            // Check if there is a custom value for the minimal tone duration.
            if(cfg != null)
            {
                minimalToneDuration = cfg.getInt(
                        OperationSetDTMF.PROP_MINIMAL_RTP_DTMF_TONE_DURATION,
                        minimalToneDuration);
            }
        }
        return minimalToneDuration;
    }

    /**
     * Gets the maximal DTMF tone duration for this account.
     *
     * @return The maximal DTMF tone duration for this account.
     */
    private int getMaximalToneDuration()
    {
        int maximalToneDuration
            = OperationSetDTMF.DEFAULT_DTMF_MAXIMAL_TONE_DURATION;

        // Look at the global property.
        ConfigurationService cfg
            = SipActivator.getConfigurationService();
        // Check if there is a custom value for the maximal tone duration.
        if(cfg != null)
        {
            maximalToneDuration = cfg.getInt(
                    OperationSetDTMF.PROP_MAXIMAL_RTP_DTMF_TONE_DURATION,
                    maximalToneDuration);
        }

        return maximalToneDuration;
    }
}
