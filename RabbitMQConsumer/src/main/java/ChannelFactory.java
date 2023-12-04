import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ChannelFactory implements PooledObjectFactory<Channel> {
  private final Connection connection;

  public ChannelFactory(Connection connection) {
    this.connection = connection;
  }

  @Override
  public PooledObject<Channel> makeObject() throws Exception {
    Channel channel = connection.createChannel();
    return new DefaultPooledObject<>(channel);
  }

  @Override
  public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {
    Channel channel = pooledObject.getObject();
    if (channel != null && channel.isOpen()) {
      channel.close();
    }
  }

  @Override
  public boolean validateObject(PooledObject<Channel> p) {
    Channel channel = p.getObject();
    return channel != null && channel.isOpen();
  }

  @Override
  public void activateObject(PooledObject<Channel> p) throws Exception {
    // No operation is required when a channel is borrowed from the pool.
  }

  @Override
  public void passivateObject(PooledObject<Channel> p) throws Exception {
    // No operation is required when a channel is returned to the pool.
  }
}
