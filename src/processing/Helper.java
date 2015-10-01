package processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import core.Tag;

public class Helper {
	
	   StringLengthComparator slc = new StringLengthComparator();
	  
	  public void removeTagsWithoutWords(List<Tag> tags)
	  {
		    // Remove tags with no words    
		    for(Iterator<Tag> iterator = tags.iterator(); iterator.hasNext();)
		    {
				Tag t = iterator.next();
				  
				if(t.getTagName().length() == 0)
				{
					iterator.remove();
				}
		    }
	  }
	  
	  public void removeBlacklistedWords(List<Tag> tags, List<String> blacklist)
	  {
		  String name, uptated;    
		  List<String> list = new ArrayList<String>();
		  PlainStringSimilarity psim = new PlainStringSimilarity();
		  
		  for(Tag tag: tags)
		  {
			  name = tag.getTagName();
			  uptated = "";
			  
			  list = psim.create_word_gram(name);
			  
			  list.removeAll(blacklist);
			  
			  for(String s: list)
			  {
				  uptated = uptated + " " + s;
			  }
			  
			  tag.setTagName(uptated.trim());
		  }
		  
		  removeTagsWithoutWords(tags);
	  }
	  
	  public void correctTagsAndIDs(List<Tag> data)
	  {
		  	// TagName: TagID
		  	Map<String, Integer> tags = new HashMap<String, Integer>();
		  	// "TrackID,TagName": LastFMWeight
		  	Map<String, Integer> song_name = new HashMap<String, Integer>();
		  	
	    	Set<String> used = new HashSet<String>();
		  	
		    int ID, weight;
		    String name, key;

		    // Find maximum LastFMWeight per song/tag pair
		    for(Tag t: data)
		    {
				ID = t.getSongID();
				name = t.getTagName();
				key = ID+name;
				weight = t.getLastFMWeight();

				if(song_name.containsKey(key))
				{
					if(weight > song_name.get(key))
					{
						song_name.put(key, weight);
					}
				}
				else
				{
					song_name.put(key, weight);
				}
		    }
		    
		    // Resolve multiple equal tags per song
		    for(Tag t: data)
		    {
				ID = t.getSongID();
				name = t.getTagName();
				key = ID+name;
				weight = t.getLastFMWeight();

				if(song_name.containsKey(key))
				{
					if(weight < song_name.get(key))
					{
						// This marks the tag object as removable
						t.setTagName("");
					}
					
					if(weight == song_name.get(key) && used.contains(key))
					{
						// This marks the tag object as removable
						t.setTagName("");
					}
					else if(weight == song_name.get(key))
					{
						used.add(key);
					}
				}
		    }
		    
		    // Resolve multiple tag ids
			for(Tag t:data)
			{
				ID = t.getTagID();
				name = t.getTagName();
				
				if(tags.containsKey(name))
				{
					if(ID != tags.get(name))
					{
						t.setTagID(tags.get(name));
					}
				}
				else
				{
					if(name.length()>0) tags.put(name, ID);
				}
			}	
	  }
	  
	  public Map<String, String> getImportantTags(List<Tag> tags, double threshold, int minWordLength)
	  {
		  Map<String, Double> important = new HashMap<String, Double>();
		  Map<String, Integer> tagid = new HashMap<String, Integer>();
		  List<String> temp = new ArrayList<String>();
		  Map<String, String> out = new LinkedHashMap<String, String>();
		  
		  for(Tag t: tags)
		  {
			  if(t.getImportance() >= threshold && t.getTagName().length() >= minWordLength)
			  {				  
				  important.put(t.getTagName(),t.getImportance());
				  tagid.put(t.getTagName(), t.getTagID());
				  temp.add(t.getTagName());
			  }
		  }
		  
		  Collections.sort(temp, slc);
		  
		  for(String l: temp)
		  {
			  if(tagid.get(l) != null)
			  {
				  out.put(l.replace(" ", "-"), tagid.get(l).toString()+","+important.get(l).toString());
			  }
			  else
			  {
				  out.put(l, "Additional Tag");
			  }
			  
		  }
		  
		  return out;
	  }
	  
	  public <T> String objectToJsonString(List<T> list)
	  {
		  	List<String> out = new ArrayList<String>();
		    
			//ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
			ObjectWriter ow = new ObjectMapper().writer();
			
			try {
				
				for(T t: list)
				{
					out.add(ow.writeValueAsString(t));
				}
				
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			
			return out.toString();
	  }
}
