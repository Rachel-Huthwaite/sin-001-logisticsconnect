package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelayStageServiceApp {

    public record DelayRequest(Integer delayStage) {}
    public record DelayResponse(String hubId, int delayStage) {}
    public record StatusEvent(String hubId, int delayStage, String timestamp) {}

    private static final Map<String, Integer> delayStages = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7052);

        app.get("/health", ctx -> ctx.result("OK"));

        // GET /delay-stage/{hubId} -> defaults to 0
        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();
            int stage = delayStages.getOrDefault(hubId, 0);
            ctx.json(new DelayResponse(hubId, stage));
        });

        // POST /delay-stage/{hubId} -> set delay stage (0 to 8)
        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();
            DelayRequest body = ctx.bodyAsClass(DelayRequest.class);

            if (body == null || body.delayStage() == null) {
                ctx.status(400).result("Invalid request body. Expected: {\"delayStage\": <int>}");
                return;
            }

            int stage = body.delayStage();
            if (stage < 0 || stage > 8) {
                ctx.status(400).result("Delay stage must be between 0 and 8.");
                return;
            }

            delayStages.put(hubId, stage);
            // Trigger MQ Event if delay stage is 3 or higher
            if (stage >= 3) {
                publishDelayAlert(hubId, stage);
            }

            ctx.json(new DelayResponse(hubId, stage));
        });
    }

    private static void publishDelayAlert(String hubId, int stage) {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic destination = session.createTopic(MqConfig.TOPIC);
            MessageProducer producer = session.createProducer(destination);

            StatusEvent event = new StatusEvent(hubId, stage, String.valueOf(System.currentTimeMillis()));
            String jsonPayload = objectMapper.writeValueAsString(event);

            TextMessage message = session.createTextMessage(jsonPayload);
            producer.send(message);

            System.out.println(">>> [MQ PRODUCER] Published delay alert for " + hubId + " (Stage " + stage + ")");

            connection.close();
        } catch (Exception e) {
            System.err.println(">>> [MQ ERROR] Failed to send message to broker: " + e.getMessage());
        }
    }
}
