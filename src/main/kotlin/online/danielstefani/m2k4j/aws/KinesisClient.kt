package online.danielstefani.m2k4j.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.model.PutRecordsRequest
import com.amazonaws.services.kinesis.model.PutRecordsResult
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsyncClient
import com.amazonaws.services.kinesis.M2KKinesis
import com.amazonaws.services.kinesisfirehose.M2KFirehose
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class KinesisClient(
    private val kinesisConfig: KinesisConfig
) {
    private val credentialsProvider: AWSCredentialsProvider? =
        if (kinesisConfig.awsAccessKey?.isNotBlank() == true &&
            kinesisConfig.awsSecretAccessKey?.isNotBlank() == true)
            object : AWSCredentialsProvider {
                val credentials = BasicAWSCredentials(
                    kinesisConfig.awsAccessKey,
                    kinesisConfig.awsSecretAccessKey
                )

                override fun getCredentials(): AWSCredentials {
                    return credentials
                }

                override fun refresh() {}
            }
        else null


    private val kinesisClient: KinesisOrFirehose =
        if (kinesisConfig.shouldSendToFirehose())
            M2KFirehose(
                AmazonKinesisFirehoseAsyncClient.builder()
                    .also { if (credentialsProvider != null) it.withCredentials(credentialsProvider) }
                    .withRegion(Regions.fromName(kinesisConfig.awsRegion!!)),
                kinesisConfig
            )
        else
            M2KKinesis(
                AmazonKinesisAsyncClient.builder()
                    .also { if (credentialsProvider != null) it.withCredentials(credentialsProvider) }
                    .withRegion(Regions.fromName(kinesisConfig.awsRegion!!))
            )

    fun putRecords(req: PutRecordsRequest): Mono<Pair<PutRecordsRequest, PutRecordsResult>> {
        return kinesisClient
            .putRecords(req)
            .toMono()
            .map { res -> Pair(req, res) }
    }
}