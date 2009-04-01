import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.io.*;

/*
 * General case n-gram for any n
 */
public class Ngram
{
    public Set<String> samples; // Set of sample sentences to train from
    public int n; // (as in n-gram)
    public NgramCounter ngc; // The data structure for holding n-gram counts
    public Set<String> wordSet;
    public double vocabSize;
    public int numTrainingNgrams;
    public HashMap<Double, Double> numberOfNgramsWithCount;
    public final String START = ":S"; // The sentence start symbol
    
    public static void main(String[] args)
    {
        if (args.length != 3) {
            System.out.println("You must supply 3 arguments:\n(1) Training file\n" +
                               "(2) Test file\n(3) an integer n > 1");
            System.exit(1);
        }
        NgramParser p = new NgramParser(args[0], true);
        HashSet<String> set = p.parse();
        
        Ngram n = new Ngram(set, Integer.parseInt(args[2]));
        n.train();
        
        System.out.println("Done training.");
        
        System.out.println("Making good turing counts...");
        n.makeGoodTuringCounts();
        System.out.println("Done making good turing counts.");
        
        NgramParser test = new NgramParser(args[1], true);
        HashSet<String> testset = test.parse();
        System.out.println("Perplexity of the test set: " + n.perplexity(testset));
        
        System.exit(0);
        System.out.println("Press enter for generated sentences.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try { 
                br.readLine(); 
            } catch (IOException ioe) { 
                System.out.println("IO error!"); 
                System.exit(1); 
            }
            System.out.println(n.getSentence());
        }
    }
    
    public Ngram(HashSet<String> samples, int n)
    {
        this.samples = samples;
        this.n = n;
        this.numberOfNgramsWithCount = new HashMap<Double, Double>();
        this.ngc = new NgramCounter(n, numberOfNgramsWithCount);
        this.wordSet = new HashSet<String>();
        this.numTrainingNgrams = 0;
    }
    
    public void train()
    {
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);
        for (String sample : samples) {
            // Create list of words in the sample sentence & fill it
            ArrayList<String> sampleWords = new ArrayList<String>();
            Matcher matcher = pattern.matcher(sample);
            while (matcher.find()) {
                String match = matcher.group();
                sampleWords.add(match);
                wordSet.add(match);
            }
            
            // Add each group of n words to the n-gram counter, e.g., ...
            // [:S :S :S w1] w2 w3 w4 w5 w6
            // :S [:S :S w1 w2] w3 w4 w5 w6
            // :S :S [:S w1 w2 w3] w4 w5 w6
            // :S :S :S [w1 w2 w3 w4] w5 w6
            // :S :S :S w1 [w2 w3 w4 w5] w6
            // :S :S :S w1 w2 [w3 w4 w5 w6]
            String[] nWords = new String[n];
            for (int i = 0; i < n; i++) {
                nWords[i] = START;
            }
            for (String word : sampleWords) {
                for (int i = 0; i < n-1; i++) {
                    nWords[i] = nWords[i+1];
                }
                nWords[n-1] = word;
                
                numTrainingNgrams++;
                
                // Insert the words into the counter
                double countForNgram = ngc.insert(nWords);
                
                // Update numberOfNgramsWithCount
                if (countForNgram != 1.0) {
                    numberOfNgramsWithCount.put(countForNgram-1,
                        numberOfNgramsWithCount.get(countForNgram-1) - 1.0);
                }
                if (!numberOfNgramsWithCount.containsKey(countForNgram)) {
                    numberOfNgramsWithCount.put(countForNgram, 1.0);
                } else {
                    numberOfNgramsWithCount.put(countForNgram,
                        numberOfNgramsWithCount.get(countForNgram) + 1.0);
                }
            }
        }
        
        vocabSize = wordSet.size();
    }
    
    public double unsmoothedProbability(String[] words)
    {
        double count = ngc.count(words);
        if (count > 0) {
            return count / ngc.level1Count(words);
        }
        return 0.0;
    }
    
    public double addOneSmoothedProbability(String[] words)
    {
        return (ngc.count(words) + 1.0) / (ngc.level1Count(words) + vocabSize);
    }
    
    public double goodTuringSmoothedProbability(String[] words)
    {
        double gtcount = ngc.gtcount(words);
        if (gtcount > 0) {
            return gtcount / ngc.level1GTCount(words);
        }
        return numberOfNgramsWithCount.get(1.0)/(double)numTrainingNgrams;
    }
    
    public void makeGoodTuringCounts()
    {
        ngc.makeGoodTuringCounts();
    }
    
    public String getSentence()
    {
        StringBuilder sentence = new StringBuilder();
        
        // The array of words we are using as context
        // The last slot is what we are trying to fill
        String[] words = new String[n];
        
        // Fill up the words array with START symbols
        for (int i = 0; i < n; i++) {
            words[i] = START;
        }
        
        // This is simply to indicate that the last symbol what we are trying to figure out
        words[n-1] = "???";
        
        // While we have not reached the end of the sentence and it's of reasonable (400 chars) length
        while (!words[n-2].equals(".") && sentence.length() < 400) {
            // Generate a new word based on context
            String nextWord = ngc.generateNextWord(words);
            
            // Update context with the new word
            for (int i = 0; i < n-2; i++) {
                words[i] = words[i+1];
            }
            words[n-2] = nextWord;
            
            // Update the sentence so far
            sentence.append(nextWord);
            sentence.append(' ');
        }
        
        return sentence.toString();
    }
    
    public double perplexity(Set<String> testSamples)
    {
        int wordCount = 0;
        Stack<Double> probabilities = new Stack<Double>();
        
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);
        String[] nWords = new String[n];
        for (String testSample : testSamples) {
            Matcher matcher = pattern.matcher(testSample);
            
            for (int i = 0; i < n; i++) {
                nWords[i] = START;
            }
            
            while (matcher.find()) {
                String match = matcher.group();
                for (int i = 0; i < n-1; i++) {
                    nWords[i] = nWords[i+1];
                }
                nWords[n-1] = match;
                
                // Find the probability of the n-gram
                probabilities.push(goodTuringSmoothedProbability(nWords));
                wordCount++;
            }
        }
        
        double product = 1;
        double power = 1.0/wordCount;
        while (!probabilities.empty()) {
            product *= Math.pow(probabilities.pop(), power);
        }
        
        double perplexity = 1 / product;
        return perplexity;
    }
}