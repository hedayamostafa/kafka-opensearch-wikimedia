package com.demo.kafka.opensearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumer {

  private static final Logger log = LoggerFactory.getLogger(OpenSearchConsumer.class);
  private static final String INDEX_NAME = "wikimedia";
  private static final String TOPIC_NAME = "wikimedia.recentchange";
  private static final String BOOTSTRAP_SERVERS = "127.0.0.1:9092";
  private static final String GROUP_ID = "consumer-opensearch-demo";

  public static void main(String[] args) {

    try (RestHighLevelClient openSearchClient = createOpenSearchClient();
         KafkaConsumer<String, String> consumer = createKafkaConsumer()) {

      ensureIndexExists(openSearchClient);

      consumer.subscribe(Collections.singleton(TOPIC_NAME));

      processRecords(openSearchClient, consumer);

    } catch (WakeupException e) {
      log.info("Consumer is shutting down successfully.");
    } catch (Exception e) {
      log.error("Unexpected exception occurred:", e);
    }

    log.info("Application has exited.");
  }

  private static RestHighLevelClient createOpenSearchClient() {
    String connString = "http://localhost:9200";

    // we build a URI from the connection string
    RestHighLevelClient restHighLevelClient;
    URI connUri = URI.create(connString);
    // extract login information if it exists
    String userInfo = connUri.getUserInfo();

    if (userInfo == null) {
      // REST client without security
      restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

    } else {
      // REST client with security
      String[] auth = userInfo.split(":");

      CredentialsProvider cp = new BasicCredentialsProvider();
      cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

      restHighLevelClient = new RestHighLevelClient(
              RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                      .setHttpClientConfigCallback(
                              httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                      .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


    }

    return restHighLevelClient;
  }

  private static void ensureIndexExists(RestHighLevelClient openSearchClient) throws IOException {
    if (!openSearchClient.indices().exists(new GetIndexRequest(INDEX_NAME), RequestOptions.DEFAULT)) {
      CreateIndexRequest createIndexRequest = new CreateIndexRequest(INDEX_NAME);
      openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
      log.info("Index '{}' created successfully.", INDEX_NAME);
    } else {
      log.info("Index '{}' already exists.", INDEX_NAME);
    }
  }

  private static void processRecords(RestHighLevelClient openSearchClient, KafkaConsumer<String, String> consumer) {
    while (true) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));
      int recordsCount = records.count();
      log.info("Received {} record(s).", recordsCount);

      if (recordsCount > 0) {
        BulkRequest bulkRequest = new BulkRequest();

        for (ConsumerRecord<String, String> record : records) {
          try {
            String id = extractId(record.value());

            log.info("ID: {}", id);

            IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                    .source(record.value(), XContentType.JSON)
                    .id(id);

            bulkRequest.add(indexRequest);
          } catch (Exception e) {
            log.warn("Skipping record due to parsing error: {}", record.value(), e);
          }
        }

        if (bulkRequest.numberOfActions() > 0) {
          try {
            BulkResponse bulkResponse = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            log.info("Successfully inserted {} record(s) into OpenSearch.", bulkResponse.getItems().length);
          } catch (IOException e) {
            log.error("Failed to execute bulk request:", e);
          }

          consumer.commitSync();
          log.info("Offsets have been committed.");
        }
      }
    }
  }

  private static String extractId(String json) {
    try {
      return JsonParser.parseString(json)
              .getAsJsonObject()
              .get("meta")
              .getAsJsonObject()
              .get("id")
              .getAsString();
    } catch (Exception e) {
      log.error("Failed to extract ID from JSON:", e);
      throw e;
    }
  }

  private static KafkaConsumer<String, String> createKafkaConsumer() {
    Properties properties = new Properties();
    properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
    properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

    log.info("Kafka consumer created with group ID '{}'.", GROUP_ID);
    return new KafkaConsumer<>(properties);
  }
}
