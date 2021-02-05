package com.corgi.mailer.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class EmailService {

    private final String space = " ";
    private final String newLine = "\n";
    private final String algorithm = "HmacSHA256";

    // properties로 분리 필요
    private final String accessKey = "accessKey";   // access key id (from portal or Sub Account)
    private final String secretKey = "secretKey";   // secret key (from portal or Sub Account)

    // Thymeleaf mail template 사용 시
//    private final SpringTemplateEngine templateEngine;

    /**
     * 이메일 발송시 옵션
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    @Getter
    private static class Email {
        // 조건부 필수
        private String senderAddress;   // 발송자 이메일 주소 (templateSid가 전달되지 않으면 필수)
        private String title;           // 메일 제목 (templateSid가 전달되지 않으면 필수)
        private String body;            // 메일 본문 (templateSid가 전달되지 않으면 필수, max:500kb)
        private List<RecipientForRequest> recipients;   // 수신자 목록(recipientGroupFilter 값이 입력되지 않으면 필수)
        private String unsubscribeMessage;              // 사용자 정의 수신 거부 문구(useBasicUnsubscribeMsg 값이 false이면 필수)

        // 선택
        private String senderName;              // 발송자 이름(0~69자)
        private Integer templateSid;            // 템플릿 ID
        private Boolean individual;             // 개인 발송 여부(개인 발송 시 참조인, 숨은 참조 무시됨)
        private Boolean confirmAndSend;         // 확인 후 발송 여부
        private Boolean advertising;            // 광고 메일 여부
        private Object parameters = null;       // 치환 파라미터(전체 수신자에게 적용). 치환 ID를 key로, 치환 ID에 매핑되는 값을 value로 가지는 Map 형태의 Object
        private String referencesHeader = "";   // 특정 메일을 모아서 보기 위해 네이버 메일에서 지원하는 기능, 해당 필드에 동일한 값을 입력한 메일들을 모아서 볼 수 있다.
        private Long reserveationUtc;           // 예약 발송 일시(reservationDateTime보다 이 값이 우선 적용된다.)
        private String reservationDateTime;     // 예약 발송 일시(reservationUtc 값이 우선한다.)
        private List<String> attachFields;      // createFile API를 통해 업로드된 파일 ID(첨부파일 ID 목록)
        private Boolean useBasicUnsubscribeMsg; // 광고 메일일 경우 기본 수신 거부 문구 사용 여부
//        private List<RecipientGroupFilter> recipientGroupFilter;    // 수신자 그룹 조합 발송 조건
    }


    /**
     * 수신자 옵션
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    @Getter
    private static class RecipientForRequest {
        // 필수
        private String address;     // 수신자 이메일 주소
        private String type;        // 수신자 유형(R: 수신자, C: 참조인, B: 숨은참조). default: R

        // 선택
        private String name;        // 수신자 이름
        private Object parameters;  // 치환 파라미터(수신자별로 적용). '치환 ID'를 key로, '치환 ID에 맵핑되는 값'을 value로 가지는 Map 형태의 Object
    }

    /**
     * 이메일 발송
     */
    public void createMail() throws Exception {
        HttpHeaders headers = makeHttpHeaders("/api/v1/mails", HttpMethod.POST, "ko-KR");

        List<RecipientForRequest> recipients = Arrays.asList(
                RecipientForRequest.builder().address("wolime2581@jentrix.com").type("R").build());

        Email param = Email.builder()
                .senderAddress("ksw6169@naver.com")
                .title("테스트 제목")
                .body("테스트<br>내용")
                .recipients(recipients)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(param), headers);

        RestTemplate template = new RestTemplate();
        ResponseEntity<String> responseEntity = template.postForEntity("https://mail.apigw.ntruss.com/api/v1/mails", entity, String.class);

        log.debug("responseBody : " + responseEntity.getBody());
        log.debug("responseStatusCode : " + responseEntity.getStatusCode());
    }

    /**
     * 템플릿을 이용한 이메일 발송
     */
    public void createTemplateMail() throws Exception {
        HttpHeaders headers = makeHttpHeaders("/api/v1/mails", HttpMethod.POST, "ko-KR");

        List<RecipientForRequest> recipients = Arrays.asList(
                RecipientForRequest.builder().address("ksw6169@naver.com").type("R").build());

        Integer templateSid = 2734;

        // 치환 태그 값
        Map<String, String> parameters = new HashMap<>();
        parameters.put("ACCTYPE", "BIZTEST");
        parameters.put("SIGNIN", "http://www.naver.com");
        parameters.put("DRIVESERVICENAME", "Space");
        parameters.put("SERVICENAME", "Ma_platform");
        parameters.put("END_DATE", "2021년@");
        parameters.put("CONTACTUS", "테스트CONTACT_US");

        Email param = Email.builder()
                .recipients(recipients)
                .templateSid(templateSid)
                .parameters(parameters)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(param), headers);

        RestTemplate template = new RestTemplate();
        ResponseEntity<String> responseEntity = template.postForEntity("https://mail.apigw.ntruss.com/api/v1/mails", entity, String.class);

        log.debug("responseBody : " + responseEntity.getBody());
        log.debug("responseStatusCode : " + responseEntity.getStatusCode());
    }

    /**
     * 수신인 이메일 주소 삭제
     */
    public void deleteAddress() throws Exception {
        HttpHeaders headers = makeHttpHeaders("/api/v1/address-book/address", HttpMethod.DELETE, "ko-KR");

        Map<String, Object> param = new HashMap<>();
        param.put("emailAddresses", Arrays.asList("ksw6169@naver.com"));

        ObjectMapper mapper = new ObjectMapper();
        HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(param), headers);

        RestTemplate template = new RestTemplate();
        ResponseEntity<String> responseEntity = template.exchange("https://mail.apigw.ntruss.com/api/v1/address-book/address", HttpMethod.DELETE, entity, String.class);

        log.debug("responseBody : " + responseEntity.getBody());
        log.debug("responseStatusCode : " + responseEntity.getStatusCode());
    }

    /**
     * 요청 헤더 설정(공통)
     */
    private HttpHeaders makeHttpHeaders(String url, HttpMethod method, String lang) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String currentTimestamp = String.valueOf(System.currentTimeMillis());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-ncp-apigw-timestamp", currentTimestamp);
        headers.add("x-ncp-iam-access-key", accessKey);
        headers.add("x-ncp-apigw-signature-v2", makeSignature(url, method.name(), currentTimestamp));
        headers.add("x-ncp-lang", lang);

        return headers;
    }

    /**
     * 인증키 생성(공통)
     */
    private String makeSignature(String url, String method, String currentTimestamp) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String message = new StringBuilder()
                .append(method)             // HTTP 메서드
                .append(space)              // 공백
                .append(url)                // 도메인을 제외한 "/" 아래 전체 url (쿼리스트링 포함)
                .append(newLine)            // 개행
                .append(currentTimestamp)   // 요청 시간(epoch milliseconds, UTC)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        String encodeBase64String = Base64.encodeBase64String(rawHmac);

        return encodeBase64String;
    }
}
