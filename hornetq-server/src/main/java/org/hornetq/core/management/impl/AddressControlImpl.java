/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.core.management.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanOperationInfo;

import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.management.AddressControl;
import org.hornetq.core.paging.PagingManager;
import org.hornetq.core.paging.PagingStore;
import org.hornetq.core.persistence.StorageManager;
import org.hornetq.core.postoffice.Binding;
import org.hornetq.core.postoffice.Bindings;
import org.hornetq.core.postoffice.PostOffice;
import org.hornetq.core.postoffice.QueueBinding;
import org.hornetq.core.security.CheckType;
import org.hornetq.core.security.Role;
import org.hornetq.core.settings.HierarchicalRepository;
import org.hornetq.utils.json.JSONArray;
import org.hornetq.utils.json.JSONObject;

/**
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 *
 *
 */
public class AddressControlImpl extends AbstractControl implements AddressControl
{

   // Constants -----------------------------------------------------

   // Attributes ----------------------------------------------------

   private final SimpleString address;

   private final PostOffice postOffice;

   private final PagingManager pagingManager;

   private final HierarchicalRepository<Set<Role>> securityRepository;

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   public AddressControlImpl(final SimpleString address,
                             final PostOffice postOffice,
                             final PagingManager pagingManager,
                             final StorageManager storageManager,
                             final HierarchicalRepository<Set<Role>> securityRepository) throws Exception
   {
      super(AddressControl.class, storageManager);
      this.address = address;
      this.postOffice = postOffice;
      this.pagingManager = pagingManager;
      this.securityRepository = securityRepository;
   }

   // Public --------------------------------------------------------

   // AddressControlMBean implementation ----------------------------

   public String getAddress()
   {
      return address.toString();
   }

   public String[] getQueueNames() throws Exception
   {
      clearIO();
      try
      {
         Bindings bindings = postOffice.getBindingsForAddress(address);
         List<String> queueNames = new ArrayList<String>();
         for (Binding binding : bindings.getBindings())
         {
            if (binding instanceof QueueBinding)
            {
               queueNames.add(binding.getUniqueName().toString());
            }
         }
         return queueNames.toArray(new String[queueNames.size()]);
      }
      catch (Throwable t)
      {
         throw new IllegalStateException(t.getMessage());
      }
      finally
      {
         blockOnIO();
      }
   }

   public String[] getBindingNames() throws Exception
   {
      clearIO();
      try
      {
         Bindings bindings = postOffice.getBindingsForAddress(address);
         String[] bindingNames = new String[bindings.getBindings().size()];
         int i = 0;
         for (Binding binding : bindings.getBindings())
         {
               bindingNames[i++] = binding.getUniqueName().toString();
         }
         return bindingNames;
      }
      catch (Throwable t)
      {
         throw new IllegalStateException(t.getMessage());
      }
      finally
      {
         blockOnIO();
      }
   }

   public Object[] getRoles() throws Exception
   {
      clearIO();
      try
      {
         Set<Role> roles = securityRepository.getMatch(address.toString());

         Object[] objRoles = new Object[roles.size()];

         int i = 0;
         for (Role role : roles)
         {
            objRoles[i++] = new Object[] { role.getName(),
                                          CheckType.SEND.hasRole(role),
                                          CheckType.CONSUME.hasRole(role),
                                          CheckType.CREATE_DURABLE_QUEUE.hasRole(role),
                                          CheckType.DELETE_DURABLE_QUEUE.hasRole(role),
                                          CheckType.CREATE_NON_DURABLE_QUEUE.hasRole(role),
                                          CheckType.DELETE_NON_DURABLE_QUEUE.hasRole(role),
                                          CheckType.MANAGE.hasRole(role) };
         }
         return objRoles;
      }
      finally
      {
         blockOnIO();
      }
   }

   public String getRolesAsJSON() throws Exception
   {
      clearIO();
      try
      {
         JSONArray json = new JSONArray();
         Set<Role> roles = securityRepository.getMatch(address.toString());

         for (Role role : roles)
         {
            json.put(new JSONObject(role));
         }
         return json.toString();
      }
      finally
      {
         blockOnIO();
      }
   }

   public long getNumberOfBytesPerPage() throws Exception
   {
      clearIO();
      try
      {
         return pagingManager.getPageStore(address).getPageSizeBytes();
      }
      finally
      {
         blockOnIO();
      }
   }

   public long getAddressSize() throws Exception
   {
      clearIO();
      try
      {
         return pagingManager.getPageStore(address).getAddressSize();
      }
      finally
      {
         blockOnIO();
      }
   }

   public long getNumberOfMessages() throws Exception
   {
      clearIO();
      long totalMsgs = 0;
      try
      {
         Bindings bindings = postOffice.getBindingsForAddress(address);
         List<String> queueNames = new ArrayList<String>();
         for (Binding binding : bindings.getBindings())
         {
            if (binding instanceof QueueBinding)
            {
               totalMsgs += ((QueueBinding) binding).getQueue().getMessageCount();
            }
         }
         return totalMsgs;
      }
      catch (Throwable t)
      {
         throw new IllegalStateException(t.getMessage());
      }
      finally
      {
         blockOnIO();
      }
   }


   public boolean isPaging() throws Exception
   {
      clearIO();
      try
      {
         return pagingManager.getPageStore(address).isPaging();
      }
      finally
      {
         blockOnIO();
      }
   }

   public int getNumberOfPages() throws Exception
   {
      clearIO();
      try
      {
         PagingStore pageStore = pagingManager.getPageStore(address);

         if (!pageStore.isPaging())
         {
            return 0;
         }
         else
         {
            return pagingManager.getPageStore(address).getNumberOfPages();
         }
      }
      finally
      {
         blockOnIO();
      }
   }

   @Override
   protected MBeanOperationInfo[] fillMBeanOperationInfo()
   {
      return MBeanInfoHelper.getMBeanOperationsInfo(AddressControl.class);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------
}
