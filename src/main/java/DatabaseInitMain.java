import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

public class DatabaseInitMain
{
    public static final int ONLY_ONE_HOST = 1;
    public static final int INDEX_OF_URI = 0;
    public static final int INDEX_OF_CSV = 1;
    public static final int HOST = 0;
    public static final int PORT = 1;
    
    
    public static void main( String[] args ) throws IOException, URISyntaxException
    {
        //establish database connection
        DB db = connectToMongo(args);
        DBCollection opportunitiesCollection = db.getCollection("opportunities");
        
        // determine current working director
        URL location = DatabaseInitMain.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(location.toURI());
        String pathContainingJar = file.getParentFile().getParent();
        
        ScriptFileReader scriptFileReader =
                new ScriptFileReader(pathContainingJar + "/" + args[INDEX_OF_CSV]);
        
        List<HashMap<String, Object>> parsedOpportunities = scriptFileReader.read();
        
//        ObjectMapper mapper = new ObjectMapper();
//        String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedOpportunities.get(0));
//        System.out.println(indented);
        
        for (HashMap<String, Object> parsedOpportunity : parsedOpportunities)
        {
            saveToDatabase(parsedOpportunity, opportunitiesCollection);
            // generate QR code
        }
    }
    
    private static void saveToDatabase(HashMap<String, Object> parsedOpportunity, DBCollection collection)
    {
        BasicDBObject opportunityObject = new BasicDBObject();
        Iterator<Entry<String, Object>> iterator = parsedOpportunity.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Object> field = (Map.Entry<String, Object>)iterator.next();
            opportunityObject.put(field.getKey(), field.getValue());
            iterator.remove();
        }
        collection.save(opportunityObject);
    }
    
    private void generateQRCode(String opportunityId)
    {
        
    }
    
    private static DB connectToMongo(String[] args) throws UnknownHostException
    {
        if (args.length == 0)
        {
            MongoClient mongoClient = new MongoClient(new ServerAddress("localhost",27017));
            return mongoClient.getDB("api-test");
        }
        else
        {
            MongoClientURI mongoClientURI = new MongoClientURI(args[INDEX_OF_URI]);
            List<String> hosts = mongoClientURI.getHosts();
            String[] hostPort = hosts.get(0).split(":");
            MongoCredential credential = mongoClientURI.getCredentials();
            MongoClient mongoClient;
            if (credential == null)
            {
                if (hostPort.length == ONLY_ONE_HOST)
                {
                    mongoClient = new MongoClient(new ServerAddress(hostPort[HOST], 27017));
                }
                else 
                {
                    mongoClient = new MongoClient(new ServerAddress(hostPort[HOST], Integer.parseInt(hostPort[PORT])));
                }

                if (mongoClientURI.getDatabase() != null)
                {
                    return mongoClient.getDB(mongoClientURI.getDatabase());
                }
                else 
                {
                    throw new RuntimeException("No database specified - please specify which database you want the"
                            + " script to run on");
                }
            }
            mongoClient = new MongoClient(new ServerAddress(hostPort[0], Integer.parseInt(hostPort[1])),
                    Arrays.asList(credential));
            return mongoClient.getDB(mongoClientURI.getDatabase());
        }
    }
}
