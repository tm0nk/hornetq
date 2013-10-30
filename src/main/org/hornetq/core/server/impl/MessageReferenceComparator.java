package org.hornetq.core.server.impl;

import java.util.Comparator;

import org.hornetq.core.server.MessageReference;

public class MessageReferenceComparator implements Comparator<MessageReference>
{
   public int compare(MessageReference ref1, MessageReference ref2)
   {
      long diff = ref1.getScheduledDeliveryTime() - ref2.getScheduledDeliveryTime();

      if (diff < 0)
      {
         return -1;
      }
      if (diff > 0)
      {
         return 1;
      }
      return 0;
   }
}
