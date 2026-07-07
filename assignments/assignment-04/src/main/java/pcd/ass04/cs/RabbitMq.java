package pcd.ass04.cs;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

final class RabbitMq {

    private RabbitMq() {
    }

    static Connection connect(RabbitConfig config) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.host());
        factory.setPort(config.port());
        factory.setUsername(config.username());
        factory.setPassword(config.password());
        factory.setAutomaticRecoveryEnabled(true);
        return factory.newConnection();
    }

    static void declare(Channel channel, LockQueues queues) throws Exception {
        channel.queueDeclare(queues.requestQueue(), false, false, false, null);
        channel.queueDeclare(queues.releaseQueue(), false, false, false, null);
    }
}
