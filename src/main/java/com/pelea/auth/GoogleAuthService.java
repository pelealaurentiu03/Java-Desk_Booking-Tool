package com.pelea.auth;

import io.github.cdimascio.dotenv.Dotenv;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pelea.database.DatabaseManager;
import com.pelea.auth.TriConsumer;

public class GoogleAuthService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");
    private static final String CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET");
    private static final String REDIRECT_URI = dotenv.get("REDIRECT_URI");
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    //Initiate the login
    public void performLogin(TriConsumer<String, String, String> onLoginSuccess) {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            
            String loginUrl = AUTH_URL + "?client_id=" + CLIENT_ID
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope=email%20profile";

            System.out.println("Opening browser for login");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(loginUrl));
            } else {
                System.out.println("Please open the link manually: " + loginUrl);
            }

            System.out.println("Waiting Google connection...");
            Socket clientSocket = serverSocket.accept();

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String line = in.readLine();
            
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("\r\n");
            out.println("<h1>Login succesfull</h1><p>You can go back to the main app.</p><script>window.close();</script>");
            out.flush();
            
            clientSocket.close();
            serverSocket.close();

            if (line != null && line.contains("code=")) {
                String code = line.split("code=")[1].split(" ")[0];
                System.out.println("Code received! Changing the code for Token...");
                
                exchangeCodeForToken(code, onLoginSuccess);
            } else {
                System.out.println("Error: Autentification code not recived.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exchangeCodeForToken(String code, TriConsumer<String, String, String> onLoginSuccess) {
        try {
            String params = "client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET +
                            "&code=" + code + "&grant_type=authorization_code" + "&redirect_uri=" + REDIRECT_URI;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String accessToken = jsonResponse.get("access_token").getAsString();
                getUserInfo(accessToken, onLoginSuccess);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void getUserInfo(String accessToken, TriConsumer<String, String, String> onLoginSuccess) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USER_INFO_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject userInfo = JsonParser.parseString(response.body()).getAsJsonObject();
                String email = userInfo.get("email").getAsString();
                String name = userInfo.has("name") ? userInfo.get("name").getAsString() : "No Name";
                String photoUrl = userInfo.has("picture") ? userInfo.get("picture").getAsString() : null;

                DatabaseManager.getInstance().saveUser(email, name);
                
                if (onLoginSuccess != null) {
                    onLoginSuccess.accept(email, name, photoUrl); // Acum va merge!
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}