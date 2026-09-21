package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IngestionServiceApp {

    //Domain record to represent a cleaned Hub entity
    public record HubRecord (String hubId, String province, String sortingCenter, Boolean active) {}

    public static void main(String[] args) {

        //Read raw records on startup
        List<HubRecord> cleanedHubs = loadAndCleanHubs ();

        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        app.get("/hubs", ctx -> ctx.json(cleanedHubs));
    }

    private static List<HubRecord> loadAndCleanHubs () {
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

                // Applied text cleaning rules

                // 1. Hub ID: Trim and force Uppercase
                String hubId = cleanString(parts[0]).toUpperCase();

                // 2. Province: Clean, apply title case, and fix spelling variants
                String province = normalizeProvince(parts[1]);

                // 3. Sorting Center: Clean and apply title case (e.g. "johannesburg central" -> "Johannesburg Central")
                String sortingCenter = capitalizeWords(cleanString(parts[2]));

                Boolean active = parseBoolean(cleanString(parts[3]));

                records.add(new HubRecord(hubId, province, sortingCenter, active));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }

    //Normalize booleans to return true, false or null
    private static Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) return null;

        String val = value.toLowerCase();
        if (Set.of("y", "yes", "1", "true").contains(val)){
            return true;
        } else if (Set.of("n","no","0","false").contains(val)) {
            return false;
        }

        //returns null for unknown or N/A or anything else unspecified
        return null;
    }

    //Removes leading/trailing spaces and collapses internal double spaces
    private static String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    //Standardize spelling variants and apply Title Case
    private static String normalizeProvince (String raw) {
        String cleaned = capitalizeWords(cleanString(raw));
        if (cleaned.isBlank()) return "";

        //Standardise KwaZulu-Natal variations
        if (cleaned.equalsIgnoreCase("Kwa-Zulu Natal") || cleaned.equalsIgnoreCase("KwaZulu Natal")) {
            return "KwaZulu-Natal";
        }
        return cleaned;
    }

    //Capitalize the first letter of each word
    private static String capitalizeWords (String input) {

        if (input == null || input.isBlank()) return input;

        String[] words = input.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();

        for (String word: words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");

            }
        }
        return sb.toString().trim();
    }
}


