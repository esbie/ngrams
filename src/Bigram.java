import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;

public class Bigram
{
    public Set<String> samples;
    public HashMap<String, HashMap<String, Integer>> counts;
    public HashMap<String, Integer> unigramCounts;
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

        NgramParser test = new NgramParser("data/fbistest.xml");
        HashSet<String> testset = test.parse();
        System.out.println(b.perplexity(testset));
    }
    
    public Bigram(Set<String> samples)
    {
        this.samples = samples;
        this.counts = new HashMap<String, HashMap<String, Integer>>();
        this.unigramCounts = new HashMap<String, Integer>();
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
                int unigramCount = 0;
                if (unigramCounts.containsKey(previousWord)) {
                    unigramCount = unigramCounts.get(previousWord);
                }
                unigramCounts.put(previousWord, unigramCount+1);
                
                // Get the new match (word2)
                String match = matcher.group();
                
                // Get access to (or create) the count map for word1.
                HashMap<String, Integer> innerCounts;
                if (counts.containsKey(previousWord)) {
                    innerCounts = counts.get(previousWord);
                } else {
                    innerCounts = new HashMap<String, Integer>();
                    counts.put(previousWord, innerCounts);
                }
                
                // Set bigram counts
                int count = 0;
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
}