package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.util.List;

public class IngestionServiceApp {

    //Domain record to represent a cleaned Hub entity
    public record HubRecord (String hubId, String province, String sortingCenter, Boolean active) {}

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        app.get("/hubs", ctx -> ctx.json(List.of(
                new HubRecord("H-500", "Gauteng", "Johannesburg Central", true)
        )));
    }
}
