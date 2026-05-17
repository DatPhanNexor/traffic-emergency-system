package com.example.suco.service.suco.baocao.system.location;

import com.example.suco.util.GeocodingUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {

    @Autowired
    private GeocodingUtil geocodingUtil;

    public String getAddress(Double viDo, Double kinhDo) {

        try {

            var addrMap = geocodingUtil.getAddressFromCoordinates(
                    viDo,
                    kinhDo
            );

            return geocodingUtil.formatAddress(addrMap);

        } catch (Exception e) {

            return "Tọa độ: " + viDo + ", " + kinhDo;
        }
    }
}