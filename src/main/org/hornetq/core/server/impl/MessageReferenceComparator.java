package org.hornetq.core.server.impl;

import java.util.Comparator;

import org.hornetq.core.server.MessageReference;

public class MessageReferenceComparator implements Comparator<MessageReference>
{
   public int compare(MessageReference ref1, MessageReference ref2)
   {
      long diff = ref1.getScheduledDeliveryTime() - ref2.getScheduledDeliveryTime();

      if (diff < 0L)
      {
         return -1;
      }
      if (diff > 0L)
      {
         return 1;
      }
      // Even if ref1 and ref2 have the same delivery time, we only want to return 0 if they are identical
      if (ref1 == ref2)
      {
         return 0;
      }
      else
      {
         // Same delivery time, different objects
         return 1;
      }
   }
}
