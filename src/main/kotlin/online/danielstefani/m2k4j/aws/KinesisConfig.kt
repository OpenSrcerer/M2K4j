package online.danielstefani.m2k4j.aws

import jakarta.validation.constraints.NotEmpty
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.integration.config.EnableIntegration
import org.springframework.validation.annotation.Validated

@Configuration
@EnableIntegration
@Validated
class KinesisConfig {

    @Value("\${aws.region}")
    var awsRegion: String? = null

    @Value("\${aws.kinesis.partitioning-strategy}")
    var kinesisPartitioningStrategy: String? = null

    @NotEmpty
    @Value("\${aws.kinesis.stream.name}")
    var kinesisStreamName: String? = null

    @Value("\${aws.access.key}")
    var awsAccessKey: String? = null

    @Value("\${aws.access.key.secret}")
    var awsSecretAccessKey: String? = null

    var sendToFirehose: Boolean? = null

    fun shouldSendToFirehose(): Boolean {
        if (sendToFirehose == null) {
            sendToFirehose = kinesisStreamName!!.contains("firehose")
        }
        return sendToFirehose!!
    }
}