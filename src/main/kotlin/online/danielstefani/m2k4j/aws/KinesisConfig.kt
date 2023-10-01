package online.danielstefani.m2k4j.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.integration.config.EnableIntegration

@Configuration
@EnableIntegration
class KinesisConfig {

    @Value("\${aws.region}")
    var awsRegion: String? = null

    @Value("\${aws.kinesis.partitioning-strategy}")
    var kinesisPartitioningStrategy: String? = null

    @Value("\${aws.kinesis.stream.arn}")
    var kinesisStreamArn: String? = null

    @Value("\${aws.access.key}")
    var awsAccessKey: String? = null

    @Value("\${aws.access.key.secret}")
    var awsSecretAccessKey: String? = null
}