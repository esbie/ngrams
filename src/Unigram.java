import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;

public class Unigram
{
    public Set<String> samples;
    public HashMap<String, Integer> counts;
    public int totalCount;
    public final String START = ":S";
    
    public static void main(String[] args)
    {
		NgramParser p = new NgramParser("data/fbistrain.xml");
        HashSet<String> set = p.parse();
        Unigram u = new Unigram(set);
        u.train();
        // u.showCounts();
        System.out.println("P(year) = " + u.unsmoothedProbability("year"));
        System.out.println(u.getSentence());

        NgramParser test = new NgramParser("data/fbistest.xml");
        HashSet<String> testset = test.parse();
        System.out.println(u.perplexity(testset));
    }
    
    public Unigram(Set<String> samples)
    {
        this.samples = samples;
        this.counts = new HashMap<String, Integer>();
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
                
                int count = 0;
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
        Stack<Double> products = new Stack<Double>();
        String regexp = "('?\\w+|\\p{Punct})";
        Pattern pattern = Pattern.compile(regexp);

        // counting number of words in test set
        for (String sample : testSamples) {
            Matcher matcher = pattern.matcher(sample);
            while (matcher.find()) {
                String match = matcher.group();
                if (unsmoothedProbability(match) > 0) {
                    products.push(unsmoothedProbability(match));
                    wordCount++;
                }
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