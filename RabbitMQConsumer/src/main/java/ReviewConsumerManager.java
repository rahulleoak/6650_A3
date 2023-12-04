import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReviewConsumerManager {
  private static final int NUM_CONSUMERS_PER_QUEUE = 3;
  private static ExecutorService consumerThreadPool;
  public static final String REVIEW_QUEUE = "reviews";

  public static void startConsumerThreads() {
    consumerThreadPool = Executors.newFixedThreadPool(NUM_CONSUMERS_PER_QUEUE);
    for ( int i = 0; i < NUM_CONSUMERS_PER_QUEUE; i++){
      addConsumer(REVIEW_QUEUE);
    }
  }

  public static void addConsumer(String queueName){
    try {
      consumerThreadPool.execute(new ReviewConsumer(queueName));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    startConsumerThreads();
  }
}
