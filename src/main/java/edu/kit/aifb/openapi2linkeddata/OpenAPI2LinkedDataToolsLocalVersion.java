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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.semanticweb.yars.nx.Node;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import edu.kit.aifb.openapi2linkeddata.GsonTools.ConflictStrategy;
import edu.kit.aifb.openapi2linkeddata.GsonTools.JsonObjectExtensionConflictException;

@Path("/rdfswagger.{type:jsonld|xml|ttl}")
public class OpenAPI2LinkedDataToolsLocalVersion {
	
	
//	@POST
//	@Produces({"application/ld+json", "text/turtle"})
//	@Consumes(MediaType.APPLICATION_JSON)
//	public Response convertSwagger(JsonObject body) {
//		
//		Object nodes = null;
//	
//		return Response.ok().entity(new GenericEntity<Iterable<Node[]>>( nodes ) { }).build();
//	}
	
	
    @GET
    @Produces("text/html")
    public JsonObject serialize(String swaggerCodeAsString, String[] arrayOfPaths) {
        String filename = "context.json";

        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);

        JSONTokener tokener = new JSONTokener(new InputStreamReader(in));
        JSONObject tempContext = new JSONObject(tokener); // context for generated swagger file
        JsonParser jsonParser = new JsonParser();
        JsonObject context = (JsonObject)jsonParser.parse(tempContext.toString());
        
        JsonObject[] formattedPathStrings = new JsonObject[arrayOfPaths.length];
        
        formattedPathStrings[formattedPathStrings.length - 1] = new JsonObject();
        formattedPathStrings[formattedPathStrings.length - 1].addProperty("x-hydra-endpoint", arrayOfPaths[arrayOfPaths.length - 1]);
        
        for (int i = formattedPathStrings.length - 2; i >= 0; i--) {
            formattedPathStrings[i] = new JsonObject();
            formattedPathStrings[i].addProperty("x-hydra-endpoint", arrayOfPaths[i]);
        }
        
        JsonObject formattedSwaggerCode = jsonParser.parse(swaggerCodeAsString).getAsJsonObject();
        
        JsonArray ar = new JsonArray();
        JsonObject[] contentOfPaths = new JsonObject[arrayOfPaths.length];
        for(int i = 0; i < contentOfPaths.length; i++) {
            contentOfPaths[i] = formattedSwaggerCode.get("paths").getAsJsonObject().get(arrayOfPaths[i]).getAsJsonObject();
        }
        
        for(int i = 0, j = formattedPathStrings.length - 1; i < formattedPathStrings.length; i++, j--) {
            ar.add(mergeJSONObjects(formattedPathStrings[j], contentOfPaths[i]));
        }
        formattedSwaggerCode.remove("paths");
        formattedSwaggerCode.add("paths", ar);
        
        //JsonObject body = jsonParser.parse(swaggerBody).getAsJsonObject(); // from swagger generated .json code
        return mergeJSONObjects(context, formattedSwaggerCode);
    }

    private JsonObject mergeJSONObjects(JsonObject json1, JsonObject json2) {
        JsonObject mergedJSON = json1;
        try {
            GsonTools.extendJsonObject(mergedJSON, ConflictStrategy.THROW_EXCEPTION, json2);
        } catch (JsonObjectExtensionConflictException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return mergedJSON;
    }
}
