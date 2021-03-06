package mining;

import core.Tag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.*;

public class ImportCSV {

	private List<String> lines;

	public List<String> importCSV(String data) {
		lines = new ArrayList<String>();
		BufferedReader br = null;

		try {
			String line;
			// From tags_cleaned.csv
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(data), "UTF8"));

			while ((line = br.readLine()) != null) {
				lines.add(Normalizer.normalize(line, Normalizer.Form.NFC));
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	public List<Tag> importTags(String data) {
		List<String> lines;
		String[] temp;

		lines = importCSV(data);

		List<Tag> tags = new ArrayList<Tag>();

		for (String l : lines) {
			temp = l.split(",");

			tags.add(new Tag(0, temp[0], temp[1], Double.parseDouble(temp[2]), 0, 0));

		}
		return tags;
	}
}