import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class Unigram
{
    public Set<String> samples;
    public HashMap<String, Double> counts;
    public int totalCount;
    public final String START = ":S";
    
    public static void main(String[] args)
    {
		NgramParser p = new NgramParser("data/fbistrain.xml");
        HashSet<String> set = p.parse();
        Unigram u = new Unigram(set);
        u.train();
        //u.showCounts();
        //u.smoothing();
        //u.showCounts();
        System.out.println("P(year) = " + u.unsmoothedProbability("year"));
        //System.out.println(u.getSentence());

        NgramParser test = new NgramParser("data/fbistest.xml");
        HashSet<String> testset = test.parse();
        System.out.println(u.perplexity(testset));
    }
    
    public Unigram(Set<String> samples)
    {
        this.samples = samples;
        this.counts = new HashMap<String, Double>();
        this.totalCount = 0;
    }
    
    public void train()
    {
        // Regexp to match words (starting with optional apos) or any punctuation (with probably extra escaping)
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);
        for (String sample : samples) {
            Matcher matcher = pattern.matcher(sample);
            while (matcher.find()) {
                String match = matcher.group();
                
                double count = 0;
                if (counts.containsKey(match)) {
                    count = counts.get(match);
                }
                counts.put(match, count+1);
                
                totalCount++;
            }
        }
    }
    
    public double unsmoothedProbability(String word)
    {
        if (counts.containsKey(word)) {
            return (double)counts.get(word) / (double) totalCount;
        } else {
            return 0.0;
        }
    }
    
    public void showCounts()
    {
        for (String key : counts.keySet()) {
            System.out.println(key + ": " + counts.get(key));
        }
    }

    public String getSentence() {
        String sentence = "";
        String currentWord = START;
        Set<String> keySet = counts.keySet();
        //creates a sentence until a period is found
        //(400 is jic it doesn't find a period)
        while (!currentWord.equals(".") && sentence.length() <= 400) {
            // rand is like a random dart thrown onto a dart board
            // multiplied by totalCount for precision (since P(word) is small)
            double rand = Math.random() * totalCount;
            Iterator<String> i = keySet.iterator();
            //looking at all the words to see where the dart lands
            while (i.hasNext() && rand >= 0) {
                currentWord = i.next();
                rand -= (double) counts.get(currentWord);
            }
            sentence += currentWord + " ";
        }
        return sentence;
    }

    public double perplexity(Set<String> testSamples) {
        float product = 1;
        int wordCount = 0;
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);

        // counting number of words in test set
        for (String sample : testSamples) {
            Matcher matcher = pattern.matcher(sample);
            while (matcher.find()) {
                String match = matcher.group();
                wordCount++;
                if (unsmoothedProbability(match) == 0.0) {
                    counts.put(match, 0.0);
                }
            }
        }

        // computing the necessary exponent
        double power = 1.0 / wordCount;
        
        simplesmoothing();

        // computing perplexity based on smoothed probabilities
        for (String sample : testSamples) {
            Matcher matcher = pattern.matcher(sample);
            while (matcher.find()) {
                String match = matcher.group();
                product *= Math.pow(unsmoothedProbability(match), power);
            }
        }
        return 1 / product;
    }
    
    public void simplesmoothing(){
        for(String key : counts.keySet()){
            Double count = counts.get(key);
            count++;
            totalCount++;
            counts.put(key, count);
        }
    }
    
    public void smoothing(){
        Collection<Double> collection = counts.values();
        HashMap<Double, Integer> map = new HashMap<Double, Integer>();
        //counting how many ngrams appear c times for each double c in the counts collection
        for(Double key: collection){
            if(map.containsKey(key)){
                int value = map.get(key);
                value++;
                map.put(key, value);
            } else {
                map.put(key, 1);
            }
        }
        HashMap<Double, Double> newcounts = new HashMap<Double, Double>();
        
        //creates a sorted list with no duplicates
        Collection<Double> noDups = new HashSet<Double>(collection);
        ArrayList<Double> keys = new ArrayList<Double>();
        keys.addAll(noDups);
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
                //TODO: use logs for precision
                newcounts.put(key, key2*((double)cplus1/c));
            }
            else{
                newcounts.put(key, key);
            }
        }

        //switching out old counts for new counts
        for(String key : counts.keySet()){
            Double count = counts.get(key);
            counts.put(key, newcounts.get(count));            
        }
    }

}