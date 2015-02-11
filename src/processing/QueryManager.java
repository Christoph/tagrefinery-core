package processing;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class QueryManager {
	// Initialize logger
	private Logger log = Logger.getLogger("Logger");
	
  // Need to be closed in the closeAll method
  private PreparedStatement deleteTrackWithLessThanTags;
  private PreparedStatement selectTagsWhichOccurMoreThan;

  // Constructor
  public QueryManager(Connection conn) {
    try {
      // Initialize preparedstatments
    	// Deletes all tracks with less than ? tags.
      deleteTrackWithLessThanTags = conn.prepareStatement("delete Track from Track inner join (select Track.ID,count(*) as tags from TT inner join Track on TT.TrackID = Track.ID group by Track.ID) as t on Track.ID = t.ID where tags < ?");
      selectTagsWhichOccurMoreThan = conn.prepareStatement("select * from Tag inner join (select TT.TagID,count(*) as n from Tag inner join TT on Tag.ID = TT.TagID group by TT.TagID) as t on Tag.ID = t.TagID Where n > ?");
      
    } catch (SQLException e) { 
    	log.severe("Error in the DB constructor."+e.getSQLState()+e.getMessage());
    	
    	e.printStackTrace(); 
    }
  }

  public void deleteTracksWithTagsLessThan(int number) throws SQLException
  {
      // Execute the query
      deleteTrackWithLessThanTags.setInt(1,number);
      deleteTrackWithLessThanTags.executeQuery();
  }
  
  public List<String> getTagsWhichOccurMoreThan(int times) throws SQLException {
  	List<String> data = new ArrayList<String>();
  	
  	selectTagsWhichOccurMoreThan.setInt(1,times);
  	ResultSet result = selectTagsWhichOccurMoreThan.executeQuery();
  	
  	while(result.next()) {
  		data.add(result.getString("Name"));
  	}
  	
  	return data;
  }
  
  public void closeAll() throws SQLException
  {
      // Close all prepared statements
      deleteTrackWithLessThanTags.close();
      selectTagsWhichOccurMoreThan.close();
  }
}
