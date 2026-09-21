package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelayStageServiceApp {

    public record DelayRequest(Integer delayStage) {}
    public record DelayResponse(String hubId, int delayStage) {}

    private static final Map<String, Integer> delayStages = new ConcurrentHashMap<>();

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
            ctx.json(new DelayResponse(hubId, stage));
        });
    }
}
