import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
@WebServlet(name = "ReviewServlet", urlPatterns = {"/review/*"})
public class ReviewServlet extends HttpServlet {
  public static final String REVIEW_QUEUE = "reviews";

  @Override
  public void init() throws ServletException {
    try {
      RabbitMqChannelPool.initialize();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String pathInfo = request.getPathInfo();
    if (pathInfo == null || pathInfo.equals("/")) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Path info is missing");
      return;
    }
    String[] pathParts = pathInfo.split("/");
    if (pathParts.length <3){
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request path");
      return;
    }
    String reviewType = pathParts[1];
    String albumID = pathParts[2];
    String message = reviewType + ":" + albumID;
    Channel channel =  null;
    try {
      channel = RabbitMqChannelPool.borrowChannel();
      channel.exchangeDeclare("reviewExchange","direct",false);
      channel.queueDeclare(REVIEW_QUEUE, true, false, false, null);
      channel.queueBind(REVIEW_QUEUE,"reviewExchange","");
      channel.basicPublish("reviewExchange", "", null, message.getBytes(StandardCharsets.UTF_8));
      response.setStatus(HttpServletResponse.SC_ACCEPTED);
      RabbitMqChannelPool.returnChannel(channel);
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      e.printStackTrace();
    }
  }
}