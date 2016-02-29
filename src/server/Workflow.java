package server;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.corundumstudio.socketio.SocketIOClient;

import core.json.gridHistory;
import processing.Composite;
import processing.Helper;
import processing.Postprocess;
import processing.Preprocess;
import processing.Spellcorrect;
import processing.Weighting;
import core.ImportCSV;
import core.Tag;
import core.json.gridOverview;

public class Workflow {
	
	// Initialize variables and classes
	private static final Logger log = Logger.getLogger("Logger");
	private Helper help = new Helper();
	private ImportCSV im = new ImportCSV();
	private Weighting weighting = new Weighting();

	private int count, packages;

	private Boolean simpleRun = false;
	private Boolean complexRun = false;


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Parameters
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Data
	private List<Tag> tags = new ArrayList<>();

	private Map<String, Long> tagsFreq = new HashMap<>();
	private Map<String, Double> vocabPre = new HashMap<>();
	private Map<String, Double> vocabPost = new HashMap<>();

	private List<String> whitelistWords = new ArrayList<String>();
	private List<String> whitelistGroups = new ArrayList<String>();
	private List<String> whitelistVocab = new ArrayList<String>();
	private List<String> blacklist = new ArrayList<String>();

	// Initialize pipeline steps
	// The index selects the working copy. 0 = original
	private Preprocess preprocess = new Preprocess(1, blacklist);
	private Spellcorrect spellcorrect = new Spellcorrect(2, whitelistWords, whitelistGroups, whitelistVocab);
	private Composite composite = new Composite(3);
	private Postprocess postprocess = new Postprocess(4);

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Load data - Dataset 0
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void applyImportedData(String json) {
		List<Map<String, Object>> map = help.jsonStringToList(json);
		String item, name, weight;
		
		if (packages < count) {
			packages++;

			for (Map<String, Object> aMap : map) {
				try {
					item = String.valueOf(aMap.get("item"));
					name = String.valueOf(aMap.get("tag"));
					weight = String.valueOf(aMap.get("weight"));

					tags.add(new Tag(item, name, Double.parseDouble(weight), 0));
				} catch (Exception e) {
					System.out.println(aMap);
				}
				
			}
		}
	}
	
	public void applyImportedDataFinished(SocketIOClient client) {
		// Set to lower case
		help.setToLowerCase(tags, 0);
		
		// Compute word frequency
		help.wordFrequency(tags, tagsFreq, 0);
		
		client.sendEvent("preFilterData", sendPreFilterHistogram());
		client.sendEvent("preFilterGrid", sendPreFilter());
	}
	
	public void applyImportedDataCount(int count) {
		tags.clear();
		
		this.count = count;
		this.packages = 0;
	}

	public void runAll(SocketIOClient client) {
		computePreprocessing(client);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Preprocessing - Dataset 1
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void computePreprocessing(SocketIOClient client) {
		help.resetStep(tags, 1);

		// Remove tags
		preprocess.applyFilter(tags, tagsFreq);
		
		// Remove characters
		preprocess.removeCharacters(tags);
		
		// Replace characters
		preprocess.replaceCharacters(tags);

		// Remove blacklisted words
		help.removeBlacklistedWords(tags, blacklist, 1);

		// Create preFilter vocab
		weighting.vocab(tags, vocabPre, 1);

		// Cluster words for further use
		spellcorrect.clustering(tags, vocabPre, whitelistVocab);
		
		client.sendEvent("similarities", sendSimilarityHistogram());
		client.sendEvent("vocab", sendVocab());
		client.sendEvent("importance", sendPreVocabHistogram());
		
		computeSpellCorrect(client);
	}
	
	// Apply changes
	public void applyPreFilter(int threshold, SocketIOClient client) {
		// Set threshold
		preprocess.setFilter(threshold);
		
		// Apply
		computePreprocessing(client);
	}
	
	public void applyPreRemove(String chars, SocketIOClient client) {
		// Set characters for removal
		preprocess.setRemove(chars);
		
		// Apply
		computePreprocessing(client);
	}
	
	public void applyPreReplace(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);
		
		preprocess.setReplace(map);
		
		// Apply
		computePreprocessing(client);
	}
	
	public void applyPreDictionary(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);
		
		preprocess.setDictionary(map);
		
		// Apply
		computePreprocessing(client);
	}
	
	// Send Params
	public double sendPreFilterParams() {
		return preprocess.getFilter();
	}
	
	public String sendPreRemoveParams() {
		return preprocess.getRemove();
	}
	
	public List<String> sendPreReplaceParams() {
		return preprocess.getReplace();
	}
	
	public List<String> sendPreDictionaryParams() {
		return blacklist;
	}
	
	// Send Data
	public String sendPreFilter() {
		return help.objectToJsonString(preprocess.preparePreFilter(tagsFreq));
	}
	
	public String sendPreFilterHistogram() {
		return help.objectToJsonString(preprocess.preparePreFilterHistogram(tagsFreq));
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Spell Correction - Dataset 2
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void computeSpellCorrect(SocketIOClient client) {
		// Reset current stage
		help.resetStep(tags, 2);

		// Apply clustering
		spellcorrect.applyClustering(tags, vocabPre);
		
		// Compute further data
		composite.group(tags);
		
		client.sendEvent("frequentGroups", sendFrequentGroups());
		client.sendEvent("frequentData", sendFrequentHistogram());
		client.sendEvent("uniqueGroups", sendUniqueGroups());
		client.sendEvent("uniqueData", sendUniqueHistogram());
		client.sendEvent("replacements", sendReplacements());

		computeGroups(client);
	}
	
	// Apply changes
	public void applySpellCorrect(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);

		spellcorrect.setSpellImportance((Double) map.get(0).get("importance"));
		spellcorrect.setSpellSimilarity((Double) map.get(0).get("similarity"));

		// Apply
		computeSpellCorrect(client);
	}

	public void applySpellImport(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);

		spellcorrect.setDictionary(map);

		// Apply
		computeSpellCorrect(client);
	}

	public void applySpellMinWordSize(int minWordSize, SocketIOClient client) {
		// Set minWordSize
		spellcorrect.setMinWordSize(minWordSize);

		// Apply
		computeSpellCorrect(client);
	}

	// Send Params
	public double sendSpellImportanceParams() {
		return spellcorrect.getSpellImportance();
	}
	
	public double sendSpellSimilarityParams() {
		return spellcorrect.getSpellSimilarity();
	}
	
	public int sendSpellMinWordSizeParams() {
		return spellcorrect.getMinWordSize();
	}

	public List<String> sendSpellDictionaryParams() {
		List<String> temp = new ArrayList<>();

		temp.addAll(whitelistWords);
		temp.addAll(whitelistGroups);

		return temp;
	}

	// Send Data
	public String sendCluster(String tag) {
		return help.objectToJsonString(spellcorrect.prepareCluster(tag, vocabPre));
	}
	
	public String sendSimilarityHistogram() {
		return help.objectToJsonString(spellcorrect.prepareSimilarityHistogram());
	}
	
	public int sendReplacements(String json) {
		List<Map<String, Object>> map = help.jsonStringToList(json);

		double imp, sim;

		if(map.get(0).get("importance").equals(0)) return 0;

		if(map.get(0).get("importance").equals(1))
		{
			imp = ((Integer) map.get(0).get("importance"));
		}
		else
		{
			imp = ((Double) map.get(0).get("importance"));
		}

		sim = getSim(map);

		return spellcorrect.prepareReplacements(sim, imp, vocabPre);
	}

	private double getSim(List<Map<String, Object>> map) {
		double sim;
		if(map.get(0).get("similarity").equals(0))
		{
			sim = ((Integer) map.get(0).get("similarity"));
		}
		else if(map.get(0).get("similarity").equals(1))
		{
			sim = ((Integer) map.get(0).get("similarity"));
		}
		else
		{
			sim = ((Double) map.get(0).get("similarity"));
		}
		return sim;
	}

	public String sendReplacementData(String json)
	{
		List<Map<String, Object>> map = help.jsonStringToList(json);

		double imp, sim;

		if(map.get(0).get("importance").equals(0)) return "";

		if(map.get(0).get("importance").equals(1))
		{
			imp = ((Integer) map.get(0).get("importance"));
		}
		else
		{
			imp = ((Double) map.get(0).get("importance"));
		}

		sim = getSim(map);

		return help.objectToJsonString(spellcorrect.prepareReplacementData(sim, imp, vocabPre));
	}
	
	public int sendReplacements() {
		double sim = spellcorrect.getSpellSimilarity();
		double imp = spellcorrect.getSpellImportance();

		return spellcorrect.prepareReplacements(sim, imp, vocabPre);
	}

	public String sendVocab() {
		return help.objectToJsonString(help.prepareVocab(vocabPre));
	}

	public String sendPreVocabHistogram() {
		return help.objectToJsonString(help.prepareVocabHistogram(vocabPre));
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Composites - Dataset 3
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void computeGroups(SocketIOClient client) {
		help.resetStep(tags, 3);
		
		// Compute all word groups
		composite.applyGroups(tags);

		// Provide further data
		weighting.vocab(tags, vocabPost, 3);

		client.sendEvent("postFilterGrid", sendPostVocab());
		client.sendEvent("postFilterData", sendPostVocabHistogram());
		
		client.sendEvent("output", sendOverview(3));
		
		prepareSalvaging(client);
	}
	
	// Apply changes
	public void applyCompositeFrequent(double threshold, SocketIOClient client) {
		// Set threshold
		composite.setFrequentThreshold(threshold);
		
		// Apply
		computeGroups(client);
	}
	
	public void applyCompositeUnique(double threshold, SocketIOClient client) {
		// Set threshold
		composite.setJaccardThreshold(threshold);
		
		// Apply
		computeGroups(client);
	}
	
	public void applyCompositeParams(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);

		composite.setMaxGroupSize((Integer) map.get(0).get("maxGroupSize"));
		composite.setMinOccurrence((Integer) map.get(0).get("minOcc"));
		composite.setSplit((Boolean) map.get(0).get("split"));

		// Apply
		computeGroups(client);
	}
	
	// Send Params
	public double sendCompFrequentParams() {
		return composite.getFrequentThreshold();
	}
	
	public double sendCompUniqueParams() {
		return composite.getJaccardThreshold();
	}
	
	public int sendCompSizeParams() {
		return composite.getMaxGroupSize();
	}
	
	public int sendCompOccParams() {
		return composite.getMinOccurrence();
	}

	public Boolean sendCompSplitParams() {
		return composite.getSplit();
	}

	// Send Data
	public String sendFrequentGroups() {
		return help.objectToJsonString(composite.prepareFrequentGroups());
	}
	
	public String sendUniqueGroups() {
		return help.objectToJsonString(composite.prepareUniqueGroups());
	}
	
	public String sendFrequentHistogram() {
		return help.objectToJsonString(composite.prepareFrequentHistogram());
	}
	
	public String sendUniqueHistogram() {
		return help.objectToJsonString(composite.prepareUniqueHistogram());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Postprocessing - Dataset 4
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void prepareSalvaging(SocketIOClient client) {
		postprocess.initializeSalvaging(vocabPost);
		
		client.sendEvent("postImportantWords", sendPostImportant());
		client.sendEvent("postSalvageWords", sendPostSalvage());

		setSimpleRun(true);
	}
	
	public void computeSalvaging(SocketIOClient client) {
		postprocess.computeSalvaging(vocabPost);
		
		client.sendEvent("postSalvageData", sendPostSalvageData());

		setComplexRun(true);
	}
	
	// Apply changes
	public void applySalvaging(SocketIOClient client) {
		help.resetStep(tags, 4);
		
		postprocess.applySalvaging(tags);
		
		client.sendEvent("output", sendOverview(4));
	}
	
	public void applyPostFilter(double threshold, SocketIOClient client) {
		// Set threshold
		postprocess.setPostFilter(threshold);
		
		// Apply
		prepareSalvaging(client);
	}
	
	public void applyPostReplace(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);
		
		postprocess.setPostReplace(map);
		
		// Apply
		prepareSalvaging(client);
	}

	public void applyPostParams(String json, SocketIOClient client) {
		List<Map<String, Object>> map = help.jsonStringToList(json);

		postprocess.setMinWordLength((Integer) map.get(0).get("minWordLength"));
		postprocess.setSplitTags((Boolean) map.get(0).get("split"));
		postprocess.setUseAllWords((Boolean) map.get(0).get("useAll"));

		// Apply
		prepareSalvaging(client);
	}
	
	// Send Params
	public double sendPostFilterParams() {
		return postprocess.getPostFilter();
	}
	
	public int sendPostLengthParams() {
		return postprocess.getMinWordLength();
	}
	
	public Boolean sendPostAllParams() {
		return postprocess.getUseAllWords();
	}
	
	public List<String> sendPostReplaceParams() {
		return postprocess.getPostReplace();
	}
	
	public Boolean sendPostSplitParams() {
		return postprocess.getSplitTags();
	}
	
	// Send Data
	public String sendPostVocab() {
		return help.objectToJsonString(help.prepareVocab(vocabPost));
	}
	
	public String sendPostVocabHistogram() {
		return help.objectToJsonString(help.prepareVocabHistogram(vocabPost));
	}
	
	public String sendPostImportant() {
		return help.objectToJsonString(postprocess.prepareImportantWords());
	}
	
	public String sendPostSalvage() {
		return help.objectToJsonString(postprocess.prepareSalvageWords());
	}
	
	public String sendPostSalvageData() {
		return help.objectToJsonString(postprocess.prepareSalvagedData());
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Header
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Boolean getSimpleRun() {
		return simpleRun;
	}

	public void setSimpleRun(Boolean simpleRun) {
		this.simpleRun = simpleRun;
	}

	public Boolean getComplexRun() {
		return complexRun;
	}

	public void setComplexRun(Boolean complexRun) {
		this.complexRun = complexRun;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Overview
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String sendOverview(int index) {
		Supplier<List<gridOverview>> supplier = ArrayList::new;

		List<gridOverview> tags_filtered = tags.stream()
				.map(p -> new gridOverview(p.getTag(index), p.getItem(), p.getImportance()))
				.collect(Collectors.toCollection(supplier));

		return help.objectToJsonString(tags_filtered);
	}

	public String sendHistory(String json) {
		List<Map<String, Object>> map = help.jsonStringToList(json);

		String tag = (String) map.get(0).get("tag");
		String item = (String) map.get(0).get("item");

	    Supplier<List<gridHistory>> supplier = () -> new ArrayList<>();

	    List<gridHistory> tags_filtered = tags.stream()
	    		.filter(p -> p.getTag().contains(tag) && p.getItem().contains(item) )
	    		.map(p -> new gridHistory(p.getTag()))
	    		.collect(Collectors.toCollection(supplier));

	    return help.objectToJsonString(tags_filtered.subList(0,1));
	}
}
