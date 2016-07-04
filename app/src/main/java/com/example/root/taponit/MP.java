package com.example.root.taponit;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;

/**
 * MercadoPago Integration Library
 * Access MercadoPago for payments integration
 *
 * @date 2012/03/29
 * @author hcasatti
 *
 */
public class MP {
    public static final String version = "0.3.4";

    private String client_id = null;
    private String client_secret = null;
    private String ll_access_token = null;
    private JSONObject access_data = null;
    private boolean sandbox = false;
    /**
     * Instantiate MP with credentials
     */
    public MP (final String client_id, final String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    /**
     * Instantiate MP with Long Live Access Token
     */
    public MP (final String ll_access_token) {
        this.ll_access_token = ll_access_token;
    }

    public boolean sandboxMode () {
        return this.sandbox;
    }

    public boolean sandboxMode (boolean enable) {
        this.sandbox = enable;
        return this.sandbox;
    }

    /** Set or clear proxy for communication
     * @param proxy uri
     */
    public void setProxy (String proxyHost, String proxyPort) {
        this.setProxy (proxyHost + ":" + proxyPort);
    }
    public void setProxy (String proxyURI) {
        RestClient.proxy = proxyURI;
    }

    /**
     * Get Access Token for API use
     * @throws JSONException
     */
    public String getAccessToken () throws JSONException, Exception {
        if (null != this.ll_access_token) {
            return this.ll_access_token;
        }

        HashMap<String, Object> appClientValues = new HashMap<String, Object>();
        appClientValues.put("grant_type", "client_credentials");
        appClientValues.put("client_id", this.client_id);
        appClientValues.put("client_secret", this.client_secret);

        String appClientValuesQuery = this.buildQuery(appClientValues);

        JSONObject access_data = RestClient.post ("/oauth/token", appClientValuesQuery, RestClient.MIME_FORM);

        if(access_data.getInt("status") == 200) {
            this.access_data = access_data.getJSONObject("response");
            return this.access_data.optString("access_token");
        } else {
            throw new Exception(access_data.toString());
        }
    }

    /**
     * Get information for specific payment
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject getPayment (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        String uriPrefix = this.sandbox ? "/sandbox" : "";

        JSONObject paymentInfo = RestClient.get (uriPrefix + "/collections/notifications/"+id+"?access_token="+accessToken);

        return paymentInfo;
    }

    public JSONObject getPaymentInfo (String id) throws JSONException, Exception {
        return this.getPayment (id);
    }

    /**
     * Get information for specific authorized payment
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject getAuthorizedPayment (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject paymentInfo = RestClient.get ("/authorized_payments/"+id+"?access_token="+accessToken);

        return paymentInfo;
    }

    /**
     * Refund accredited payment
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject refundPayment (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject refundStatus = new JSONObject ();
        refundStatus.put("status", "refunded");

        JSONObject response = RestClient.put ("/collections/"+id+"?access_token="+accessToken, refundStatus);

        return response;
    }

    /**
     * Cancel pending payment
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject cancelPayment (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject cancelStatus = new JSONObject ();
        cancelStatus.put("status", "cancelled");

        JSONObject response = RestClient.put ("/collections/"+id+"?access_token="+accessToken, cancelStatus);

        return response;
    }

    /**
     * Cancel preapproval payment
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject cancelPreapprovalPayment (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject cancelStatus = new JSONObject ();
        cancelStatus.put("status", "cancelled");

        JSONObject response = RestClient.put ("/preapproval/"+id+"?access_token="+accessToken, cancelStatus);

        return response;
    }

    /**
     * Search payments according to filters, with pagination
     * @param filters
     * @param offset
     * @param limit
     * @return
     * @throws JSONException
     */
    public JSONObject searchPayment (Map<String, Object> filters) throws JSONException, Exception {
        return this.searchPayment(filters, 0, 0);
    }
    public JSONObject searchPayment (Map<String, Object> filters, int offset, int limit) throws JSONException {
        return this.searchPayment(filters, Long.valueOf(offset), Long.valueOf(limit));
    }
    public JSONObject searchPayment (Map<String, Object> filters, Long offset, Long limit) throws JSONException {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        filters.put("offset", offset);
        filters.put("limit", limit);

        String filtersQuery = this.buildQuery (filters);

        String uriPrefix = this.sandbox ? "/sandbox" : "";

        JSONObject collectionResult = RestClient.get (uriPrefix + "/collections/search?"+filtersQuery+"&access_token="+accessToken);
        return collectionResult;
    }

    /**
     * Create a checkout preference
     * @param preference
     * @return
     * @throws JSONException
     */
    public JSONObject createPreference (String preference) throws JSONException, Exception {
        JSONObject preferenceJSON = new JSONObject (preference);
        return this.createPreference(preferenceJSON);
    }
    public JSONObject createPreference (Map<?, ?> preference) throws JSONException, Exception {
        JSONObject preferenceJSON = map2json (preference);
        return this.createPreference(preferenceJSON);
    }
    public JSONObject createPreference (JSONObject preference) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject preferenceResult = RestClient.post ("/checkout/preferences?access_token="+accessToken, preference);
        return preferenceResult;
    }

    /**
     * Update a checkout preference
     * @param string $id
     * @param array $preference
     * @return array(json)
     * @throws JSONException
     */
    public JSONObject updatePreference (String id, String preference) throws JSONException, Exception {
        JSONObject preferenceJSON = new JSONObject (preference);
        return this.updatePreference(id, preferenceJSON);
    }
    public JSONObject updatePreference (String id, Map<?, ?> preference) throws JSONException, Exception {
        JSONObject preferenceJSON = map2json (preference);
        return this.updatePreference(id, preferenceJSON);
    }
    public JSONObject updatePreference (String id, JSONObject preference) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject preferenceResult = RestClient.put ("/checkout/preferences/"+id+"?access_token="+accessToken, preference);
        return preferenceResult;
    }

    /**
     * Get a checkout preference
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject getPreference (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject preferenceResult = RestClient.get ("/checkout/preferences/"+id+"?access_token="+accessToken);
        return preferenceResult;
    }

    /**
     * Create a preapproval payment
     * @param preapproval
     * @return
     * @throws JSONException
     */
    public JSONObject createPreapprovalPayment (String preapproval) throws JSONException, Exception {
        JSONObject preapprovalJSON = new JSONObject (preapproval);
        return this.createPreapprovalPayment(preapprovalJSON);
    }
    public JSONObject createPreapprovalPayment (Map<?, ?> preapproval) throws JSONException, Exception {
        JSONObject preapprovalJSON = map2json (preapproval);
        return this.createPreapprovalPayment(preapprovalJSON);
    }
    public JSONObject createPreapprovalPayment (JSONObject preapproval) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject preapprovalResult = RestClient.post ("/preapproval?access_token="+accessToken, preapproval);
        return preapprovalResult;
    }

    /**
     * Get a preapproval payment
     * @param id
     * @return
     * @throws JSONException
     */
    public JSONObject getPreapprovalPayment (String id) throws JSONException, Exception {
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        JSONObject preapprovalResult = RestClient.get ("/preapproval/"+id+"?access_token="+accessToken);
        return preapprovalResult;
    }

    /**
     * Generic resource get
     * @param uri
     * @param authenticate
     * @return
     * @throws JSONException
     */
    public JSONObject get (String uri, Map<String, Object> params, boolean authenticate) throws JSONException, Exception {
        if (params == null) {
            params = new HashMap<String, Object> ();
        }
        if (authenticate) {
            String accessToken;
            try {
                accessToken = this.getAccessToken ();
            } catch (Exception e) {
                JSONObject result = new JSONObject(e.getMessage());
                return result;
            }

            params.put("access_token", accessToken);
        }

        if (!params.isEmpty()) {
            uri += (uri.contains("?") ? "&" : "?") + this.buildQuery (params);
        }

        JSONObject result = RestClient.get (uri);
        return result;
    }

    /**
     * Generic resource get (authenticated)
     * @param uri
     * @param params
     * @return
     * @throws JSONException
     */
    public JSONObject get (String uri, Map<String, Object> params) throws JSONException, Exception {
        return this.get(uri, params, true);
    }

    /**
     * Generic resource get
     * @param uri
     * @param authenticate
     * @return
     * @throws JSONException
     */
    public JSONObject get (String uri, boolean authenticate) throws JSONException, Exception {
        return this.get(uri, null, authenticate);
    }

    /**
     * Generic resource get (authenticated)
     * @param uri
     * @return
     * @throws JSONException
     */
    public JSONObject get (String uri) throws JSONException, Exception {
        return this.get(uri, null, true);
    }

    /**
     * Generic resource post
     * @param uri
     * @param data
     * @return
     * @throws JSONException
     */
    public JSONObject post (String uri, String data) throws JSONException, Exception {
        JSONObject dataJSON = new JSONObject (data);
        return this.post(uri, dataJSON);
    }
    public JSONObject post (String uri, String data, Map<String, Object> params) throws JSONException, Exception {
        JSONObject dataJSON = new JSONObject (data);
        return this.post(uri, dataJSON, params);
    }
    public JSONObject post (String uri, Map<?, ?> data) throws JSONException, Exception {
        JSONObject dataJSON = map2json (data);
        return this.post(uri, dataJSON);
    }
    public JSONObject post (String uri, Map<?, ?> data, Map<String, Object> params) throws JSONException, Exception {
        JSONObject dataJSON = map2json (data);
        return this.post(uri, dataJSON, params);
    }
    public JSONObject post (String uri, JSONObject data) throws JSONException, Exception {
        return this.post(uri, data, null);
    }
    public JSONObject post (String uri, JSONObject data, Map<String, Object> params) throws JSONException, Exception {
        if (params == null) {
            params = new HashMap<String, Object> ();
        }
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
            params.put("access_token", accessToken);
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        if (!params.isEmpty()) {
            uri += (uri.contains("?") ? "&" : "?") + this.buildQuery (params);
        }

        JSONObject result = RestClient.post (uri, data);
        return result;
    }

    /**
     * Generic resource put
     * @param uri
     * @param data
     * @return
     * @throws JSONException
     */
    public JSONObject put (String uri, String data) throws JSONException, Exception {
        JSONObject dataJSON = new JSONObject (data);
        return this.put(uri, dataJSON);
    }
    public JSONObject put (String uri, String data, Map<String, Object> params) throws JSONException, Exception {
        JSONObject dataJSON = new JSONObject (data);
        return this.put(uri, dataJSON, params);
    }
    public JSONObject put (String uri, Map<?, ?> data) throws JSONException, Exception {
        JSONObject dataJSON = map2json (data);
        return this.put(uri, dataJSON);
    }
    public JSONObject put (String uri, Map<?, ?> data, Map<String, Object> params) throws JSONException, Exception {
        JSONObject dataJSON = map2json (data);
        return this.put(uri, dataJSON, params);
    }
    public JSONObject put (String uri, JSONObject data) throws JSONException, Exception {
        return this.put(uri, data, null);
    }
    public JSONObject put (String uri, JSONObject data, Map<String, Object> params) throws JSONException, Exception {
        if (params == null) {
            params = new HashMap<String, Object> ();
        }
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
            params.put("access_token", accessToken);
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        if (!params.isEmpty()) {
            uri += (uri.contains("?") ? "&" : "?") + this.buildQuery (params);
        }

        JSONObject result = RestClient.put (uri, data);
        return result;
    }

    /**
     * Generic resource delete
     * @param uri
     * @return
     * @throws JSONException
     */
    public JSONObject delete (String uri, Map<String, Object> params) throws JSONException, Exception {
        if (params == null) {
            params = new HashMap<String, Object> ();
        }
        String accessToken;
        try {
            accessToken = this.getAccessToken ();
            params.put("access_token", accessToken);
        } catch (Exception e) {
            JSONObject result = new JSONObject(e.getMessage());
            return result;
        }

        if (!params.isEmpty()) {
            uri += (uri.contains("?") ? "&" : "?") + this.buildQuery (params);
        }

        JSONObject result = RestClient.delete (uri);
        return result;
    }

    /**
     * Generic resource delete
     * @param uri
     * @return
     * @throws JSONException
     */
    public JSONObject delete (String uri) throws JSONException, Exception {
        return this.delete (uri, null);
    }

    /*****************************************************************************************************/
    private String buildQuery (Map<String, Object> params) {
        String[] query = new String[params.size()];
        int index = 0;
        for (String key : params.keySet()) {
            String val = String.valueOf(params.get(key) != null ? params.get(key) : "");
            try {
                val = URLEncoder.encode(val, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            query[index++] = key+"="+val;
        }

        return StringUtils.join(query, "&");
    }

    private static JSONObject map2json (Map<?, ?> preference) throws JSONException, Exception {
        JSONObject result = new JSONObject();

        for (Entry<?, ?> entry : preference.entrySet()) {
            if (entry.getValue () instanceof Collection) {
                result.put((String) entry.getKey(), map2json((Collection<?>)entry.getValue()));
            } else if (entry.getValue() instanceof Map) {
                result.put((String) entry.getKey(), map2json((Map<?, ?>)entry.getValue()));
            } else {
                result.put((String) entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private static JSONArray map2json (Collection<?> collection) throws JSONException, Exception {
        JSONArray result = new JSONArray();

        for (Object object : collection) {
            if (object instanceof Map) {
                result.put(map2json((Map<?, ?>)object));
            } else {
                result.put(object);
            }
        }

        return result;
    }

    private static class RestClient {
        private static final String API_BASE_URL = "https://api.mercadopago.com";
        public static final String MIME_JSON = "application/json";
        public static final String MIME_FORM = "application/x-www-form-urlencoded";
        public static String proxy = null;

        private static JSONObject exec (String method, String uri, Object data, String contentType) throws JSONException {
            ClientResponse apiResult;
            JSONObject response = new JSONObject ();

            try {
                apiResult = buildRequest(API_BASE_URL+uri, contentType).method(method, ClientResponse.class, data);
                int apiHttpCode = apiResult.getStatus();

                response.put("status", apiHttpCode);

                String responseBody = apiResult.getEntity(String.class);

                response.put("response", responseBody.indexOf("[") == 0 ? new JSONArray(responseBody) : new JSONObject(responseBody));
            } catch (Exception e) {
                response.put("status", 500);
                response.put("error", e.getMessage());
            }

            return response;
        }

        public static JSONObject get (String uri) throws JSONException {
            return get(uri, MIME_JSON);
        }

        public static JSONObject get (String uri, String contentType) throws JSONException {
            return exec ("GET", uri, null, contentType);
        }

        public static JSONObject post (String uri, Object data) throws JSONException {
            return post(uri, data, MIME_JSON);
        }

        public static JSONObject post (String uri, Object data, String contentType) throws JSONException {
            return exec ("POST", uri, data, contentType);
        }

        public static JSONObject put (String uri, Object data) throws JSONException {
            return put(uri, data, MIME_JSON);
        }

        public static JSONObject put (String uri, Object data, String contentType) throws JSONException {
            return exec ("PUT", uri, data, contentType);
        }

        public static JSONObject delete (String uri) throws JSONException {
            return delete(uri, MIME_JSON);
        }

        public static JSONObject delete (String uri, String contentType) throws JSONException {
            return exec ("DELETE", uri, null, contentType);
        }

        private static Builder buildRequest (String resourceUrl, String contentType) {
            // Obtenemos cliente Http de Apache
            DefaultApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
            if (null != proxy) {
                config.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI, proxy);
            }

            ApacheHttpClient client = ApacheHttpClient.create(config);

            WebResource resource = client.resource(resourceUrl);
            Builder req = resource.type(contentType).accept("application/json");
            req.header("User-Agent", "MercadoPago Java SDK v"+MP.version);
            return req;
        }
    }
}