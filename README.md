<p align="center">
    <img src="img/m2klogo.png" alt="logo" width="250"/>
</p>

## What's M2K?
M2K is a numeronym-acronym which stands for "**MQTT to Kinesis**". Have you ever wanted a way to quickly and easily bridge your MQTT broker into AWS Kinesis Data Streams? MQTT provides exactly that functionality, and strives to do it right.

## Why put MQTT messages on AWS Kinesis?
There are many advantages to putting MQTT messages on Kinesis, but this depends on your use-case. There are many situations where moving messages of a MQTT broker to Kinesis is necessary, such as:
* Most of your infrastructure is running in the cloud.
* You want to distribute your MQTT messages to another source (Kinesis has great outreach).
* You want to process your MQTT messages and want to do it efficiently.

## How to use M2K?
**M2K ought to be run as a standalone app.** So far there's no client library which you can plug in to your application, but given enough interest it could be implemented. As such, there are two ways that you can do this:

### a) The Easy Way
M2K provides a docker container image which you can pull [here](https://github.com/OpenSrcerer/M2K4j/pkgs/container/m2k4j). Run this image in a container, providing your environment variables & you're good to go.

### b) The More Annoying Way
If you don't want to use the provided docker image, you can also clone this repository and build the project from source. You can do that by going into the project root and running `./gradlew build`, or an equivalent syntax depending on your OS.

After that finishes, there will be an uber-jar named `M2K4j-X.Y.Z.jar` where `X.Y.Z` is the current version. Use this jar file to run the application (don't forget to provide environment variables here as well).

## How to configure M2K?
In the root of this project you will find a file named `.env.example`. This file is a template for all the configuration that M2K needs. The convention is that this file is copied into a file named `.env`, then referenced through either a command, or a docker container configuration. Here's an example `docker-compose.yml` which does just that:
**Remember, if you want default variables, leave them commented out in the `.env` file.**
```yaml
version: "3.8"

services:
  m2k4j:
    container_name: M2K4j-instance
    image: ghcr.io/opensrcerer/m2k4j:v0.1.6
    env_file:
      - .env
```

### Configurable Parameters (values provided are defaults)

```java
##################################################
#                      MQTT
##################################################
MQTT_HOST= // Required. Hostname for your MQTT broker. 
MQTT_USERNAME= // Leave commented out if your broker does not need authentication.
MQTT_PASSWORD= // Leave commented out if your broker does not need authentication.
MQTT_PORT=1883 // Connection port for your MQTT broker.
MQTT_CLIENTID=M2K // Name of the client that will connect to your broker. It will show up in the connection as ${MQTT_CLIENTID}-randomUUID
MQTT_SUBSCRIPTIONS=# // Subscription topics. Supports a comma separated list of subscriptions, example: "abc/#, lol/1342, N/#" (with no quotes).

##################################################
#                      AWS
##################################################

AWS_KINESIS_STREAM_ARN= // Required. The ARN of your Kinesis Data Stream. 
        
// The strategy to retrieve the partitioning key for your messages. 
// Accepts PAYLOAD_HASH, MQTT_TOPIC or custom JSON pointer expression.
AWS_KINESIS_PARTITIONING_STRATEGY=PAYLOAD_HASH 
        
AWS_REGION=eu-west-1 // AWS region of your Kinesis Data Stream.
        
// NOT RECOMMENDED, MAKE SURE YOU KNOW WHAT YOU ARE DOING IF YOU PASS CREDENTIALS THIS WAY.
// RATHER USE OTHER WAYS, SUCH AS AN ECS ROLE OR .aws/credentials FILE.
// https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
AWS_ACCESS_KEY=
AWS_SECRET_ACCESS_KEY=

##################################################
#                      OTHER
##################################################

// Logging level of the app. Accepts ERROR, WARN, INFO, DEBUG, TRACE
APP_LOGGING_LEVEL=INFO
```

## [Partitioning Key Strategies](https://docs.aws.amazon.com/streams/latest/dev/key-concepts.html)

Quick Rundown: A Kinesis Data stream can have multiple shards, each with limited receiving throughput. In Kinesis, every message sent to it has a _partition key_, which is used to find out which shard that message should go to. This is also used for ordering, but to find out more click the title of this subheading and consult the AWS Docs.

You want this spread to be as even as possible to not get ratelimited, so M2K uses different partitioning methods to spread the message in different shards. You can choose between three strategies:

### 1. `PAYLOAD_HASH` (default)
This is a simple but effective method to spread messages evenly. Doesn't need configuration, and as such is recommended to be used unless you have a good reason to switch it. May fall back to `MQTT_TOPIC` (only for the affected message) if a MQTT mesage does not have a payload.

### 2. `MQTT_TOPIC`
In this approach, the MQTT topic of every message is used as its partitioning key. This can create hot shards, so only use it if you know that you are subscribing to multiple topics.

### 3. `JSON_KEY`
Here, you pass a [JSON Pointer Expression](https://www.baeldung.com/json-pointer) to the `AWS_KINESIS_PARTITIONING_STRATEGY` environment variable. M2K will extract the JSON key and use it as the partitioning key.

**Used only if your MQTT payloads are JSON-formatted.** If you have a mixed-payload system where some payloads use JSON and some don't, this strategy will be used for the JSON payloads, `PAYLOAD_HASH` for the others, and `MQTT_TOPIC` for messages with no payload.

<img src="img/kinesis-arch.png" alt="logo" style="max-width: 600px"/>
Image Credit to Amazon AWS
