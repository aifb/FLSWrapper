package edu.kit.aifb.openapi2linkeddata;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import edu.kit.aifb.openapi2linkeddata.GsonTools.ConflictStrategy;
import edu.kit.aifb.openapi2linkeddata.GsonTools.JsonObjectExtensionConflictException;

@Path("/rdfswagger.{type:jsonld|xml|ttl}")
public class OpenAPI2LinkedDataTools
{
	public static String getBaseUrl(HttpServletRequest request) 
	{
		String scheme = request.getScheme() + "://";
		String serverName = request.getServerName();
		String serverPort = (request.getServerPort() == 80) ? "" : ":" + request.getServerPort();
		String contextPath = request.getContextPath();
		return scheme + serverName + serverPort + contextPath;
	}

	@GET
	@Produces("text/html")
	public JsonObject parse(HttpServletRequest baseRequest)
	{
		//String sURL = "/ksri-km-flswrapper/swagger.json";
	    String sURL = "/swagger.json";
		String filename = "context.json";

		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
		
		JSONTokener tokener = new JSONTokener(new InputStreamReader(in));
		JSONObject tempContext = new JSONObject(tokener); // context for generated swagger file
		JsonParser jsonParser = new JsonParser();
	    JsonObject context = (JsonObject)jsonParser.parse(tempContext.toString());
	    
		JsonObject body = new JsonObject(); // from swagger generated .json code
		JsonObject mergedJSON = null;
		
		URL url;
		try
		{
			url = new URL(getBaseUrl(baseRequest) + sURL);
			HttpURLConnection request;
			try
			{
				request = (HttpURLConnection) url.openConnection();
				request.connect();
				
				JsonParser jsonParser1 = new JsonParser();
				body = (JsonObject)jsonParser1.parse(new InputStreamReader((InputStream)request.getContent())); // Swagger's generated code will merged with context
				mergedJSON = mergeJSONObjects(context, body);
				return mergedJSON;
			} 
			catch (IOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mergedJSON;
		
	}
	public JsonObject mergeJSONObjects(JsonObject json1, JsonObject json2) {
	    JsonObject mergedJSON = json1;
	    try {
            GsonTools.extendJsonObject(mergedJSON, ConflictStrategy.THROW_EXCEPTION, json2);
        } catch (JsonObjectExtensionConflictException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	    System.out.println(mergedJSON);
		return mergedJSON;
	}
}
