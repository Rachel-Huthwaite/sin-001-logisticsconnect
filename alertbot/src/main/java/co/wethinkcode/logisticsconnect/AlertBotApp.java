package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class AlertBotApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7054);

        app.get("/health", ctx -> ctx.result("OK"));

        startAlertBotSubscriber();
    }

    private static void startAlertBotSubscriber() {
        new Thread(() -> {
            try {
                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
                Connection connection = connectionFactory.createConnection();
                connection.start();

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic destination = session.createTopic(MqConfig.TOPIC);
                MessageConsumer consumer = session.createConsumer(destination);

                System.out.println(">>> [ALERT BOT] Subscribed to topic: " + MqConfig.TOPIC);

                consumer.setMessageListener(message -> {
                    if (message instanceof TextMessage textMessage) {
                        try {
                            System.out.println("==========================================");
                            System.out.println("🚨 [ALERT BOT BROADCAST] 🚨");
                            System.out.println("Payload: " + textMessage.getText());
                            System.out.println("==========================================");
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println(">>> [ALERT BOT ERROR] Failed to connect to ActiveMQ: " + e.getMessage());
            }
        }).start();
    }
}
