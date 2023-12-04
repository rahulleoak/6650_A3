package org.ClientSide;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;


public class Client1 {

  private static final int initThreadGroupSize = 10;
  private static final int initAPICalls = 100;
  private static final int updatedAPICalls = 1000;
  private static final AtomicInteger totalRequests = new AtomicInteger(0);

  public static CloseableHttpClient createHttpClient() {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
    manager.setDefaultMaxPerRoute(100);
    manager.setMaxTotal(100);
    return HttpClients.custom().setConnectionManager(manager).disableAutomaticRetries().build();
  }

  public static void threadStart(ExecutorService executor, RequestHandler requestHandler, File imageFile) {
    for (int i = 0; i < initThreadGroupSize; i++) {
      executor.execute(() -> {
        for (int j = 0; j < initAPICalls; j++) {
          performRequests("POST", requestHandler, imageFile);
          performRequests("GET", requestHandler, imageFile);
        }
      });
    }
    executor.shutdown();
    while (!executor.isTerminated()){
    }
  }

  public static void testThreads(RequestHandler requestHandler,
      File imageFile, int delay) {

    for (int k = 0; k < updatedAPICalls; k++) {
      totalRequests.addAndGet(performRequests("POST", requestHandler, imageFile));
      totalRequests.addAndGet(performRequests("GET", requestHandler, imageFile));
    }
  }

  public static int performRequests(String requestType, RequestHandler requestHandler, File imageFile) {
    boolean successfulRequests = false;
    int retryCount = 1;


    while (!successfulRequests && retryCount < 5) {
      try {
        if ("GET".equals(requestType)) {
          requestHandler.sendGetRequest("1");
        } else if ("POST".equals(requestType)) {
          requestHandler.sendPostRequest("Sex Pistols", "Never Mind The Bollocks!", "1977", imageFile);
        }

        successfulRequests = true;
      } catch (Exception e) {
        e.printStackTrace();
        retryCount++;
      }
    }
    return retryCount;
  }


  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("Invalid args! " +
          "Arguments should be:  <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
      System.exit(1);
    }

    int threadGroupSize = Integer.parseInt(args[0]);
    int numThreadGroups = Integer.parseInt(args[1]);
    int delay = Integer.parseInt(args[2]);
    String IPAddr = args[3];

    CloseableHttpClient httpClient = createHttpClient();

    ExecutorService initExecutor = Executors.newFixedThreadPool(initThreadGroupSize);
    RequestHandler requestHandler = new RequestHandler(httpClient, IPAddr);
    File imageFile = new File("src/main/resources/nmtb.png");

    threadStart(initExecutor, requestHandler, imageFile);

    long startTime = System.currentTimeMillis();
    ExecutorService addExecutor = Executors.newFixedThreadPool(numThreadGroups);
    for (int i = 0; i < numThreadGroups; i++) {
      addExecutor.execute(() -> {
        ExecutorService executor1 = Executors.newFixedThreadPool(threadGroupSize);
        for (int j = 0; j < threadGroupSize; j++) {
          executor1.execute(() -> {
            //Print statement used to which pool and thread in that pool is running.
            //System.out.println(Thread.currentThread().getName());
            testThreads(requestHandler, imageFile, delay);
          });
        }
        executor1.shutdown();
        while (!executor1.isTerminated()) {
        }
      });
      try {
        Thread.sleep(delay * 1000L);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
    }
    addExecutor.shutdown();
    while (!addExecutor.isTerminated()) {
    }

    long endTime = System.currentTimeMillis();
    long wallTime = (endTime - startTime) / 1000;
    System.out.println(totalRequests.get());
    double throughput = (double) totalRequests.get() / wallTime;
    System.out.println("\nWall Time: " + wallTime + " seconds");
    System.out.println("Throughput: " + throughput + " requests per second");
  }
}
