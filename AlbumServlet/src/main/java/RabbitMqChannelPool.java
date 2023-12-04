import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class RabbitMqChannelPool {
  private static Connection connection;
  private static ObjectPool<Channel> channelPool;

  public static void initialize() throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("34.215.189.203"); // Adjust as necessary
    connection = factory.newConnection();
    channelPool = new GenericObjectPool<>(new ChannelFactory(connection)); //think about blockingueue
  }

  public static Channel borrowChannel() throws Exception {
    return channelPool.borrowObject();
  }

  public static void returnChannel(Channel channel) throws Exception {
    channelPool.returnObject(channel);
  }

  public static void closePool() throws Exception{
    if (channelPool!= null){
      channelPool.close();
    }
    if (connection!=null && connection.isOpen()){
      connection.close();
    }
  }

}
