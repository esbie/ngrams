import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.*;

/*
 * General case n-gram for any n
 */
public class Ngram
{
    public Set<String> samples; // Set of sample sentences to train from
    public int n; // (as in n-gram)
    public NgramCounter ngc; // The data structure for holding n-gram counts
    public final String START = ":S"; // The sentence start symbol
    
    public static void main(String[] args)
    {
        if (args.length != 2) {
            System.out.println("You must supply two arguments:\n(1) Training file\n(2) an integer n > 1");
            System.exit(1);
        }
        NgramParser p = new NgramParser(args[0], true);
        HashSet<String> set = p.parse();
        
        Ngram n = new Ngram(set, Integer.parseInt(args[1]));
        n.train();
        
        System.out.println("Done Training. Press enter for generated sentences.");
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
        this.ngc = new NgramCounter(n);
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
                
                // Insert the words into the counter
                ngc.insert(joinWithTabs(nWords));
            }
        }
    }
    
    // Convenience(???) method for if you have a n-long array of words
    public double unsmoothedProbability(String[] words)
    {
        return unsmoothedProbability(joinWithTabs(words));
    }
    
    public double unsmoothedProbability(String tabbedWords)
    {
        if (ngc.count(tabbedWords) > 0) {
            return ngc.count(tabbedWords) / ngc.level1Count(tabbedWords);
        }
        return 0.0;
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
            String nextWord = ngc.generateNextWord(joinWithTabs(words));
            
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
    
    // Convenience function to join an array of strings with the tab character
    // Why isn't there a default join method for arrays in Java?
    public static String joinWithTabs(String[] strings)
    {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < strings.length - 1; i++) {
            joined.append(strings[i]);
            joined.append('\t');
        }
        joined.append(strings[strings.length-1]);
        return joined.toString();
    }
}