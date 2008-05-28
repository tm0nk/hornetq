/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.messaging.core.remoting.impl.mina;

import org.jboss.messaging.core.logging.Logger;
import org.jboss.messaging.core.remoting.KeepAliveFactory;
import org.jboss.messaging.core.remoting.impl.wireformat.Ping;
import org.jboss.messaging.core.remoting.impl.wireformat.Pong;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * @version <tt>$Revision$</tt>
 */
public class ServerKeepAliveFactory implements KeepAliveFactory
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger
           .getLogger(ServerKeepAliveFactory.class);

   // Attributes ----------------------------------------------------

   // FIXME session mapping must be cleaned when the server session is closed:
   // either normally or exceptionally
   /**
    * Key = server session ID Value = client session ID
    */
   private List<Long> sessions = new ArrayList<Long>();

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // KeepAliveFactory implementation -------------------------------

   public Pong pong(long sessionID, Ping ping)
   {
      Pong pong = new Pong(sessionID, !sessions.contains(sessionID));
      pong.setTargetID(ping.getResponseTargetID());
      return pong;
   }

   public List<Long> getSessions()
   {
      return sessions;
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
