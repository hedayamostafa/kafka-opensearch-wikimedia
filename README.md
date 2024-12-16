# Wikimedia Kafka OpenSearch Integration

This project demonstrates a Kafka producer and consumer pipeline to process live Wikimedia changes and index the data into OpenSearch. It uses Kafka to stream events from Wikimedia's API and processes them with OpenSearch for efficient searching and indexing.

---

## Overview

## Searching from OpenSearch Dashboard

Once the data is indexed into OpenSearch, you can search it via OpenSearch Dashboards:
<img width="1468" alt="Screenshot 2024-12-16 at 23 46 07" src="https://github.com/user-attachments/assets/e58942aa-b3f5-4083-8d49-040a781f69fa" />
### 1. Access OpenSearch Dashboards: Visit http://localhost:5601 in your browser.
### 2. Search Data:
- Navigate to the Dev Tools section in the sidebar.
- You can make a GET request to search the indexed data. For example:
<img width="1461" alt="Screenshot 2024-12-16 at 23 47 15" src="https://github.com/user-attachments/assets/ba63055e-53bf-4a17-932b-dc3d73b62dde" />

### Components

1. **Kafka Producer**:

   - Streams live changes from the Wikimedia API.
   - Pushes events to a Kafka topic named wikimedia.recentchange.

2. **Kafka Consumer**:

   - Reads messages from the Kafka topic.
   - Extracts metadata and indexes the data into OpenSearch for querying and analytics.

3. **OpenSearch**:

   - Used as the search and indexing engine for storing Wikimedia events.
---
## Requirements

- **Java 8+**
- **Apache Kafka** (installed and running locally)
- **OpenSearch** (or Elasticsearch equivalent)
- **Maven** (for dependency management)
- **Docker**
---

## Setup Instructions

### 1. Install Kafka

- Download and install Apache Kafka from [Kafka Downloads](https://kafka.apache.org/downloads).
- Start Zookeeper and Kafka server:
  
bash
  bin/zookeeper-server-start.sh config/zookeeper.properties
  bin/kafka-server-start.sh config/server.properties


### 2. Install OpenSearch

- Download and start OpenSearch from [OpenSearch Downloads](https://opensearch.org/downloads.html).
- Ensure it's running at http://localhost:9200.
<img width="1291" alt="Screenshot 2024-12-16 at 18 09 03" src="https://github.com/user-attachments/assets/1fc79e20-c605-4f8d-a023-4f5ae0c6339d" />

Note: If you're using Docker, the OpenSearch service is accessible on port 9200 and the OpenSearch Dashboards is accessible on port 5601.

<img width="1294" alt="Screenshot 2024-12-16 at 18 17 18" src="https://github.com/user-attachments/assets/a79819be-060a-47fb-8beb-bb4aca1c8aac" />

---

## Running the Application

### 1. Kafka Producer (WikimediaChangesProducer)

This component streams live changes from Wikimedia and produces them to a Kafka topic.

- **Run the producer**:

  
bash
  mvn compile exec:java -Dexec.mainClass="com.demo.kafka.opensearch.producer.wikimedia.WikimediaChangesProducer"


- The producer listens to the Wikimedia Event Stream at https://stream.wikimedia.org/v2/stream/recentchange and publishes data to Kafka.

### 2. Kafka Consumer (OpenSearchConsumer)

This component consumes messages from the Kafka topic and indexes them into OpenSearch.

- **Run the consumer**:
  
bash
  mvn compile exec:java -Dexec.mainClass="com.demo.kafka.opensearch.OpenSearchConsumer"


---

## Key Kafka Topics

- **wikimedia.recentchange**: The topic where Wikimedia events are published.

---

## Features

- **Asynchronous Kafka Producer**: Streams live Wikimedia events to Kafka.
- **Resilient Kafka Consumer**: Handles message consumption with bulk indexing into OpenSearch.
- **OpenSearch Integration**: Creates an index (wikimedia) and stores the streamed data for querying.
---

## Configuration

### Kafka Producer Properties

- bootstrap.servers: Kafka server URL (default: 127.0.0.1:9092).
- key.serializer: Serializer for message keys.
- value.serializer: Serializer for message values.

### Kafka Consumer Properties

- bootstrap.servers: Kafka server URL (default: 127.0.0.1:9092).
- group.id: Consumer group identifier.
- enable.auto.commit: Set to false to manually commit offsets.

---

## Dependencies

- [Apache Kafka](https://kafka.apache.org/)
- [OpenSearch Java High-Level REST Client](https://opensearch.org/docs/latest/clients/java/)
- [Gson](https://github.com/google/gson)
- [SLF4J](http://www.slf4j.org/)
- [LaunchDarkly EventSource](https://github.com/launchdarkly/java-eventsource)

---

## Logs

- Producer logs Wikimedia events being published to Kafka.
- Consumer logs record counts and indexing results for OpenSearch.

---

## Troubleshooting

- **Kafka Not Running**: Ensure the Kafka broker is running locally on 127.0.0.1:9092.
- **OpenSearch Connectivity**: Verify OpenSearch is accessible at http://localhost:9200.
- **Stream Errors**: Check logs for errors in handling Wikimedia events or OpenSearch indexing.
