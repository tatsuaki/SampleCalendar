package com.tatuaki.k.test.calendar.samplecalendar;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

public class GoogleCalendar {

    private static HttpTransport HTTP_TRANSPORT;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    // リダイレクトURL
    private static final String REDIRECT_URL = "http://localhost:8080/google/callback";

    public GoogleCalendar() {
    }

    /**
     * google flow 認証のためのオブジェクト取得
     */
    public GoogleAuthorizationCodeFlow getFlow() throws IOException, GeneralSecurityException {

        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // スコープの設定
        Set<String> scopes = new HashSet<String>();
        scopes.add(CalendarScopes.CALENDAR);
        scopes.add(CalendarScopes.CALENDAR_READONLY);

        GoogleClientSecrets clientSecrets = getClientSecrets();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setAccessType("offline").setApprovalPrompt("force").build();

        return flow;
    }

    /**
     * JSONファイル取得
     */
    public GoogleClientSecrets getClientSecrets() throws IOException {
        Reader SECRET_FILE = new InputStreamReader(
                GoogleCalendar.class.getResourceAsStream("/client_secrets.json"));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, SECRET_FILE);

        return clientSecrets;
    }

    /**
     * 認証URL取得
     */
    public String getGoogleOAuthURL() throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = getFlow();
        return flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URL).build();
    }
}
