package com.kiumiu.ca.api500px;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.util.Log;

import com.fivehundredpx.api.auth.AccessToken;

/**
 * Utility class for handling REST Http command
 * @author Jerry Fan
 */
public class RESTTransport {
	private static final String TAG = "RESTTransport";

    private static String HOST = "https://api.500px.com/v1";

    private AccessToken accessToken;
    private String consumerKey;
    private String consumerSecret;

    public RESTTransport(AccessToken accessToken, String consumerKey,
            String consumerSecret) {
        super();
        this.accessToken = accessToken;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    public RESTTransport(String consumerKey) {
        super();
        this.consumerKey = consumerKey;
    }

    public JSONObject get(String url) {
        if (null != accessToken) {
            HttpGet request = new HttpGet(HOST + url);
            return handleSigned(request);
        } else {
            final String finalUrl = String.format("%s%s&consumer_key=%s", HOST,
                    url, this.consumerKey);
            return handle(new HttpGet(finalUrl));
        }
    }

    public JSONObject post(String url, List<? extends NameValuePair> params) {
        HttpPost request = new HttpPost(HOST + url);
        try {
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Parameters in post are invalid", e);
        }
        return handleSigned(request);
    }
    
    public JSONObject postMultiPart(String url, MultipartEntity params) {
        HttpPost request = new HttpPost(HOST + url);
        request.setEntity(params);
        return handleSigned(request);
    }
    
    public JSONObject put(String url, List<? extends NameValuePair> params) {
    	HttpPut request = new HttpPut(HOST + url);
        try {
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Parameters in put are invalid", e);
        }
        return handleSigned(request);
    }

    public JSONObject delete(String url) {
        HttpDelete request = new HttpDelete(HOST + url);
        
        return handleSigned(request);
    }

    private void signRequest(HttpUriRequest request)
            throws OAuthMessageSignerException,
            OAuthExpectationFailedException, OAuthCommunicationException {
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(
                consumerKey, consumerSecret);
        consumer.setTokenWithSecret(accessToken.getToken(),
                accessToken.getTokenSecret());
        consumer.sign(request);
    }

    private JSONObject handleSigned(HttpUriRequest request) {
        try {
            signRequest(request);
        } catch (Exception e) {
            Log.e(TAG, "Erro trying to sign the request.", e);
        }
        return handle(request);
    }

    private JSONObject handle(HttpUriRequest request) {
    	
    	boolean isFailed = false;
    	
        try {
            DefaultHttpClient client = new DefaultHttpClient();

            HttpResponse response = client.execute(request);
            final int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                final String msg = String.format(
                        "Error, statusCode not OK(%d). for url: %s",
                        statusCode, request.getURI().toString());
                Log.e(TAG, msg);
                isFailed = true;
            }

            HttpEntity responseEntity = response.getEntity();
            InputStream inputStream = responseEntity.getContent();
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }

            JSONObject json = new JSONObject(total.toString());
            if(isFailed) {
            	Log.e(TAG, json.toString());
            	return null;
            }
            
            return json;
        } catch (Exception e) {
            Log.e(TAG, "Error obtaining response from 500px api.", e);
        }
        return null;
    }
}
