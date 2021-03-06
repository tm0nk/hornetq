/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hornetq.tests.integration.stomp;
import org.junit.Before;
import org.junit.After;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.junit.Assert;

import org.hornetq.core.protocol.stomp.StompProtocolManagerFactory;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.HornetQServers;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.client.HornetQJMSConnectionFactory;
import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.impl.JMSServerManagerImpl;
import org.hornetq.tests.unit.util.InVMNamingContext;
import org.hornetq.tests.util.UnitTestCase;

public abstract class StompTestBase extends UnitTestCase
{
   protected final int port = 61613;

   private Socket stompSocket;

   private ByteArrayOutputStream inputBuffer;

   private ConnectionFactory connectionFactory;

   protected Connection connection;

   protected Session session;

   protected Queue queue;

   protected Topic topic;

   protected JMSServerManager server;

   protected String defUser = "brianm";

   protected String defPass = "wombats";

   protected boolean autoCreateServer = true;



   // Implementation methods
   // -------------------------------------------------------------------------
   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();

      if (autoCreateServer)
      {
         server = createServer();
         server.start();
         connectionFactory = createConnectionFactory();

         stompSocket = createSocket();
         inputBuffer = new ByteArrayOutputStream();

         connection = connectionFactory.createConnection();
         session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
         queue = session.createQueue(getQueueName());
         topic = session.createTopic(getTopicName());
         connection.start();
      }
   }

   protected void setUpAfterServer() throws Exception
   {
      setUpAfterServer(false);
   }

   protected void setUpAfterServer(boolean jmsCompressLarge) throws Exception
   {
      connectionFactory = createConnectionFactory();
      HornetQConnectionFactory hqFact = (HornetQConnectionFactory)connectionFactory;

      hqFact.setCompressLargeMessage(jmsCompressLarge);
      
      stompSocket = createSocket();
      inputBuffer = new ByteArrayOutputStream();

      connection = connectionFactory.createConnection();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      queue = session.createQueue(getQueueName());
      topic = session.createTopic(getTopicName());
      connection.start();

   }

   /**
   * @return
   * @throws Exception
   */
   protected JMSServerManager createServer() throws Exception
   {
      Configuration config = createBasicConfig();
      config.setSecurityEnabled(false);
      config.setPersistenceEnabled(false);

      Map<String, Object> params = new HashMap<String, Object>();
      params.put(TransportConstants.PROTOCOL_PROP_NAME, StompProtocolManagerFactory.STOMP_PROTOCOL_NAME);
      params.put(TransportConstants.PORT_PROP_NAME, TransportConstants.DEFAULT_STOMP_PORT);
      params.put(TransportConstants.STOMP_CONSUMERS_CREDIT, "-1");
      TransportConfiguration stompTransport = new TransportConfiguration(NettyAcceptorFactory.class.getName(), params);
      config.getAcceptorConfigurations().add(stompTransport);
      config.getAcceptorConfigurations().add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
      HornetQServer hornetQServer = HornetQServers.newHornetQServer(config, defUser, defPass);

      JMSConfiguration jmsConfig = new JMSConfigurationImpl();
      jmsConfig.getQueueConfigurations()
               .add(new JMSQueueConfigurationImpl(getQueueName(), null, false, getQueueName()));
      jmsConfig.getTopicConfigurations().add(new TopicConfigurationImpl(getTopicName(), getTopicName()));
      server = new JMSServerManagerImpl(hornetQServer, jmsConfig);
      server.setContext(new InVMNamingContext());
      return server;
   }

   @Override
   @After
   public void tearDown() throws Exception
   {
      if (autoCreateServer)
      {
         connection.close();
         if (stompSocket != null)
         {
            stompSocket.close();
         }
         server.stop();
      }
      super.tearDown();
   }

   protected void cleanUp() throws Exception
   {
      connection.close();
      if (stompSocket != null)
      {
         stompSocket.close();
      }
   }

   protected void reconnect() throws Exception
   {
      reconnect(0);
   }

   protected void reconnect(long sleep) throws Exception
   {
      stompSocket.close();

      if (sleep > 0)
      {
         Thread.sleep(sleep);
      }

      stompSocket = createSocket();
      inputBuffer = new ByteArrayOutputStream();
   }

   protected ConnectionFactory createConnectionFactory()
   {
      return new HornetQJMSConnectionFactory(false, new TransportConfiguration(InVMConnectorFactory.class.getName()));
   }

   protected Socket createSocket() throws IOException
   {
      return new Socket("localhost", port);
   }

   protected String getQueueName()
   {
      return "test";
   }

   protected String getQueuePrefix()
   {
      return "jms.queue.";
   }

   protected String getTopicName()
   {
      return "testtopic";
   }

   protected String getTopicPrefix()
   {
      return "jms.topic.";
   }

   public void sendFrame(String data) throws Exception
   {
      byte[] bytes = data.getBytes("UTF-8");
      OutputStream outputStream = stompSocket.getOutputStream();
      for (byte b : bytes)
      {
         outputStream.write(b);
      }
      outputStream.flush();
   }

   public void sendFrame(byte[] data) throws Exception
   {
      OutputStream outputStream = stompSocket.getOutputStream();
      for (byte element : data)
      {
         outputStream.write(element);
      }
      outputStream.flush();
   }

   public String receiveFrame(long timeOut) throws Exception
   {
      stompSocket.setSoTimeout((int)timeOut);
      InputStream is = stompSocket.getInputStream();
      int c = 0;
      for (;;)
      {
         c = is.read();
         if (c < 0)
         {
            throw new IOException("socket closed.");
         }
         else if (c == 0)
         {
            c = is.read();
            if (c != '\n')
            {
               byte[] ba = inputBuffer.toByteArray();
               System.out.println(new String(ba, "UTF-8"));
            }
            Assert.assertEquals("Expecting stomp frame to terminate with \0\n", c, '\n');
            byte[] ba = inputBuffer.toByteArray();
            inputBuffer.reset();
            return new String(ba, "UTF-8");
         }
         else
         {
            inputBuffer.write(c);
         }
      }
   }

   public void sendMessage(String msg) throws Exception
   {
      sendMessage(msg, queue);
   }

   public void sendMessage(String msg, Destination destination) throws Exception
   {
      MessageProducer producer = session.createProducer(destination);
      TextMessage message = session.createTextMessage(msg);
      producer.send(message);
   }

   public void sendMessage(byte[] data, Destination destination) throws Exception
   {
      sendMessage(data, "foo", "xyz", destination);
   }

   public void sendMessage(String msg, String propertyName, String propertyValue) throws Exception
   {
      sendMessage(msg.getBytes("UTF-8"), propertyName, propertyValue, queue);
   }

   public void sendMessage(byte[] data, String propertyName, String propertyValue, Destination destination) throws Exception
   {
      MessageProducer producer = session.createProducer(destination);
      BytesMessage message = session.createBytesMessage();
      message.setStringProperty(propertyName, propertyValue);
      message.writeBytes(data);
      producer.send(message);
   }

   protected void waitForReceipt() throws Exception
   {
      String frame = receiveFrame(50000);
      assertNotNull(frame);
      assertTrue(frame.indexOf("RECEIPT") > -1);
   }

   protected void waitForFrameToTakeEffect() throws InterruptedException
   {
      // bit of a dirty hack :)
      // another option would be to force some kind of receipt to be returned
      // from the frame
      Thread.sleep(500);
   }
}
