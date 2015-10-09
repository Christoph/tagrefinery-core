package processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import core.TagMovie;
import core.TagsToCSV;

public class Weighting {

	public Weighting() {
		super();
	}

	public void byWeightedMean(List<TagMovie> tags, String prefix, Boolean verbose) {
	    /////////////////////////////////
	    // Variables
	    Map<String, Double> tag_words = new HashMap<String, Double>();
	    
	  	TagsToCSV writer;
	  	List<String> weights = new ArrayList<String>();
	  	Set<String> temp = new HashSet<String>();
	    
	
	    String key;
	    double importance, max_w = 0;
	    double value;
	    Boolean print_groups;
	    
		/////////////////////////////////
		// Configuration
		
		// Verbose
		print_groups = verbose;	
	
		/////////////////////////////////
		// Algorithm	
	    
	    // Summing up the weights
	    for(int i = 0;i < tags.size(); i++)
	    {	    	    		
			importance = tags.get(i).getImportance();
			key = tags.get(i).getTagName();
	
			if(tag_words.containsKey(key))
			{
				value = tag_words.get(key);
				
				// Sum up the weight over all songs
				tag_words.put(key, value + importance);
			}
			else
			{
				tag_words.put(key, importance);
			}
	    }
	    
	    // maximum importance of tag_words
	    for(String s: tag_words.keySet())
	    {
	    	if(tag_words.get(s) > max_w)
	    	{
	    		max_w = tag_words.get(s);
	    	}
	    }
	    
	    // normalizing tags
	    for(TagMovie t:tags)
	    {
	    	importance = tag_words.get(t.getTagName());
	    	
	    	t.setImportance(importance/max_w);
	    	
	    	weights.add(t.getTagName()+","+importance/max_w);
	    }
	    
	    // Write temp files
	    if(print_groups) 
		{	
	    	// Remove duplicates and sort
	    	temp.addAll(weights);
	    	
	    	weights.clear();
	    	
	    	weights.addAll(temp);
	    	Collections.sort(weights);
	    	
	    	writer = new TagsToCSV("importance_"+prefix+".csv");
	    	writer.writeLines(weights,"importance");
		}
		
	
	    tag_words = null;
	}

}