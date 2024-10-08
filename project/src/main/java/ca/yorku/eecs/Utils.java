package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.json.*;

class Utils implements HttpHandler{      
	
	Neo4j neo;
	 JSONObject jsonResponse;
	public Utils() {
		neo = new Neo4j();
		jsonResponse = new JSONObject();
	}
	
	

	//Receive request and determine if it is get or put
	//then call the corresponding method
    @Override
    public void handle(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();
        String path = uri.getPath();
        String method = request.getRequestMethod();
        String response = "";

        if (method.equals("PUT")) {
        	handlePut(request);
        }
        else if (method.equals("GET")) {
            handleGet(request);
        }
        
        else {
            response = "{\"status\":\"405 METHOD NOT ALLOWED\"}";   //If the method is not GET or PUT
            request.sendResponseHeaders(405, response.length());
        }


    }

    //handle all the get method
    private void handleGet(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();
        String path = uri.getPath();
        String query = uri.getQuery();
        String response = "";
        Map<String, String> entry = new HashMap<String, String>();
        if(path.equals("/api/v1/getMostActedName")) {
        	
        }
        else if (query == null) {
            response = "400 BAD REQUEST: Missing required information";
            sendString(request, response, 400);
            return;
        }
        else {
        	 entry = splitQuery(query);
        }
        try {
            if (path.equals("/api/v1/getActor")) {
            	//check format
            	if (!entry.containsKey("actorId") || entry.get("actorId").isEmpty()) {
            	    response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
            	    sendString(request, response, 400);
            	    return;
            	}
            	// check existence
            	if(!neo.checkId(entry.get("actorId"))){
	                response = "404 NOT FOUND: actorId not found";      
	            	sendString(request, response, 404);
	                return;
	            }
            	List<String> movies = neo.getMoviesByActorId(entry.get("actorId"));
            	Map<String, Object> map = new LinkedHashMap<>();
            	map.put("actorId", entry.get("actorId"));
            	map.put("name", neo.getActorNameById(entry.get("actorId")));

            	JSONArray movieArray = new JSONArray();
            	for (String movie : movies) {
            	    movieArray.put(movie);
            	}

            	map.put("movies", movieArray);

            	JSONObject jsonObject = new JSONObject(map);
            	response = jsonObject.toString(2);
            	sendString(request, response, 200);


            }
            else if (path.equals("/api/v1/getMovie")) {
                // Check format
                if (!entry.containsKey("movieId") || entry.get("movieId").isEmpty()) {
                    response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
                    sendString(request, response, 400);
                    return;
                }
                // Check existence
                if (!neo.checkMovieId(entry.get("movieId"))) {
                    response = "404 NOT FOUND: movieId not found";
                    sendString(request, response, 404);
                    return;
                }
                List<String> actors = neo.getActorIdsByMovieId(entry.get("movieId"));
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("movieId", entry.get("movieId"));
                map.put("title", neo.getActorIdsByMovieId(entry.get("movieId")));

                JSONArray actorArray = new JSONArray();
                for (String actor : actors) {
                    actorArray.put(actor);
                }

                map.put("actors", actorArray);

                JSONObject jsonObject = new JSONObject(map);
                response = jsonObject.toString(2);
                sendString(request, response, 200);
            }
            else if (path.equals("/api/v1/hasRelationship")) {
            	 String actorId = entry.get("actorId");
            	    String movieId = entry.get("movieId");

            	    if (actorId == null || movieId == null || actorId.isEmpty() || movieId.isEmpty()) {
            	        response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
            	        sendString(request, response, 400);
            	        return;
            	    }

            	    if (!neo.checkId(actorId) || !neo.checkMovieId(movieId)) {
            	        response = "404 NOT FOUND: Provided ID does not exist";
            	        sendString(request, response, 404);
            	        return;
            	    }

            	    boolean exists = neo.checkRelationship(actorId, movieId);
            	    Map<String, Object> map = new LinkedHashMap<>();
            	    map.put("actorId", actorId);
            	    map.put("movieId", movieId);
            	    map.put("hasRelationship", exists);

            	    JSONObject jsonObject = new JSONObject(map);
            	    response = jsonObject.toString(2);

            	    sendString(request, response, 200);
            }
            else if (path.equals("/api/v1/computeBaconNumber")) {
                String actorId = entry.get("actorId");

                if (actorId == null || actorId.isEmpty()) {
                    response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
                    sendString(request, response, 400);
                    return;
                }

                if (!neo.checkId(actorId)) {
                    response = "404 NOT FOUND: Provided actorId does not exist";
                    sendString(request, response, 404);
                    return;
                }

                int baconNumber = neo.computeBaconNumber(actorId);
                
                if (baconNumber == -1) {
                    response = "404 NOT FOUND: There is no path to Kevin Bacon";
                    sendString(request, response, 404);
                    return;
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("baconNumber", baconNumber);

                JSONObject jsonObject = new JSONObject(map);
                response = jsonObject.toString(2);

                sendString(request, response, 200);
            }
            else if (path.equals("/api/v1/computeBaconPath")) {
                String actorId = entry.get("actorId");

                if (actorId == null || actorId.isEmpty()) {
                    response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
                    sendString(request, response, 400);
                    return;
                }

                if (!neo.checkId(actorId)) {
                    response = "404 NOT FOUND: Provided ID does not exist";
                    sendString(request, response, 404);
                    return;
                }

                if (actorId.equals("nm0000102")) {
                    List<String> baconPath = new ArrayList<>();
                    baconPath.add("nm0000102");

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("baconPath", baconPath);
                    response = jsonObject.toString(2);
                    sendString(request, response, 200);
                    return;
                }

                List<String> baconPath = neo.computeBaconPath(actorId);

                if (baconPath == null || baconPath.isEmpty()) {
                    response = "404 NOT FOUND: No path exists between actor and Kevin Bacon";
                    sendString(request, response, 404);
                    return;
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("baconPath", baconPath);
                response = jsonObject.toString(2);

                sendString(request, response, 200);
            }
            else if (path.equals("/api/v1/getMostActedName")) {
                String actorName = neo.getMostActedName();

                if (actorName == null) {
                    response = "404 NOT FOUND: No actor found";
                    sendString(request, response, 404);
                    return;
                }

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("name", actorName);

                JSONObject jsonObject = new JSONObject(map);
                response = jsonObject.toString(2);

                sendString(request, response, 200);
            }
            else {
                response = "500 Server Error";

                sendString(request, response, 500);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendString(request, "Server error", 500);
        }
	}

    //handle all the put method
    private void handlePut(HttpExchange request) throws IOException {

        URI uri = request.getRequestURI();
        String path = uri.getPath();
        String query = uri.getQuery();
        String response = "";
        if (query == null) {
            response = "400 BAD REQUEST: Missing required information";
            sendString(request, response, 400);
            return;
        }
        try {
	        if (path.equals("/api/v1/addActor")) {
	            Map<String, String> entry = splitQuery(query);
	
	            // Info formatted
	            if (!entry.containsKey("name") || entry.get("name").isEmpty() ||
	                !entry.containsKey("actorId") || entry.get("actorId").isEmpty()) {
	                response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
	                
	                sendString(request, response, 400);
	                return;
	            }
	
	            // Check duplicates
	            if (neo.checkId(entry.get("actorId"))) {
	                // Respond with a 400 Bad Request as the actorId must be unique
	                response = "400 BAD REQUEST: ActorId already exists";
	            	sendString(request, response, 400);
	                return;
	            }
	
	            response = "200 OK";
	
	            neo.addActor(entry.get("name"), entry.get("actorId"));
	            sendString(request, response, 200);
	        }
	        else if (path.equals("/api/v1/addMovie")) {
	            Map<String, String> entry = splitQuery(query);
	
	            // Info formatted
	            if (!entry.containsKey("name") || entry.get("name").isEmpty() ||
	                !entry.containsKey("movieId") || entry.get("movieId").isEmpty()) {
	                response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
	            	sendString(request, response, 400);
	                return;
	            }
	
	            // Check duplicates
	            if (neo.checkMovieId(entry.get("movieId"))) {
	                // Respond with a 400 Bad Request as the actorId must be unique
	                response = "400 BAD REQUEST: MovieId already exists";
	            	sendString(request, response, 400);
	                return;
	            }
	
	            response = "200 OK"; // Example response for success
	
	            neo.addMovie(entry.get("name"), entry.get("movieId"));
	            sendString(request, response, 200);
	        }
	        else if(path.equals("/api/v1/addRelationship")) {
	            Map<String, String> entry = splitQuery(query);
	
	            // Info formatted
	            if (!entry.containsKey("actorId") || entry.get("actorId").isEmpty() ||
	                !entry.containsKey("movieId") || entry.get("movieId").isEmpty()) {
	                response = "400 BAD REQUEST: Request body is improperly formatted or missing required information";
	            	sendString(request, response, 400);
	                return;
	            }
	
	            //check existence
	            if (!neo.checkMovieId(entry.get("movieId"))) {
	                response = "404 NOT FOUND: MovieId not found";
	            	sendString(request, response, 404);
	                return;
	            }
	            else if(!neo.checkId(entry.get("actorId"))){
	                response = "404 NOT FOUND: actorId not found";
	                
	                
	            	sendString(request, response, 404);
	                return;
	            }
	            // Check duplicate relationship
	            if (neo.checkRelationship(entry.get("actorId"), entry.get("movieId"))) {
	                response = "400 BAD REQUEST: Relationship already exists";
	            	sendString(request, response, 400);
	                return;
	            }

	            
	            
	            response = "200 OK"; // Example response for success
	
	            neo.addRelationship(entry.get("actorId"), entry.get("movieId"));
	            sendString(request, response, 200);
	        }
	        
        } catch (Exception e) {
        	e.printStackTrace();
        	sendString(request, "Server error\n", 500);
        }
    }
	
    //starter code
    // use for extracting query params
    public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    	        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    	        String[] pairs = query.split("&");
    	        for (String pair : pairs) {
    	            int idx = pair.indexOf("=");
    	            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    	        }
    	        return query_pairs;
    	    }
    //starter code
	private void sendString(HttpExchange request, String data, int restCode) 
			throws IOException {
		request.sendResponseHeaders(restCode, data.length());
        OutputStream os = request.getResponseBody();
        os.write(data.getBytes());
        os.close();
	}

    //starter code
	// one possible option for extracting JSON body as String
    public static String convert(InputStream inputStream) throws IOException {
                
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
    //starter code
    // another option for extracting JSON body as String
    public static String getBody(HttpExchange he) throws IOException {
                InputStreamReader isr = new InputStreamReader(he.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            
            int b;
            StringBuilder buf = new StringBuilder();
            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }

            br.close();
            isr.close();
	    
        return buf.toString();
        }
}