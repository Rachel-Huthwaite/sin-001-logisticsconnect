package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HubServiceApp {

    public record HubRecord(String hubId, String province, String sortingCenter, Boolean active) {}

    private static final Map<String, HubRecord> hubDatabase = new ConcurrentHashMap<>();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7051);

        loadHubsFromIngestion();

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs", ctx -> ctx.json(hubDatabase.values()));

        app.get("/hubs/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").trim().toUpperCase();
            HubRecord hub = hubDatabase.get(hubId);

            if (hub != null) {
                ctx.json(hub);
            } else {
                ctx.status(404).result("Hub not found: " + hubId);
            }
        });
    }

    private static void loadHubsFromIngestion() {
        System.out.println("Fetching data from Ingestion Service...");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7050/hubs"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<HubRecord> hubs = objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<HubRecord>>() {}
                );

                hubDatabase.clear();
                for (HubRecord hub : hubs) {
                    if (hub.hubId() != null) {
                        hubDatabase.put(hub.hubId().trim().toUpperCase(), hub);
                    }
                }
                System.out.println("Loaded " + hubDatabase.size() + " hubs into HubService.");
            } else {
                System.err.println("Failed to fetch hubs. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Error connecting to IngestionService: " + e.getMessage());
        }
    }
}
