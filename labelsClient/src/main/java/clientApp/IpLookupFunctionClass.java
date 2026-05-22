package clientApp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IpLookupFunctionClass {
    private static final String ipLookupFunctionURL = "https://europe-west1-cn2526-t3-g12.cloudfunctions.net/ip-lookup";

    public static List<String> getExternalIps() throws IOException {
        URL url = new URL(ipLookupFunctionURL);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int statusCode = connection.getResponseCode();

        if(statusCode != 200){
            throw new RuntimeException("Erro na conexão à função IP Lookup");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String line;

        while((line = br.readLine()) != null)
            response.append(line);

        br.close();
        connection.disconnect();

        return parseExternalIps(response.toString());
    }

    public static List<String> parseExternalIps(String response){
        Gson gson = new Gson();

        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> result = gson.fromJson(response, type); //converter o json em Map<String, Object>

        List<String> ips = new ArrayList<>();

        List<Map<String, Object>> instances = (List<Map<String, Object>>) result.get("instances");

        if(instances == null)
            return ips;

        for(Map<String, Object> instance : instances){
            String externalIp = instance.get("externalIp").toString();

            if(externalIp != null && !(externalIp.equals("N/A")))
                ips.add(externalIp);
        }

        return ips;
    }
}