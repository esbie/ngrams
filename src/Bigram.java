import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;

public class Bigram
{
    public Set<String> samples;
    public HashMap<String, HashMap<String, Double>> counts;
    public HashMap<String, Double> unigramCounts;
    public final String START = ":S";
    
    public static void main(String[] args)
    {
		NgramParser p = new NgramParser("data/fbistrain.xml", true);
        HashSet<String> set = p.parse();
        Bigram b = new Bigram(set);
        b.train();
        // b.showCounts();
        System.out.println("P(a year) = "
                + b.unsmoothedProbability("a", "year"));
        System.out.println(b.getSentence());
        //b.simpleSmoothing();
        System.out.println("P(a year) = "
                + b.unsmoothedProbability("a", "year"));

        NgramParser test = new NgramParser("data/fbistest.xml");
        HashSet<String> testset = test.parse();
        System.out.println(b.perplexity2(testset));
    }
    
    public Bigram(Set<String> samples)
    {
        this.samples = samples;
        this.counts = new HashMap<String, HashMap<String, Double>>();
        this.unigramCounts = new HashMap<String, Double>();
    }
    
    public void train()
    {
        // Regexp to match words (starting with optional apos) or any punctuation (with probably extra escaping)
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);
        for (String sample : samples) {
            Matcher matcher = pattern.matcher(sample);
            String previousWord = START; // originally set to beginning-of-sentence marker
            while (matcher.find()) {
                // Set unigram counts (for word1)
                double unigramCount = 0;
                if (unigramCounts.containsKey(previousWord)) {
                    unigramCount = unigramCounts.get(previousWord);
                }
                unigramCounts.put(previousWord, unigramCount+1);
                
                // Get the new match (word2)
                String match = matcher.group();
                
                // Get access to (or create) the count map for word1.
                HashMap<String, Double> innerCounts;
                if (counts.containsKey(previousWord)) {
                    innerCounts = counts.get(previousWord);
                } else {
                    innerCounts = new HashMap<String, Double>();
                    counts.put(previousWord, innerCounts);
                }
                
                // Set bigram counts
                double count = 0;
                if (innerCounts.containsKey(match)) {
                    count = innerCounts.get(match);
                }
                innerCounts.put(match, count+1);
                
                // Update previousWord
                previousWord = match;
            }
        }
    }
    
    public double unsmoothedProbability(String word1, String word2)
    {
        if (counts.containsKey(word1)) {
            if (counts.get(word1).containsKey(word2)) {
                return (double) counts.get(word1).get(word2) / unigramCounts.get(word1);
            } else {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }
    
    public void showCounts()
    {
        for (String word1 : counts.keySet()) {
            for (String word2 : counts.get(word1).keySet()) {
                System.out.println(word1 + " " + word2 + ": " + counts.get(word1).get(word2));
            }
        }
    }

    public String getSentence() {
        String sentence = "";
        String currentWord = START;
        String nextWord = START;
        //creates a sentence until a period is found
        //(400 is jic it doesn't find a period)
        while (!currentWord.equals(".") && sentence.length() <= 400) {
            Set<String> keySet = counts.get(currentWord).keySet();
            // rand is like a random dart thrown onto a dart board
            // multiplied by totalCount for precision (since P(word) is small)
            double rand = Math.random() * unigramCounts.get(currentWord);
            Iterator<String> i = keySet.iterator();
            //looking at all the words to see where the dart lands
            while (i.hasNext() && rand >= 0) {
                nextWord = i.next();
                rand -= (double) counts.get(currentWord).get(nextWord);
            }
            currentWord = nextWord;
            sentence += nextWord + " ";
        }
        return sentence;
    }
    
    public double perplexity2(Set<String> testSamples) {
        float product = 1;
        int wordCount = 0;
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);

        int j = 0;
        // counting number of words in test set
        // adding previously unseen words/bigrams to the hashmap with count 0
        for (String sample : testSamples) {
            Matcher matcher = pattern.matcher(sample);
            String previousWord = START;
            while (matcher.find()) {
                String match = matcher.group();
                wordCount++;
                if (counts.containsKey(previousWord)) {
                    if (!counts.get(previousWord).containsKey(match)) {
                        counts.get(previousWord).put(match, 0.0);
                    }
                } else {
                    HashMap<String, Double> innerMap = new HashMap<String, Double>();
                    innerMap.put(match, 0.0);
                    counts.put(previousWord, innerMap);
                    unigramCounts.put(previousWord, 0.0);
                }
                // Update previousWord
                previousWord = match;
            }
            j++;
            System.out.println(j+" of "+testSamples.size()+" iterations");
        }

        // computing the necessary exponent
        double power = 1.0 / wordCount;
        System.out.println("now smoothing");        
        simpleSmoothing();
        System.out.println("now computing perplexity");
        // computing perplexity based on smoothed probabilities
        for (String sample : testSamples) {
            Matcher matcher = pattern.matcher(sample);
            String previousWord = START;
            while (matcher.find()) {
                String match = matcher.group();
                product *= Math.pow(unsmoothedProbability(previousWord, match), power);
                // Update previousWord
                previousWord = match;
            }
        }
        return 1 / product;
    }
    
    public void simpleSmoothingBroken(){
        int j = 0;
        Collection<String> keySet = counts.keySet();
        for(String word1 : keySet){
            System.out.println("word1 "+word1);
            if (!counts.containsKey(word1)) {
                System.out.println("created new Map");
                counts.put(word1, new HashMap<String,Double>());
            }
            if(!unigramCounts.containsKey(word1)){
                System.out.println("created new unigram count");
                unigramCounts.put(word1, 0.0);
            }
            Double totalCount = unigramCounts.get(word1);
            for(String word2 : keySet){
                System.out.println("    word1 "+word1+" word2 "+word2);
                if (!counts.get(word1).containsKey(word2)) {
                    counts.get(word1).put(word2, 0.0);                        
                }
                
                Double count = counts.get(word1).get(word2);
                System.out.println("checkpoint");
                count++;
                totalCount++;
                System.out.println("checkpoint2");
                counts.get(word1).put(word2, count);
                System.out.println("    count "+count+" totalCount "+totalCount);
            }
            unigramCounts.put(word1, totalCount);
            j++;
            System.out.println(j+" of "+keySet.size()+" smoothing iterations");
        }
    }
    
    public void simpleSmoothing(){
        Collection<String> keySet = counts.keySet();
        for(String word1 : keySet){
            if (counts.containsKey(word1) && unigramCounts.containsKey(word1)) {
                Double totalCount = unigramCounts.get(word1);
                for(String word2 : counts.get(word1).keySet()){
                    Double count = counts.get(word1).get(word2);
                    count++;
                    totalCount++;
                    counts.get(word1).put(word2, count);
                }
                unigramCounts.put(word1, totalCount);
            }
        }
    }

    public double perplexity(Set<String> testSamples) {
        float product = 1;
        int wordCount = 0;
        Stack<Double> products = new Stack<Double>();
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);

        // counting number of words in test set
        for (String sample : testSamples) {
            Matcher matcher = pattern.matcher(sample);
            String previousWord = START;
            while (matcher.find()) {
                String match = matcher.group();
                if (unsmoothedProbability(previousWord, match) > 0) {
                    products.push(unsmoothedProbability(previousWord, match));
                    wordCount++;
                }
                // Update previousWord
                previousWord = match;
            }
        }

        // computing the necessary exponent
        double power = 1.0 / wordCount;

        // computing perplexity based on probabilities
        while (!products.empty()) {
            product *= Math.pow(products.pop(), power);
        }
        return 1 / product;
    }
    
    public void smoothing(){
        Collection<HashMap<String, Double>> collection = counts.values();
        HashMap<Double, Integer> map = new HashMap<Double, Integer>();
        //counting how many ngrams appear c times for each double c in the counts collection
        for(HashMap<String, Double> keymap: collection){
            for(Double key : keymap.values()){
                if(map.containsKey(key)){
                    int value = map.get(key);
                    value++;
                    map.put(key, value);
                } else {
                    map.put(key, 1);
                }
            }
        }
        HashMap<Double, Double> newcounts = new HashMap<Double, Double>();
        ArrayList<Double> keys = new ArrayList<Double>();
        //creates a sorted list with no duplicates
        for (HashMap<String, Double> h : collection){
            Collection<Double> noDups = new HashSet<Double>(h.values());
            keys.addAll(noDups);
        }
        
        Collections.sort(keys);
        //creating lookup table for adjusted counts
        for(int i=0; i<keys.size(); i++){
            Double key = keys.get(i);
            //check: does keys[i+1] exist
            if(i != (keys.size() - 1)){
                Double key2 = keys.get(i+1);
                //number of ngrams that occur c times
                int c = map.get(key);
                //number of ngrams that occur c+1 times
                int cplus1 = map.get(key2);
                //this is the actual computation of good-turing smoothing
                newcounts.put(key, key2*((double)cplus1/c));
            }
            else{
                newcounts.put(key, key);
            }
        }

        //switching out old counts for new counts
        for(String key : counts.keySet()){
            for(String key2 : counts.get(key).keySet()){
                Double count = counts.get(key).get(key2);
                counts.get(key).put(key2, newcounts.get(count));      
            }
        }
    }
}