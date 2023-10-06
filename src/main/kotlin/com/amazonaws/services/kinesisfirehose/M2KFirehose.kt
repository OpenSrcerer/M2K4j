package com.amazonaws.services.kinesisfirehose

import com.amazonaws.client.M2KFirehoseAsyncClientParams
import com.amazonaws.services.kinesis.model.*
import com.amazonaws.services.kinesis.waiters.AmazonKinesisWaiters
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest
import com.amazonaws.services.kinesisfirehose.model.Record
import online.danielstefani.m2k4j.aws.KinesisConfig
import online.danielstefani.m2k4j.aws.KinesisOrFirehose
import java.nio.ByteBuffer

class M2KFirehose(
    builder: AmazonKinesisFirehoseClientBuilder,
    private val kinesisConfig: KinesisConfig
) : AmazonKinesisFirehoseAsyncClient(M2KFirehoseAsyncClientParams(builder)), KinesisOrFirehose {
    override fun putRecords(putRecordsRequest: PutRecordsRequest?): PutRecordsResult {
        val result = super.putRecordBatch(
            PutRecordBatchRequest()
                .withRecords(putRecordsRequest!!.records.map {
                    Record().withData(it.data)
                })
                .withDeliveryStreamName(kinesisConfig.kinesisStreamName)
        )
        return PutRecordsResult()
            .withRecords(result.requestResponses.map {
                PutRecordsResultEntry()
                    .withShardId(null)
                    .withSequenceNumber(it.recordId)
                    .withErrorCode(it.errorCode)
                    .withErrorMessage(it.errorMessage)
            })
    }

    override fun addTagsToStream(addTagsToStreamRequest: AddTagsToStreamRequest?): AddTagsToStreamResult {
        TODO("Not yet implemented")
    }

    override fun createStream(createStreamRequest: CreateStreamRequest?): CreateStreamResult {
        TODO("Not yet implemented")
    }

    override fun createStream(streamName: String?, shardCount: Int?): CreateStreamResult {
        TODO("Not yet implemented")
    }

    override fun decreaseStreamRetentionPeriod(decreaseStreamRetentionPeriodRequest: DecreaseStreamRetentionPeriodRequest?): DecreaseStreamRetentionPeriodResult {
        TODO("Not yet implemented")
    }

    override fun deleteStream(deleteStreamRequest: DeleteStreamRequest?): DeleteStreamResult {
        TODO("Not yet implemented")
    }

    override fun deleteStream(streamName: String?): DeleteStreamResult {
        TODO("Not yet implemented")
    }

    override fun deregisterStreamConsumer(deregisterStreamConsumerRequest: DeregisterStreamConsumerRequest?): DeregisterStreamConsumerResult {
        TODO("Not yet implemented")
    }

    override fun describeLimits(describeLimitsRequest: DescribeLimitsRequest?): DescribeLimitsResult {
        TODO("Not yet implemented")
    }

    override fun describeStream(describeStreamRequest: DescribeStreamRequest?): DescribeStreamResult {
        TODO("Not yet implemented")
    }

    override fun describeStream(streamName: String?): DescribeStreamResult {
        TODO("Not yet implemented")
    }

    override fun describeStream(streamName: String?, exclusiveStartShardId: String?): DescribeStreamResult {
        TODO("Not yet implemented")
    }

    override fun describeStream(
        streamName: String?,
        limit: Int?,
        exclusiveStartShardId: String?
    ): DescribeStreamResult {
        TODO("Not yet implemented")
    }

    override fun describeStreamConsumer(describeStreamConsumerRequest: DescribeStreamConsumerRequest?): DescribeStreamConsumerResult {
        TODO("Not yet implemented")
    }

    override fun describeStreamSummary(describeStreamSummaryRequest: DescribeStreamSummaryRequest?): DescribeStreamSummaryResult {
        TODO("Not yet implemented")
    }

    override fun disableEnhancedMonitoring(disableEnhancedMonitoringRequest: DisableEnhancedMonitoringRequest?): DisableEnhancedMonitoringResult {
        TODO("Not yet implemented")
    }

    override fun enableEnhancedMonitoring(enableEnhancedMonitoringRequest: EnableEnhancedMonitoringRequest?): EnableEnhancedMonitoringResult {
        TODO("Not yet implemented")
    }

    override fun getRecords(getRecordsRequest: GetRecordsRequest?): GetRecordsResult {
        TODO("Not yet implemented")
    }

    override fun getShardIterator(getShardIteratorRequest: GetShardIteratorRequest?): GetShardIteratorResult {
        TODO("Not yet implemented")
    }

    override fun getShardIterator(
        streamName: String?,
        shardId: String?,
        shardIteratorType: String?
    ): GetShardIteratorResult {
        TODO("Not yet implemented")
    }

    override fun getShardIterator(
        streamName: String?,
        shardId: String?,
        shardIteratorType: String?,
        startingSequenceNumber: String?
    ): GetShardIteratorResult {
        TODO("Not yet implemented")
    }

    override fun increaseStreamRetentionPeriod(increaseStreamRetentionPeriodRequest: IncreaseStreamRetentionPeriodRequest?): IncreaseStreamRetentionPeriodResult {
        TODO("Not yet implemented")
    }

    override fun listShards(listShardsRequest: ListShardsRequest?): ListShardsResult {
        TODO("Not yet implemented")
    }

    override fun listStreamConsumers(listStreamConsumersRequest: ListStreamConsumersRequest?): ListStreamConsumersResult {
        TODO("Not yet implemented")
    }

    override fun listStreams(listStreamsRequest: ListStreamsRequest?): ListStreamsResult {
        TODO("Not yet implemented")
    }

    override fun listStreams(): ListStreamsResult {
        TODO("Not yet implemented")
    }

    override fun listStreams(exclusiveStartStreamName: String?): ListStreamsResult {
        TODO("Not yet implemented")
    }

    override fun listStreams(limit: Int?, exclusiveStartStreamName: String?): ListStreamsResult {
        TODO("Not yet implemented")
    }

    override fun listTagsForStream(listTagsForStreamRequest: ListTagsForStreamRequest?): ListTagsForStreamResult {
        TODO("Not yet implemented")
    }

    override fun mergeShards(mergeShardsRequest: MergeShardsRequest?): MergeShardsResult {
        TODO("Not yet implemented")
    }

    override fun mergeShards(
        streamName: String?,
        shardToMerge: String?,
        adjacentShardToMerge: String?
    ): MergeShardsResult {
        TODO("Not yet implemented")
    }

    override fun putRecord(putRecordRequest: PutRecordRequest?): PutRecordResult {
        TODO("Not yet implemented")
    }

    override fun putRecord(streamName: String?, data: ByteBuffer?, partitionKey: String?): PutRecordResult {
        TODO("Not yet implemented")
    }

    override fun putRecord(
        streamName: String?,
        data: ByteBuffer?,
        partitionKey: String?,
        sequenceNumberForOrdering: String?
    ): PutRecordResult {
        TODO("Not yet implemented")
    }

    override fun registerStreamConsumer(registerStreamConsumerRequest: RegisterStreamConsumerRequest?): RegisterStreamConsumerResult {
        TODO("Not yet implemented")
    }

    override fun removeTagsFromStream(removeTagsFromStreamRequest: RemoveTagsFromStreamRequest?): RemoveTagsFromStreamResult {
        TODO("Not yet implemented")
    }

    override fun splitShard(splitShardRequest: SplitShardRequest?): SplitShardResult {
        TODO("Not yet implemented")
    }

    override fun splitShard(streamName: String?, shardToSplit: String?, newStartingHashKey: String?): SplitShardResult {
        TODO("Not yet implemented")
    }

    override fun startStreamEncryption(startStreamEncryptionRequest: StartStreamEncryptionRequest?): StartStreamEncryptionResult {
        TODO("Not yet implemented")
    }

    override fun stopStreamEncryption(stopStreamEncryptionRequest: StopStreamEncryptionRequest?): StopStreamEncryptionResult {
        TODO("Not yet implemented")
    }

    override fun updateShardCount(updateShardCountRequest: UpdateShardCountRequest?): UpdateShardCountResult {
        TODO("Not yet implemented")
    }

    override fun updateStreamMode(updateStreamModeRequest: UpdateStreamModeRequest?): UpdateStreamModeResult {
        TODO("Not yet implemented")
    }

    override fun waiters(): AmazonKinesisWaiters {
        TODO("Not yet implemented")
    }
}