package tags;

import java.util.Collection;
import java.util.List;

import de.umass.lastfm.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.*;

public class Core {

  private static final Logger log = Logger.getLogger("Logger");

  public static void main(String[] args) {
  // Initializing the logger
	Handler handler;

	try {
      handler = new FileHandler( "log.txt" );
      handler.setFormatter(new SimpleFormatter());
      log.addHandler( handler );
    } catch (SecurityException e1) { e1.printStackTrace();
    } catch (IOException e1) { e1.printStackTrace(); }
	
    log.info("Initializing");

    // Initializing variables
    Collection<Tag> tags;
    String[] line;
    List<String> lines;
    InputStream input = null;
    int counter = 0;

    // Load config files
    Properties dbconf = new Properties();
    try {

      input = new FileInputStream("config.db");
      dbconf.load(input);

    } catch (IOException e) { e.printStackTrace(); }

    // Initializing classes 
    DB db = new DB(dbconf);
    LastFM last = new LastFM(dbconf);
    ImportCSV data = new ImportCSV();

    log.info("Import");
    
    // Import data
    lines = data.importCSV();
    
    for(String l :lines)
    {
    counter++;
    
    if(counter%100 == 0)
    {
    	log.info("Imported "+counter+" rows.");
    }
    
    line = l.split(",");

    tags = last.mineTags(line[0].trim(), line[1].trim());
    db.insert(line[0].trim(),line[1].trim(),tags);
    
    // Wait to stay below 5 cals per second.
    try { Thread.sleep(250); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    // Close all
    db.closeAll();

    log.info("End");
  }
}
