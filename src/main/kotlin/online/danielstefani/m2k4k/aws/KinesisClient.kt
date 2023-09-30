package online.danielstefani.m2k4k.aws

import com.fasterxml.jackson.databind.ObjectMapper
import online.danielstefani.m2k4k.dto.Mqtt5Message
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class KinesisClient(
    private val kinesisConfig: KinesisConfig
) {
    companion object {
        private val objectMapper = ObjectMapper()
        private val logger = LoggerFactory.getLogger(KinesisClient::class.java)

        // Arbitrary buffer size, could set as configurable on .env later
        private const val MAX_SAVE_BUFFER_SIZE = 3000
        private const val MAX_KINESIS_PUT_REQUEST_LENGTH = 500 // Defined by AWS
    }

    private val kinesisClient: KinesisAsyncClient =
        KinesisAsyncClient.builder() // Needs credentials file in .aws!
            .also {
                if (kinesisConfig.awsAccessKey?.isNotBlank() == true &&
                    kinesisConfig.awsSecretAccessKey?.isNotBlank() == true) {
                    it.credentialsProvider {
                        AwsBasicCredentials.create(kinesisConfig.awsAccessKey, kinesisConfig.awsSecretAccessKey)
                    }
                }
            }
            .region(Region.of(kinesisConfig.awsRegion))
            .build()

    fun pushMessagesToKinesis(messages: List<Mqtt5Message>): Flux<PutRecordsResponse> {
        return Flux.fromIterable(messages)
            .onBackpressureBuffer(MAX_SAVE_BUFFER_SIZE)
            .window(MAX_KINESIS_PUT_REQUEST_LENGTH) // Flux<Flux<Tuple<Topic, Message>>> (window of 500)
            .flatMap { it.toPutRecordsRequest() } // Flux<PutRecordsRequest>
            .flatMap { kinesisClient.putRecords(it).toMono() }
            .doOnError { logger.error("[kinesis->client] // " +
                    "Failed to send messages to Kinesis stream ${kinesisConfig.kinesisStreamArn}") }
            .doOnComplete { logger.info("[kinesis->client] //" +
                    "Sent ${messages.size} message bundles to Kinesis stream ${kinesisConfig.kinesisStreamArn}") }
    }

    fun Flux<Mqtt5Message>.toPutRecordsRequest(): Mono<PutRecordsRequest> {
        return this.map {
                PutRecordsRequestEntry.builder()
                    .partitionKey(it.topic.toString()) // Use serial for partition key
                    .data(SdkBytes.fromByteArray(objectMapper.writeValueAsBytes(it)))
                    .build()
            }
            .collectList()
            .map {
                PutRecordsRequest.builder().streamARN(kinesisConfig.kinesisStreamArn).records(it).build()
            }
    }
}