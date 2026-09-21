package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TransitServiceApp {

    public record HubRecord(String hubId, String province, String sortingCenter, Boolean active) {}
    public record DelayResponse(String hubId, int delayStage) {}
    public record EtaResponse(String hubId, String province, String sortingCenter, int delayStage, int estimatedHours) {}

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        // GET /transit/eta/{hubId}
        app.get("/transit/eta/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();

            // 1. Fetch Hub Details from HubService (7051)
            HubRecord hub = fetchHub(hubId);
            if (hub == null) {
                ctx.status(404).result("Hub not found in HubService: " + hubId);
                return;
            }

            // 2. Fetch Delay Stage from DelayStageService (7052)
            int delayStage = fetchDelayStage(hubId);

            // 3. Calculate ETA hours: 24h base + (12h * delayStage)
            int estimatedHours = 24 + (delayStage * 12);

            ctx.json(new EtaResponse(
                    hub.hubId(),
                    hub.province(),
                    hub.sortingCenter(),
                    delayStage,
                    estimatedHours
            ));
        });
    }

    private static HubRecord fetchHub(String hubId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7051/hubs/" + hubId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), HubRecord.class);
            }
        } catch (Exception e) {
            System.err.println("Error calling HubService: " + e.getMessage());
        }
        return null;
    }

    private static int fetchDelayStage(String hubId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7052/delay-stage/" + hubId))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                DelayResponse delay = objectMapper.readValue(response.body(), DelayResponse.class);
                return delay.delayStage();
            }
        } catch (Exception e) {
            System.err.println("Error calling DelayStageService: " + e.getMessage());
        }
        return 0; // Default fallback to 0 delay
    }
}
