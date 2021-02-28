package fr.flowarg.mcmsal;

import fr.flowarg.flowcollections.MapHelper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// Adapted from MiniLauncher (a project of MiniDigger), see his amazing work on GitHub!
// I made a backport of the work of MiniDigger for Java 8 (MiniLauncher uses the new Java 11 HTTPClient).
public class MicrosoftAuthentication
{
    private static final String AUTH_TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private static final String XBL_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_STORE_URL = "https://api.minecraftservices.com/entitlements/mcstore";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    public void authenticate(String authCode, Consumer<AuthInfo> callback) throws MCMSALException
    {
        try
        {
            final URL url = URI.create(AUTH_TOKEN_URL).toURL();
            final Map<Object, Object> data = MapHelper.of("client_id", "00000000402b5328", "code", authCode, "grant_type", "authorization_code", "redirect_uri", "https://login.live.com/oauth20_desktop.srf", "scope", "service::user.auth.xboxlive.com::MBI_SSL");
            final JSONObject jsonObject = (JSONObject)new JSONParser().parse(sendPostRequest(DataType.FORM_DATA, data, url, ContentType.APP_FORM_ENCODED, null));
            callback.accept(this.acquireXBLToken((String)jsonObject.get("access_token")));
        } catch (MalformedURLException | ParseException e)
        {
            throw new MCMSALException(e);
        }
    }

    private AuthInfo acquireXBLToken(String accessToken) throws MCMSALException
    {
        try
        {
            final URL url = URI.create(XBL_AUTH_URL).toURL();
            final Map<Object, Object> data = MapHelper.of("Properties", MapHelper.of("AuthMethod", "RPS", "SiteName", "user.auth.xboxlive.com", "RpsTicket", accessToken), "RelyingParty", "http://auth.xboxlive.com", "TokenType", "JWT");

            final JSONObject jsonObject = (JSONObject)new JSONParser().parse(sendPostRequest(DataType.JSON_DATA, data, url, ContentType.APP_JSON, connection -> connection.setRequestProperty("Accept", ContentType.APP_JSON.getContentType())));
            final String xblToken = (String)jsonObject.get("Token");
            return this.acquireXsts(xblToken);
        } catch (MalformedURLException | ParseException e)
        {
            throw new MCMSALException(e);
        }
    }

    private AuthInfo acquireXsts(String xblToken) throws MCMSALException
    {
        try
        {
            final URL url = URI.create(XSTS_AUTH_URL).toURL();
            final Map<Object, Object> data = MapHelper.of("Properties", MapHelper.of("SandboxId", "RETAIL", "UserTokens", Collections.singletonList(xblToken)), "RelyingParty", "rp://api.minecraftservices.com/", "TokenType", "JWT");

            final JSONObject jsonObject = (JSONObject)new JSONParser().parse(sendPostRequest(DataType.JSON_DATA, data, url, ContentType.APP_JSON, connection -> connection.setRequestProperty("Accept", ContentType.APP_JSON.getContentType())));
            final String xblXsts = (String)jsonObject.get("Token");
            final JSONObject claims = (JSONObject)jsonObject.get("DisplayClaims");
            final JSONArray xui = (JSONArray)claims.get("xui");
            final String uhs = (String)((JSONObject)xui.get(0)).get("uhs");
            return this.acquireMinecraftToken(uhs, xblXsts);
        } catch (MalformedURLException | ParseException e)
        {
            throw new MCMSALException(e);
        }
    }

    private AuthInfo acquireMinecraftToken(String xblUhs, String xblXsts) throws MCMSALException
    {
        try
        {
            final URL url = URI.create(MC_LOGIN_URL).toURL();
            final Map<Object, Object> data = MapHelper.of("identityToken", "XBL3.0 x=" + xblUhs + ";" + xblXsts);

            final JSONObject jsonObject = (JSONObject)new JSONParser().parse(sendPostRequest(DataType.JSON_DATA, data, url, ContentType.APP_JSON, connection -> connection.setRequestProperty("Accept", ContentType.APP_JSON.getContentType())));
            final String mcAccessToken = (String)jsonObject.get("access_token");
            this.checkMcStore(mcAccessToken);
            return this.checkMcProfile(mcAccessToken);
        } catch (MalformedURLException | ParseException e)
        {
            throw new MCMSALException(e);
        }
    }

    private String checkMcStore(String mcAccessToken) throws MCMSALException
    {
        try
        {
            final URL url = URI.create(MC_STORE_URL).toURL();
            return sendGetRequest(url, mcAccessToken);
            // TODO make a store object.
        } catch (MalformedURLException e)
        {
            throw new MCMSALException(e);
        }
    }

    private AuthInfo checkMcProfile(String mcAccessToken) throws MCMSALException
    {
        try
        {
            final URL url = URI.create(MC_PROFILE_URL).toURL();
            final String body = sendGetRequest(url, mcAccessToken);

            final JSONObject jsonObject = (JSONObject)new JSONParser().parse(body);
            final String name = (String)jsonObject.get("name");
            final String uuid = (String)jsonObject.get("id");
            final String uuidDashes = uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");

            return new AuthInfo(name, mcAccessToken, UUID.fromString(uuidDashes), MapHelper.of(), "mojang", this.checkMcStore(mcAccessToken));
        } catch (ParseException | MalformedURLException e)
        {
            throw new MCMSALException(e);
        }
    }

    // FlowArg

    private static String sendPostRequest(DataType dataType, Map<Object, Object> data, URL url, ContentType contentType, Consumer<HttpURLConnection> parameters)
    {
        HttpsURLConnection connection = null;
        try
        {
            final byte[] payloadBytes = dataType.getFunction().apply(data).getBytes(StandardCharsets.UTF_8);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType.getContentType());
            configureConnection(connection);
            if(parameters != null)
                parameters.accept(connection);

            final OutputStream out = connection.getOutputStream();
            out.write(payloadBytes, 0, payloadBytes.length);
            out.close();
            int responseCode = connection.getResponseCode();
            BufferedReader reader = null;
            String body = "";
            if(responseCode >= 200 && responseCode < 300 && connection.getInputStream() != null)
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            else if(connection.getErrorStream() != null)
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            if(reader != null)
            {
                if(contentType != ContentType.APP_JSON)
                    body = reader.readLine();
                else
                {
                    String str;
                    final StringBuilder sb = new StringBuilder();
                    while((str = reader.readLine()) != null)
                        sb.append(str);
                    body = sb.toString();
                }
                reader.close();
            }
            return body;
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            if (connection != null)
                connection.disconnect();
        }
        return "";
    }

    private static String sendGetRequest(URL url, String token)
    {
        HttpsURLConnection connection = null;
        try
        {
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            configureConnection(connection);

            int responseCode = connection.getResponseCode();
            BufferedReader reader = null;
            String body = "";
            if(responseCode >= 200 && responseCode < 300 && connection.getInputStream() != null)
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            else if(connection.getErrorStream() != null)
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            if(reader != null)
            {
                String str;
                final StringBuilder sb = new StringBuilder();
                while((str = reader.readLine()) != null)
                    sb.append(str);
                body = sb.toString();
                reader.close();
            }
            return body;
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            if (connection != null)
                connection.disconnect();
        }
        return "";
    }

    private static void configureConnection(HttpURLConnection connection)
    {
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
    }
}
