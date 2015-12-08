package com.script.dbinit;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Team 3 aka "Team Pick Two"
 * DatabaseInitMain.java
 * Purpose: Populates the opportunities collection using the given .tsv that
 *          exists in the same directory as the jar.  Additionally, generates
 *          QR codes for each opportunity saved to the database.  Thos are also
 *          located in the same directory but in a folder 
 * 
 * @author arjunrao
 * @version 1.0 12/8/15
 */
public class DatabaseInitMain
{
    private static Logger LOG = LoggerFactory.getLogger(DatabaseInitMain.class);
    
    public static final int ONLY_ONE_HOST = 1;
    public static final int INDEX_OF_URI = 0;
    public static final int INDEX_OF_CSV = 1;
    public static final int HOST = 0;
    public static final int PORT = 1;
    public static String OUTPUT_PATH = "";
    
    /** main:   Connects to the mongoDB specified by args[0].  Parses the .tsv file
     *          specified by args[1].  Saves each opportunity to the databse and generates
     *          a QR code for each one saved.
     * 
     * @param args[0] mongoURI
     * @param args[1] e.g "opportunities.tsv"
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void main( String[] args ) throws IOException, URISyntaxException
    {
        //establish database connection
        DB db = connectToMongo(args);
        DBCollection opportunitiesCollection = db.getCollection("opportunities");
        
        // determine current working director
        URL location = DatabaseInitMain.class.getProtectionDomain().getCodeSource().getLocation();
        File file = new File(location.toURI());
        OUTPUT_PATH = file.getParentFile().getParent();
        
        ScriptFileReader scriptFileReader =
                new ScriptFileReader(OUTPUT_PATH + "/" + args[INDEX_OF_CSV]);
        
        List<HashMap<String, Object>> parsedOpportunities = scriptFileReader.read();
        
        for (HashMap<String, Object> parsedOpportunity : parsedOpportunities)
        {
            String opportunityId = (String)parsedOpportunity.get("opportunityId");
            if (LOG.isInfoEnabled())
            {
                ObjectMapper mapper = new ObjectMapper();
                String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedOpportunity);
                LOG.info("Processing opportunityId=" + opportunityId + "\n" + indented);
            }
            saveToDatabase(parsedOpportunity, opportunitiesCollection);
            // generate QR BitMatrix
            BitMatrix bitMatrix = generateQRCode(opportunityId);
            if (bitMatrix == null)
            {
                LOG.error("Failed to properly generate QR BitMatrix");
                continue;
            }
            // generateQRCodeImage
            generateQRCodeImage(bitMatrix, opportunityId);
        }
    }
    
    /** saveToDatabase: Helper method that saves the given parsedOpportunity to the given
     *                  database collection
     * 
     * @param parsedOpportunity
     * @param collection
     */
    private static void saveToDatabase(HashMap<String, Object> parsedOpportunity, DBCollection collection)
    {
        BasicDBObject opportunityObject = new BasicDBObject();
        Iterator<Entry<String, Object>> iterator = parsedOpportunity.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Object> field = (Map.Entry<String, Object>)iterator.next();
            opportunityObject.put(field.getKey(), field.getValue());
            iterator.remove();
        }
        if (!opportunityObject.containsField("otherRequirements"))
        {
            opportunityObject.put("otherRequirements", "");
        }
        collection.save(opportunityObject);
    }
    
    /** generateQRCode: Helper method that encodes the given opportunityId as a QR code.
     *                  Returns a BitMatrix to be turned into an image later.
     * 
     * @param opportunityId
     * @return BitMatrix
     */
    private static BitMatrix generateQRCode(String opportunityId)
    {
        ByteBuffer inputBuffer = ByteBuffer.wrap(opportunityId.getBytes());
        // decode UTF-8
        CharBuffer decodedString = Charset.forName("UTF-8").decode(inputBuffer);
        // encode ISO-8559-1
        ByteBuffer outputBuffer = Charset.forName("ISO-8859-1").encode(decodedString);

        String stringEncodedOpportunityId;
        try
        {
            stringEncodedOpportunityId = new String(outputBuffer.array(), "ISO-8859-1");
            int width = 200;
            int height = 200;
            Writer writer = new QRCodeWriter();
            try
            {
                if (LOG.isInfoEnabled())
                {
                    LOG.info("Encoding QR BitMatrix for opportunityId=" + opportunityId);
                }
                return writer.encode(stringEncodedOpportunityId, BarcodeFormat.QR_CODE, width, height);
            }
            catch (WriterException e)
            {
                e.printStackTrace();
                return null;
            }
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
            return null;
        }
    }
    
    /** generateQRCodeImage:    Helper method that generates an image with the given bitMatrix.
     *                          Outputs the image to QRCodes/opportunityId.png
     * 
     * @param bitMatrix
     * @param opportunityId
     */
    private static void generateQRCodeImage(BitMatrix bitMatrix, String opportunityId)
    {
        File qrCodesPath = new File(OUTPUT_PATH + "/QRCodes");
        qrCodesPath.mkdir();
        File file = new File(qrCodesPath + "/" + opportunityId + ".png");
        try {
            MatrixToImageWriter.writeToFile(bitMatrix, "PNG", file);
            if (LOG.isInfoEnabled())
            {
                LOG.info("printing to " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    /** connectToMongo: Helper method that establishes a connection to the mongo database
     * 
     * @param args
     * @return DB
     * @throws UnknownHostException
     */
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