import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
public class ReviewConsumer implements Runnable{
  private Channel channel;
  private final String queueName;
  private DynamoDbClient dynamoDb;
  private final String tableName = "album"; // Change to your DynamoDB table name
  private final String AWS_ACCESS_KEY_ID="";
  private final String AWS_SECRET_ACCESS_KEY="";
  private final String AWS_SESSION_TOKEN="";

  public ReviewConsumer(String queueName) throws Exception {
    this.queueName = queueName;
    RabbitMqChannelPool.initialize();
    this.channel = RabbitMqChannelPool.borrowChannel();

    // Initialize DynamoDB client
    this.dynamoDb = DynamoDbClient.builder()
        .region(Region.US_WEST_2) // Change to your preferred region
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsSessionCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN)))
        .build();
  }



  @Override
  public void run() {

    try {
      channel.exchangeDeclare("reviewExchange","direct",false);
      channel.queueDeclare(queueName,true,false,false,null);
      channel.queueBind(queueName,"reviewExchange","");
      channel.basicQos(1500);
      DeliverCallback deliverCallback = this::handleDelivery;
      channel.basicConsume(queueName,true,deliverCallback,consumerTag -> {});
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      closeChannel();
    }
  }

  private void handleDelivery(String consumerTag, Delivery delivery) {
    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
    String[] URLSplit  = message.split(":");
    String reviewType = URLSplit[0];
    String albumID = URLSplit[1];
    processReview(reviewType,albumID);
  }

  private void processReview(String reviewType, String albumID) {
    String updateExpression;
    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

    if (reviewType.equals("like")) {
      updateExpression = "set likeCount = if_not_exists (likeCount,:start) + :val";
    } else {
      updateExpression = "set dislikeCount = if_not_exists (dislikeCount,:start) + :val";
    }

    expressionAttributeValues.put(":val", AttributeValue.builder().n("1").build());
    expressionAttributeValues.put(":start", AttributeValue.builder().n("0").build());

    UpdateItemRequest updateRequest = UpdateItemRequest.builder()
        .tableName(tableName)
        .key(Collections.singletonMap("albumID",AttributeValue.builder().s(albumID).build()))
        .updateExpression(updateExpression)
        .expressionAttributeValues(expressionAttributeValues)
        .build();

    dynamoDb.updateItem(updateRequest);
  }


  public void closeChannel() {
    try {
      if (channel != null && channel.isOpen()) {
        RabbitMqChannelPool.returnChannel(channel);
      }
    } catch (Exception e) {
      e.printStackTrace();
      // Handle closing exception
    }
  }

}
