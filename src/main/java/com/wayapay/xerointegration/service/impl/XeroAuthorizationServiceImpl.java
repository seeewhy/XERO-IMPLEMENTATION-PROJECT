package com.wayapay.xerointegration.service.impl;

import com.google.gson.Gson;
import com.wayapay.xerointegration.constant.Item;
import com.wayapay.xerointegration.dto.xero.response.*;
import com.wayapay.xerointegration.service.GenericService;
import com.wayapay.xerointegration.service.XeroAuthorizationService;
import com.wayapay.xerointegration.storage.ZooItemKeeper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class XeroAuthorizationServiceImpl implements XeroAuthorizationService
{
    @Value("${xero.grant-type}")
    private String xeroGrantType;

    @Value("${xero.scope}")
    private String xeroScope;
    @Value("${xero.client-id}")
    private String xeroClientId;
    @Value("${xero.client-secret}")
    private String xeroClientSecret;
    @Value("${xero.token-url}")
    private String xeroTokenUrl;

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private GenericService genericService;

    private static final Gson JSON = new Gson();

    @Override
    public String getXeroAccessToken(){

        AccessTokenResponseDTO responseDTO = ZooItemKeeper.getItem(Item.XERO_AUTH_TOKEN);
        LocalDateTime exp = LocalDateTime.parse(responseDTO.getSafeExpirationDateString());
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(exp) || now.isEqual(exp)){
            return exchangeWithXeroForAccessToken();
        }
        return responseDTO.getAccessToken();
    }


    private String exchangeWithXeroForAccessToken(){
        String credentialsConcat = String.join(":", xeroClientId, xeroClientSecret);
        String basicAuthValue = Base64.getEncoder().encodeToString(credentialsConcat.getBytes(StandardCharsets.UTF_8));
        String basicAuthHeaderValue = "Basic ".concat(basicAuthValue);

        Map<String, String> header = new HashMap<>();
        header.put("Authorization", basicAuthHeaderValue);

        Map<String, Object> fields = new HashMap<>();
        fields.put("grant_type", xeroGrantType);
        fields.put("scope", xeroScope);

        String responseJsonFormPost = genericService.postForForm(xeroTokenUrl, fields, header, null);
        AccessTokenResponseDTO responseDTO = JSON.fromJson(responseJsonFormPost, AccessTokenResponseDTO.class);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expirationDateTime = now.plusSeconds(responseDTO.getExpiresIn());
        LocalDateTime safeExpirationDateTime = expirationDateTime.minusMinutes(5);

        responseDTO.setExpirationDateString(expirationDateTime.toString());
        responseDTO.setSafeExpirationDateString(safeExpirationDateTime.toString());
        ZooItemKeeper.saveItem(Item.XERO_AUTH_TOKEN, responseDTO);
        log.info("Xero fresh access token saved to ZooItemKeeper");
        log.info("Zoo keeper -------->>>> {}", ZooItemKeeper.memoryStorage);

        return responseDTO.getAccessToken();
    }

}
