package org.example.microgrid.registry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class MeterRegistry
{
    private final Map<String, String> meterToLan = new HashMap<>();

    /**
     * @param resourceName The name of the file in src/main/resources (e.g., "meters.csv")
     */
    public MeterRegistry(String resourceName)
    {
        loadDataFromResources(resourceName);
    }

    private void loadDataFromResources(String fileName)
    {
        // getResourceAsStream looks inside src/main/resources automatically
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName))
        {
            if (is == null)
            {
                System.err.println("Critical Error: GIS file '" + fileName + "' not found in src/main/resources!");
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is)))
            {
                String line;
                int count = 0;
                while ((line = br.readLine()) != null)
                {
                    // Skip empty lines or malformed data
                    String[] parts = line.split(",");
                    if (parts.length >= 2)
                    {
                        String meterId = parts[0].trim();
                        String lanId = parts[1].trim();
                        meterToLan.put(meterId, lanId);
                        count++;
                    }
                }
                System.out.println("GIS Registry initialized. Loaded " + count + " meter-to-LAN mappings.");
            }
        } catch (Exception e)
        {
            System.err.println("Failed to load GIS data: " + e.getMessage());
        }
    }

    public void register(String meterId, String lanId)
    {
        meterToLan.put(meterId, lanId);
    }

    public String getLanId(String meterId)
    {
        return meterToLan.get(meterId);
    }
}