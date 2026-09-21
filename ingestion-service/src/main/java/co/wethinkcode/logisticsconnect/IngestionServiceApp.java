package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IngestionServiceApp {

    //Domain record to represent a cleaned Hub entity
    public record HubRecord (String hubId, String province, String sortingCenter, Boolean active) {}

    public static void main(String[] args) {

        //Read raw records on startup
        List<HubRecord> rawHubs = loadRawHubs ();

        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        app.get("/hubs", ctx -> ctx.json(rawHubs));
    }

    private static List<HubRecord> loadRawHubs () {
        List<HubRecord> records = new ArrayList<>();

        // Load CSV from src/main/resources via ClassLoader
        InputStream is = IngestionServiceApp.class.getClassLoader().getResourceAsStream("hubs-global.csv");
        if (is == null) {
            System.err.println("Error: hubs-global.csv not found in resources!");
            return Collections.emptyList();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                // Ignore empty lines
                if (line.isBlank()) continue;

                // Skip header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Split line by comma (-1 keeps trailing empty fields)
                String[] parts = line.split(",", -1);
                if (parts.length < 4) continue;

                // Extract raw string values without cleaning yet
                String rawHubId = parts[0];
                String rawProvince = parts[1];
                String rawSortingCenter = parts[2];
                String rawActive = parts[3];

                // Create record with raw strings (leaving boolean as null for now)
                records.add(new HubRecord(rawHubId, rawProvince, rawSortingCenter, null));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }
}


