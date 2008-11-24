//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The ServiceResolver queries three times consecutively for services of
 * a given type, and then removes itself from the timer.
 * <p/>
 * The ServiceResolver will run only if JmDNS is in state ANNOUNCED.
 * REMIND: Prevent having multiple service resolvers for the same type in the
 * timer queue.
 */
public class ServiceResolver extends TimerTask
{
    static Logger logger = Logger.getLogger(ServiceResolver.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;
    /**
     * Counts the number of queries being sent.
     */
    int count = 0;
    private String type;

    public ServiceResolver(JmDNSImpl jmDNSImpl, String type)
    {
        this.jmDNSImpl = jmDNSImpl;
        this.type = type;
    }

    public void start(Timer timer)
    {
        timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
    }

    public void run()
    {
        try
        {
            if (this.jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                if (count++ < 3)
                {
                    logger.finer("run() JmDNS querying service");
                    long now = System.currentTimeMillis();
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(new DNSQuestion(type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN));
                    for (Iterator s = this.jmDNSImpl.getServices().values().iterator(); s.hasNext();)
                    {
                        final ServiceInfoImpl info = (ServiceInfoImpl) s.next();
                        try
                        {
                            out.addAnswer(new DNSRecord.Pointer(info.getType(), DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.getQualifiedName()), now);
                        }
                        catch (IOException ee)
                        {
                            break;
                        }
                    }
                    this.jmDNSImpl.send(out);
                }
                else
                {
                    // After three queries, we can quit.
                    this.cancel();
                }
            }
            else
            {
                if (this.jmDNSImpl.getState() == DNSState.CANCELED)
                {
                    this.cancel();
                }
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.jmDNSImpl.recover();
        }
    }
}