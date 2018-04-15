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
import java.util.Map;
import java.util.Set;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import edu.kit.aifb.openapi2linkeddata.GsonTools.ConflictStrategy;
import edu.kit.aifb.openapi2linkeddata.GsonTools.JsonObjectExtensionConflictException;
import io.swagger.models.Swagger;
import io.swagger.util.Json;

public class OpenAPI2LinkedDataToolsLocalVersion {
    public JsonObject serialize(Swagger swaggerObject) {
        
        String filename = "context.json";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename); // read context.json as inputStream
        JsonParser jsonParser = new JsonParser();
        JsonObject context = (JsonObject)jsonParser.parse(new InputStreamReader(in).toString()); // Create an JsonObject (gson) from inputStream of context.jason
        
        Map<String, ?> map = swaggerObject.getPaths(); // It should be better a LinkedHashMap, for this thing Swaggers's Json class must be changed
                                                       // Map is filled in with the paths. For this the already existing method from swagger core is used
        Set<String> setOfPaths = map.keySet(); // Set has a default method to convert a map to an array of string, so I used it
        String[] arrayOfPaths = setOfPaths.toArray(new String[setOfPaths.size()]); // Here the set is converted to Array of Strings
        
        // Here hydra-Stuff is written to each path
        // Since all paths were saved in the Set<String> in the reverse order, this cycle runs "from bottom to top"
        JsonObject[] formattedPathStrings = new JsonObject[arrayOfPaths.length];
        for (int i = formattedPathStrings.length - 1; i >= 0; i--) {
            formattedPathStrings[i] = new JsonObject();
            formattedPathStrings[i].addProperty("x-hydra-endpoint", arrayOfPaths[i]); // We once discussed whether x-hydra-endpoint needs to stand everywhere. 
                                                                                      // Last time we came to the fact that it must actually stand everywhere, in all paths.
            // Now it looks like this: "x-hydra-endpoint": object, ... "x-hydra-endpoint": object, ... , "x-hydra-endpoint": object, ...
        }
        
        String swaggerCodeAsString = Json.pretty(swaggerObject);
        JsonObject formattedSwaggerCode = jsonParser.parse(swaggerCodeAsString).getAsJsonObject(); // The whole swagger.json is saved as a JsonObject
        
        JsonObject[] contentOfPaths = new JsonObject[arrayOfPaths.length];
        // Through swagger.json is iterated and all values of all paths are saved to contentOfPaths. 
        for(int i = 0; i < contentOfPaths.length; i++) {
            contentOfPaths[i] = formattedSwaggerCode.get("paths").getAsJsonObject().get(arrayOfPaths[i]).getAsJsonObject(); // In debug mode, it's easy to see what's in contentOfPaths
        }
        
        JsonArray ar = new JsonArray();
        for(int i = 0, j = formattedPathStrings.length - 1; i < formattedPathStrings.length; i++, j--) {
            ar.add(mergeJSONObjects(formattedPathStrings[j], contentOfPaths[i])); // The "new" paths are supplemented with the old contents
        }
        
        formattedSwaggerCode.remove("paths"); // Container "path" is removed from old swagger.json
        formattedSwaggerCode.add("paths", ar); // In its place, the paths are written in a new format with old contents
        
        return mergeJSONObjects(context, formattedSwaggerCode); // Finally, we just need to merge the context with the new swagger.json code
    }

    private JsonObject mergeJSONObjects(JsonObject json1, JsonObject json2) {
        JsonObject mergedJSON = json1;
        try {
            GsonTools.extendJsonObject(mergedJSON, ConflictStrategy.THROW_EXCEPTION, json2);
        } catch (JsonObjectExtensionConflictException e) {
            e.printStackTrace();
        }
        return mergedJSON;
    }
}
