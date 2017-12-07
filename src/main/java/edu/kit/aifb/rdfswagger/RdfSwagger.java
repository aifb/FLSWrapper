package edu.kit.aifb.rdfswagger;

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

import edu.kit.aifb.rdfswagger.GsonTools.ConflictStrategy;
import edu.kit.aifb.rdfswagger.GsonTools.JsonObjectExtensionConflictException;

@Path("/rdfswagger.{type:jsonld|xml|ttl}")
public class RdfSwagger
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
	public void Parser(HttpServletRequest baseRequest)
	{
		String sURL = "/ksri-km-flswrapper/swagger.json";
		String filename = "context.json";

		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
		
		JSONTokener tokener = new JSONTokener(new InputStreamReader(in));
		JSONObject tempContext = new JSONObject(tokener); // context for generated swagger file
		JsonParser jsonParser = new JsonParser();
	    JsonObject context = (JsonObject)jsonParser.parse(tempContext.toString());
	    
		JsonObject body = new JsonObject(); // from swagger generated .json code

		URL url;
		try
		{
			url = new URL(getBaseUrl(baseRequest) + sURL);
			HttpURLConnection request;
			try
			{
				request = (HttpURLConnection) url.openConnection();
				request.connect();
				//JsonElement root = new JsonParser().parse(new InputStreamReader((InputStream)request.getContent()));
				
				JsonParser jsonParser1 = new JsonParser();
				body = (JsonObject)jsonParser1.parse(new InputStreamReader((InputStream)request.getContent()));
				JsonObject mergedJSON = mergeJSONObjects(context, body);
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
	}
	public JsonObject mergeJSONObjects(JsonObject json1, JsonObject json2) {
	    JsonObject temp = json1;
	    try {
            GsonTools.extendJsonObject(temp, ConflictStrategy.THROW_EXCEPTION, json2);
        } catch (JsonObjectExtensionConflictException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

		/*Iterator<String> i1 = json1.get
		Iterator<String> i2 = json2.keys();
		String tmp_key;
		while(i1.hasNext()) {
			tmp_key = (String) i1.next();
			mergedJSON.add(tmp_key, (JsonElement) json1.get(tmp_key));
		}
		while(i2.hasNext()) {
			tmp_key = (String) i2.next();
			mergedJSON.add(tmp_key, (JsonElement) json2.get(tmp_key));
		}
		JsonObject d = mergedJSON;*/
		String result = (json1.toString().substring(0, json1.toString().length() - 1) + ",").concat(json2.toString().substring(1, json2.toString().length()));
		JsonParser parser = new JsonParser();
		JsonObject o = parser.parse(result).getAsJsonObject();
		//mergedJSON = new JSONObject(result);
		System.out.println(result);
		//System.out.println(mergedJSON.toString());
		return temp;
	}
}
