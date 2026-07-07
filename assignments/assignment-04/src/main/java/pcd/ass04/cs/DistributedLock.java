package pcd.ass04.cs;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class DistributedLock implements AutoCloseable {

    private final String processId;
    private final LockQueues queues;
    private final Connection connection;
    private final Channel channel;
    private final String replyQueue;

    public DistributedLock(RabbitConfig config, String lockName, String processId) throws Exception {
        this.processId = processId;
        this.queues = LockQueues.of(lockName);
        this.connection = RabbitMq.connect(config);
        this.channel = connection.createChannel();
        RabbitMq.declare(channel, queues);
        this.replyQueue = channel.queueDeclare().getQueue();
    }

    public boolean acquire(Duration timeout) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        BlockingQueue<String> grants = new ArrayBlockingQueue<>(1);
        String consumerTag = channel.basicConsume(replyQueue, true, (tag, delivery) -> {
            String grantedId = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String deliveryCorrelation = delivery.getProperties().getCorrelationId();
            if (processId.equals(grantedId) && correlationId.equals(deliveryCorrelation)) {
                grants.offer(grantedId);
            }
        }, tag -> { });

        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
            .replyTo(replyQueue)
            .correlationId(correlationId)
            .build();
        channel.basicPublish("", queues.requestQueue(), props, processId.getBytes(StandardCharsets.UTF_8));
        try {
            return grants.poll(timeout.toMillis(), TimeUnit.MILLISECONDS) != null;
        } finally {
            channel.basicCancel(consumerTag);
        }
    }

    public void release() throws Exception {
        channel.basicPublish("", queues.releaseQueue(), null, processId.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception {
        channel.close();
        connection.close();
    }
}
