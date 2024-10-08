package ca.yorku.eecs;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.json.JSONObject;
import org.neo4j.driver.v1.Record;

public class Neo4j {
	private Driver driver;
	private String uriDb;
	
	
	public Neo4j() {
		uriDb = "bolt://localhost:7687"; // may need to change if you used a different port for your DBMS
		Config config = Config.builder().withoutEncryption().build();
		
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","12345678"), config);
	}
	/*
	Adds a new actor to the database.
	Parameters:
	actor: The name of the actor to be added.
	id: The unique ID assigned to the actor.
	Returns:
	None.
	 */
	public void addActor(String actor, String id) {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> tx.run("MERGE (a:Actor {name: $actor, id: $id})",
				parameters("actor", actor, "id", id)));
			session.close();
		}
	}
	/*
	Adds a new movie to the database.
	Parameters:
	movie: The name of the movie to be added.
	id: The unique ID assigned to the movie.
	Returns:
	None.
	*/
	public void addMovie(String movie, String id) {
		try (Session session = driver.session()) {
			session.writeTransaction(tx -> tx.run("MERGE (a:Movie {name: $movie, id: $id})",
				parameters("movie", movie, "id", id)));
			session.close();
		}
	}
	/*
	Checks if a given actor ID exists in the database.
	Parameters:
	actorId: The unique ID of the actor.
	Returns:
	true if the ID exists, false otherwise.
	*/
	public boolean checkId(String actorId) {
		try (Session session = driver.session()) {
			return session.readTransaction(tx -> {
				StatementResult result = tx.run("MATCH (a:Actor {id: $actorId}) RETURN count(a) > 0 as exists", 
						parameters("actorId", actorId));
				return result.single().get("exists").asBoolean();
			});
		}
	}
	/*
	Checks if a given movie ID exists in the database.
	Parameters:
	movieId: The unique ID of the movie.
	Returns:
	true if the ID exists, false otherwise.
	*/
	public boolean checkMovieId(String movieId) {
		try (Session session = driver.session()) {
			return session.readTransaction(tx -> {
				StatementResult result = tx.run("MATCH (m:Movie {id: $movieId}) RETURN count(m) > 0 as exists", 
						parameters("movieId", movieId));
				return result.single().get("exists").asBoolean();
			});
		}
	}
	/*
	Adds a relationship between an actor and a movie.
	Parameters:
	actorId: The unique ID of the actor.
	movieId: The unique ID of the movie.
	Returns:
	None.
	*/
	public void addRelationship(String actorId, String movieId) {
		try (Session session = driver.session()) {
	        try (Transaction tx = session.beginTransaction()) {
	            StatementResult result = tx.run("MATCH (a:Actor {id: $actorId}), (m:Movie {id: $movieId}) " +
	                                            "MERGE (a)-[:ACTED_IN]->(m)",
	                                            parameters("actorId", actorId, "movieId", movieId));
	            tx.success(); // Commit the transaction
	        }
	    }
	}
	/*Checks if a relationship exists between an actor and a movie.
	Parameters:
	actorId: The unique ID of the actor.
	movieId: The unique ID of the movie.
	Returns:
	true if the relationship exists, false otherwise.
	*/
	public boolean checkRelationship(String actorId, String movieId) {
	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run(
	                "MATCH (a:Actor {id: $actorId})-[:ACTED_IN]->(m:Movie {id: $movieId}) RETURN count(a) > 0 as exists",
	                parameters("actorId", actorId, "movieId", movieId)
	            );
	            return result.single().get("exists").asBoolean();
	        });
	    }
	}
	/*
	Retrieves the name of an actor by their ID.
	Parameters:
	actorId: The unique ID of the actor.
	Returns:
	The name of the actor.
	*/
	public String getActorNameById(String actorId) {
	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run("MATCH (a:Actor {id: $actorId}) RETURN a.name AS name",
	                    parameters("actorId", actorId));
	            return result.hasNext() ? result.single().get("name").asString() : null;
	        });
	    }
	}
	/*
	Retrieves the name of the actor who has acted in the most movies.
	Parameters:
	None.
	Returns:
	The name of the actor.
	*/
	public String getMostActedName() {
	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run(
	                "MATCH (a:Actor)-[:ACTED_IN]->(m:Movie) " +
	                "RETURN a.id AS actorId, count(m) AS moviesCount " +
	                "ORDER BY moviesCount DESC " +
	                "LIMIT 1"
	            );

	            if (result.hasNext()) {
	                Record record = result.single();
	                String actorId = record.get("actorId").asString();
	                return getActorNameById(actorId);
	            }
	            return null;
	        });
	    }
	}
	/*
	Retrieves the movies acted by a specific actor.
	Parameters:
	actorId: The unique ID of the actor.
	Returns:
	A list of movie names.
	*/
	public List<String> getMoviesByActorId(String actorId) {
	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run("MATCH (a:Actor {id: $actorId})-[:ACTED_IN]->(m:Movie) RETURN m.id AS movieId",
	                    parameters("actorId", actorId));
	            List<String> movies = new ArrayList<>();
	            while (result.hasNext()) {
	                Record record = result.next();
	                movies.add(record.get("movieId").asString());
	            }
	            return movies;
	        });
	    }
	}
    /*
	Retrieves the actor IDs for a specific movie.
	Parameters:
	movieId: The unique ID of the movie.
	Returns:
	A list of actor IDs.
	*/
	public List<String> getActorIdsByMovieId(String movieId) {
	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run("MATCH (a:Actor)-[:ACTED_IN]->(m:Movie {id: $movieId}) RETURN a.id AS actorId",
	                    parameters("movieId", movieId));
	            List<String> actors = new ArrayList<>();
	            while (result.hasNext()) {
	                Record record = result.next();
	                actors.add(record.get("actorId").asString());
	            }
	            return actors;
	        });
	    }
	}
    /*
	Calculates the Bacon number for a specific actor.
	Parameters:
	actorId: The unique ID of the actor.
	Returns:
	An integer representing the actor's Bacon number.
	*/
	public int computeBaconNumber(String actorId) {
	    if (actorId.equals("nm0000102")) {
	        return 0;
	    }
	    
	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run(
	                "MATCH p=shortestPath((bacon:Actor {id: 'nm0000102'})-[:ACTED_IN*]-(actor:Actor {id: $actorId})) " +
	                "RETURN length(p)/2 AS baconNumber",
	                parameters("actorId", actorId));
	            return result.hasNext() ? result.single().get("baconNumber").asInt() : -1; 
	        });
	    }
	}
	/*
	Calculates the Bacon path for a specific actor.
	Parameters:
	actorId: The unique ID of the actor.
	Returns:
	A list of strings representing the path to Kevin Bacon.
	*/
	public List<String> computeBaconPath(String actorId) {
	    if (actorId.equals("nm0000102")) {
	        List<String> baconPath = new ArrayList<>();
	        baconPath.add("nm0000102");
	        return baconPath;
	    }

	    try (Session session = driver.session()) {
	        return session.readTransaction(tx -> {
	            StatementResult result = tx.run(
	                "MATCH p=shortestPath((bacon:Actor {id: 'nm0000102'})-[*]-(actor:Actor {id: $actorId})) " +
	                "UNWIND nodes(p) AS n " +
	                "RETURN CASE WHEN 'Actor' IN labels(n) THEN n.id END AS actorId, " +
	                "CASE WHEN 'Movie' IN labels(n) THEN n.id END AS movieId",
	                parameters("actorId", actorId));

	            List<String> baconPath = new ArrayList<>();
	            while (result.hasNext()) {
	                Record record = result.next();
	                if (record.get("actorId") != null && !record.get("actorId").isNull()) {
	                    baconPath.add(record.get("actorId").asString());
	                }
	                if (record.get("movieId") != null && !record.get("movieId").isNull()) {
	                    baconPath.add(record.get("movieId").asString());
	                }
	            }

	            return baconPath.isEmpty() ? null : baconPath;
	        });
	    }
	}


	
	
	
	public void close() {
		driver.close();
	}
}
