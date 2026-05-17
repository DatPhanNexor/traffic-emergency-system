package com.example.suco.util;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GeocodingUtil {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Chuyển đổi kinh độ, vĩ độ thành địa chỉ chi tiết (quận, huyện, đường)
     * @param latitude vĩ độ
     * @param longitude kinh độ
     * @return Map chứa các thông tin địa chỉ
     */
    public Map<String, String> getAddressFromCoordinates(Double latitude, Double longitude) {
        Map<String, String> addressMap = new HashMap<>();
        
        try {
            String url = String.format("%s?format=json&lat=%f&lon=%f&language=vi&zoom=18&addressdetails=1",
                    NOMINATIM_URL, latitude, longitude);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new java.net.URI(url))
                    .header("User-Agent", "SuCo-Map-App")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode address = rootNode.get("address");
                
                if (address != null) {
                    // Lấy thông tin địa chỉ chi tiết
                    addressMap.put("tenDuong", getJsonValue(address, "road"));
                    addressMap.put("quan", getJsonValue(address, "suburb")); // quận/huyện
                    addressMap.put("huyenHoac", getJsonValue(address, "county")); // huyện
                    addressMap.put("thanhPho", getJsonValue(address, "city")); // thành phố
                    addressMap.put("tinh", getJsonValue(address, "state")); // tỉnh
                    addressMap.put("soNha", getJsonValue(address, "house_number")); // số nhà
                    addressMap.put("addressFull", rootNode.get("display_name").asText()); // địa chỉ đầy đủ
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return addressMap;
    }

    /**
     * Format địa chỉ thành chuỗi ngắn gọn
     * @param addressMap Map chứa thông tin địa chỉ
     * @return Chuỗi địa chỉ formatted
     */
    public String formatAddress(Map<String, String> addressMap) {
        StringBuilder address = new StringBuilder();
        
        if (addressMap.containsKey("soNha") && !addressMap.get("soNha").isEmpty()) {
            address.append(addressMap.get("soNha")).append(" ");
        }
        
        if (addressMap.containsKey("tenDuong") && !addressMap.get("tenDuong").isEmpty()) {
            address.append(addressMap.get("tenDuong")).append(", ");
        }
        
        if (addressMap.containsKey("quan") && !addressMap.get("quan").isEmpty()) {
            address.append(addressMap.get("quan")).append(", ");
        } else if (addressMap.containsKey("huyenHoac") && !addressMap.get("huyenHoac").isEmpty()) {
            address.append(addressMap.get("huyenHoac")).append(", ");
        }
        
        if (addressMap.containsKey("thanhPho") && !addressMap.get("thanhPho").isEmpty()) {
            address.append(addressMap.get("thanhPho"));
        }
        
        return address.toString().replaceAll(", $", "");
    }

    private String getJsonValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : "";
    }
}
