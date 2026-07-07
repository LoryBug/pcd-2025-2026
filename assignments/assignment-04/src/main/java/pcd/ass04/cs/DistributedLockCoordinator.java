package pcd.ass04.cs;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public final class DistributedLockCoordinator implements AutoCloseable {

    private final Connection connection;
    private final Channel channel;
    private final CoordinatorState state = new CoordinatorState();
    private final CountDownLatch started = new CountDownLatch(2);
    private final String requestConsumer;
    private final String releaseConsumer;

    public DistributedLockCoordinator(RabbitConfig config, String lockName, boolean resetQueues) throws Exception {
        LockQueues queues = LockQueues.of(lockName);
        this.connection = RabbitMq.connect(config);
        this.channel = connection.createChannel();
        RabbitMq.declare(channel, queues);
        if (resetQueues) {
            channel.queuePurge(queues.requestQueue());
            channel.queuePurge(queues.releaseQueue());
        }
        this.requestConsumer = channel.basicConsume(queues.requestQueue(), true, onRequest(), tag -> { });
        started.countDown();
        this.releaseConsumer = channel.basicConsume(queues.releaseQueue(), true, onRelease(), tag -> { });
        started.countDown();
    }

    public void awaitStarted() throws InterruptedException {
        started.await();
    }

    private DeliverCallback onRequest() {
        return (consumerTag, delivery) -> {
            String processId = new String(delivery.getBody(), StandardCharsets.UTF_8);
            AMQP.BasicProperties props = delivery.getProperties();
            Optional<CoordinatorState.Grant> grant = state.onRequest(
                processId, props.getReplyTo(), props.getCorrelationId());
            if (grant.isPresent()) {
                publishGrant(grant.orElseThrow());
            }
        };
    }

    private DeliverCallback onRelease() {
        return (consumerTag, delivery) -> {
            String processId = new String(delivery.getBody(), StandardCharsets.UTF_8);
            Optional<CoordinatorState.Grant> grant = state.onRelease(processId);
            if (grant.isPresent()) {
                publishGrant(grant.orElseThrow());
            }
        };
    }

    private void publishGrant(CoordinatorState.Grant grant) throws IOException {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
            .correlationId(grant.correlationId())
            .build();
        channel.basicPublish("", grant.replyTo(), props, grant.processId().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception {
        try {
            channel.basicCancel(requestConsumer);
            channel.basicCancel(releaseConsumer);
        } finally {
            channel.close();
            connection.close();
        }
    }
}
