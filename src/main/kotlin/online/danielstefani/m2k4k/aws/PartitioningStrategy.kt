package online.danielstefani.m2k4k.aws

enum class PartitioningStrategy {
    /**
     * Passes the MD5 hash of the payload to the partition key of the Kinesis request entry.
     */
    PAYLOAD_HASH,

    /**
     * Passes the topic of the message to the partition key of the Kinesis request entry.
     */
    MQTT_TOPIC,

    /**
     * Passes a custom JSON key of the payload to the partition key of the Kinesis request entry.
     * In case the payload isn't JSON or key isn't found, reverts to PAYLOAD_HASH strategy.
     * Uses a JSON Pointer Expression.
     */
    JSON_KEY;

    companion object {
        private var computedStrategy: Pair<PartitioningStrategy, String?>? = null

        fun getComputedStrategy(strategy: String): Pair<PartitioningStrategy, String?> {
            if (computedStrategy == null)
                computeStrategy(strategy)
            return computedStrategy!!
        }

        private fun computeStrategy(strategy: String) {
            computedStrategy =
                try {
                    val partitioningStrategy = valueOf(strategy) // ONLY accepts PAYLOAD_HASH or MQTT_TOPIC
                    if (partitioningStrategy == JSON_KEY) throw IllegalArgumentException()
                    Pair(partitioningStrategy, null)
                } catch (ex: IllegalArgumentException) {
                    Pair(JSON_KEY, strategy) // uses strategy as JSON_KEY
                }
        }
    }
}