package com.amazonaws.client

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.handlers.RequestHandler2
import com.amazonaws.metrics.RequestMetricCollector
import com.amazonaws.monitoring.CsmConfigurationProvider
import com.amazonaws.monitoring.MonitoringListener
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class M2KKinesisAsyncClientParams(
    private val clientBuilder: AmazonKinesisClientBuilder
) : AwsAsyncClientParams() {
    override fun getCredentialsProvider(): AWSCredentialsProvider {
        return clientBuilder.credentials
    }

    override fun getClientConfiguration(): ClientConfiguration {
        return clientBuilder.clientConfiguration
    }

    override fun getRequestMetricCollector(): RequestMetricCollector {
        return clientBuilder.metricsCollector
    }

    override fun getRequestHandlers(): MutableList<RequestHandler2> {
        return clientBuilder.requestHandlers
    }

    override fun getClientSideMonitoringConfigurationProvider(): CsmConfigurationProvider {
        return clientBuilder.clientSideMonitoringConfigurationProvider
    }

    override fun getMonitoringListener(): MonitoringListener {
        return clientBuilder.monitoringListener
    }

    override fun getExecutor(): ExecutorService {
        return Executors.newCachedThreadPool()
    }
}