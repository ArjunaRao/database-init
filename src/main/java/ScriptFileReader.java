import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ScriptFileReader
{
    private String csvPath;
    
    public ScriptFileReader() {}
    
    public ScriptFileReader(String csvPath)
    {
        this.csvPath = csvPath;
    }
    
    public List<HashMap<String, Object>> read()
    {
        List<HashMap<String, Object>> parsedOpportunities = new ArrayList<HashMap<String, Object>>();
        BufferedReader bufferedReader = null;
        String line = "";
        String splitBy = "\t";
        
        try
        {
            bufferedReader = new BufferedReader(new FileReader(csvPath));
            // this loop runs once for each entry (line) in the csv
            while ((line = bufferedReader.readLine()) != null)
            {
                HashMap<String, Object> parsedOpportunity = new HashMap<String, Object>();
                List<String> topics = new ArrayList<String>();
                List<String> academicStanding = new ArrayList<String>();
                List<String> majors = new ArrayList<String>();
                List<String> learningOutcomes = new ArrayList<String>();
                List<String> skills = new ArrayList<String>();
                
                String[] opportunityUnparsed = line.split(splitBy);
                // for each field in the current opportunity
                for (String field : opportunityUnparsed)
                {
                   // split each field at the =
                   String[] fieldUnparsed = field.split("=");
                   // for the arrays present in the opportunities object
                   if (fieldUnparsed[0].equals("topic"))
                   {
                       topics.add(fieldUnparsed[1]);
                   }
                   else if (fieldUnparsed[0].equals("academicStanding"))
                   {
                       academicStanding.add(fieldUnparsed[1]);
                   }
                   else if (fieldUnparsed[0].equals("major"))
                   {
                       majors.add(fieldUnparsed[1]);
                   }
                   else if (fieldUnparsed[0].equals("learningOutcome"))
                   {
                       learningOutcomes.add(fieldUnparsed[1]);
                   }
                   else if (fieldUnparsed[0].equals("skill"))
                   {
                       skills.add(fieldUnparsed[1]);
                   }
                   // for all other fields, create a new entry in the HashMap
                   else
                   {
                       parsedOpportunity.put(fieldUnparsed[0], fieldUnparsed[1]);
                   }
                }
                parsedOpportunity.put("topics", topics);
                parsedOpportunity.put("academicStanding", academicStanding);
                parsedOpportunity.put("majors", majors);
                parsedOpportunity.put("learningOutcomes", learningOutcomes);
                parsedOpportunity.put("skills", skills);
                
                parsedOpportunities.add(parsedOpportunity);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        } 
        finally 
        {
            if (bufferedReader != null)
            {
                try
                {
                    bufferedReader.close();
                } 
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return parsedOpportunities;
    }
}