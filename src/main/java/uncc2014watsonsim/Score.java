package uncc2014watsonsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uncc2014watsonsim.scorers.Merge;

/**
 * Represent how to create and merge a score.
 * This is mostly autogenerated.
 * @author Sean
 */
final class Meta implements Comparable<Meta> {
	public final String name;
	public final double default_value;
	public final Merge merge_type;
	public Meta(String name, double default_value, Merge merge_type) {
		this.name = name;
		this.default_value = default_value;
		this.merge_type = merge_type;
	}
	@Override
	public int compareTo(Meta o) {
		if (o == null) return 0;
		return o.name.compareTo(name);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Meta other = (Meta) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
}
/**
 * Namespace for managing score vectors.
 * 
 * The score vectors are designed to be memory efficient.
 * So they have no objects or pointers; only primitives.
 * You can manage them using static methods in this class.
 * @author Sean
 */
public class Score {
	/**
	 * Returns a convenient copy of scores as a map.
	 * @param scores
	 * @return
	 */
	public static Map<String, Double> asMap(double[] scores) {
		String[] version = versions.get(scores.length);
		Map<String, Double> map = new HashMap<>();
		for (int i=0; i<version.length; i++) {
			map.put(version[i], scores[i]);
		}
		return map;
	}
	
	/**
	 * Get a "blank" vector (all defaults)
	 */
	public static double[] empty() {
		double[] scores = new double[metas.size()];
		int i = 0;
		for (Meta m : metas.values()) {
			scores[i++] = m.default_value;
		}
		return scores;
	}
	
	/**
	 * Get a specific score
	 * 
	 * @param scores	The score vector
	 * @param name		The name of the score
	 */
	public static double get(double[] scores, String name, double otherwise) {
		String[] version = versions.get(scores.length);
		int index = Arrays.binarySearch(version, name);
		if (index >= 0) {
			return scores[index];
		} else {
			return otherwise;
		}
	}
	/**
	 * Get a bunch of scores in a new order.
	 * There is no going back!
	 * You can't get() or set() or update() the output of this function!
	 * @param incoming
	 * @param names
	 * @return
	 */
	public static double[] getEach(double[] incoming, List<String> names) {
		double[] outgoing = new double[names.size()];
		for (int i=0; i<names.size(); i++)
			outgoing[i] = get(incoming, names.get(i), -1);
		return outgoing;
	}
	
	/**
	 * @return a copy of the latest schema (the names)
	 */
	public static String[] latestSchema() {
		return versions.get(versions.size()-1).clone();
	}
	
	/**
	 * Merge two scores
	 */
	public static double[] merge(double[] left, double[] right) {
		left = update(left);
		right = update(right);
		double[] center = new double[left.length];
		int i = 0;
		for ( Meta m : metas.values() ) {
			if (m.merge_type == Merge.Mean) {
				center[i] = left[i] + right[i] / 2;
			} else if (m.merge_type == Merge.Or) {
				center[i] = left[i] + right[i] > 0 ? 1.0 : 0.0;
			} else if (m.merge_type == Merge.Min) {
				center[i] = Math.min(left[i], right[i]);
			} else if (m.merge_type == Merge.Max) {
				center[i] = Math.max(left[i], right[i]);
			}
			i++;
		}
		return center;
	}
	
	/** Register the answer score for automatically generated model data
	 * 
	 * This function is idempotent.
	 * @param name		The name of the score as it will be presented to Weka
	 * @param default_value		What the value of the score should be if it is missing
	 * @param merge_mode		How to merge two scores of the same name
	 */
	public synchronized static void register(String name,
			double default_value,
			Merge merge_mode) {
		if (!metas.containsKey(name)) {
			metas.put(name,
					new Meta(name, default_value, merge_mode));
			// This is a new entry
			// We have to make it an array because otherwise editing metas will
			// cause all the versions to change
			versions.add(metas.keySet().toArray(new String[versions.size()]));
		}
	}
	
	/**
	 * Set a specific score, possibly updating it in the process
	 * @param scores	The score vector
	 * @param name		The name of the score
	 * @param value		The value to set the score to
	 */
	public static double[] set(double[] scores, String name, double value) {
		scores = update(scores);
		set_(scores, name, value);
		return scores;
	}
	
	/**
	 * Set a specific score in-place (set() makes a copy)
	 * @param scores	The score vector
	 * @param name		The name of the score
	 * @param value		The value to set the score to
	 */
	private static boolean set_(double[] scores, String name, double value) {
		String[] version = versions.get(scores.length);
		int index = Arrays.binarySearch(version, name);
		if (index >= 0) {
			scores[index] = value;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Convert the given scores into the latest schema.
	 * @param scores	The scores array in any previous version
	 * @return			A new scores array in the latest version
	 */
	public static double[] update(double[] scores) {
		int latest_length = versions.size()-1;
		if (scores.length == latest_length)
			// It's already up to date
			return scores;
		
		// This looks like merge sort
		// but old_schema is guaranteed to be a subset of new_schema
		String[] old_schema = versions.get(scores.length);
		String[] new_schema = versions.get(latest_length);
		double[] scores_out = new double[latest_length];
		int oi = 0;
		for (int ni=0; ni < latest_length; ni++)
			if (new_schema[ni].equals(old_schema[oi]))
				// They both have it, step forward
				scores_out[ni] = scores[oi++];
			else
				// The old schema is missing this one
				scores_out[ni] = metas.get(new_schema[ni]).default_value;
		return scores;
	}
	
	/*
	 * Note:
	 * You can only add score types, not delete them. So in a given run, you
	 * can unambiguously map the array length to a version/interpretation of
	 * that vector. This means you do not need to fudge the format to make it
	 * work.
	 * The short of it: You can keep the dense, plain double[] and still evolve
	 * the schema for it.
	 */
	private static final List<String[]> versions = new ArrayList<>();
	
	// Lucene and Indri decide how many passages to retrieve for a candidate answer using this
	public static final int MAX_PASSAGE_COUNT = 50;
	
	private static final TreeMap<String, Meta> metas = new TreeMap<>();
	
	static {
		// This means the length of the incoming double[] is the same as
		// the index into versions[]. 
		versions.add(new String[0]);
	}
}
