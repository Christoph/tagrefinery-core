package tags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.*;
import java.nio.file.Files;
import java.util.*;

public class ImportCSV {

  private List<String> lines;

public List<String> importCSV(String data)
  {
    lines = new ArrayList<>();
    try {
      File file = new File(data);
      lines = Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
    } catch (IOException e) { e.printStackTrace(); }
    
  

    return lines;
  }
  
  public List<Tag> importTags(String data)
  {
    List<String> lines;
    String[] temp;
    List<Tag> tags = new ArrayList<Tag>();
    
    lines = importCSV(data);
    
    for(String l: lines)
    {
    	temp = l.split(",");
    	
  		tags.add(new Tag(Integer.parseInt(temp[0]), temp[6].replace("\"", ""), Integer.parseInt(temp[4]), Integer.parseInt(temp[5]), Double.parseDouble(temp[8]), Integer.parseInt(temp[7]), Integer.parseInt(temp[1]),temp[2].replace("\"", ""), Integer.parseInt(temp[3]),Integer.parseInt(temp[9])));
   	}
    
    return tags;
  }
}