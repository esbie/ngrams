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
    public HashMap<String, Double> counts;
    public double totalCount;
    public final String START = ":S";
    
    // For add-one smoothing
    public Set<String> wordSet; // Used to find the vocabulary
    public double vocabSize; // Size of the vocabulary
    
    // For Good Turing Smoothing
    public double numTrainingUnigrams; // The size of the training set (# non-distinct words)
    public HashMap<Double, Double> numberOfUnigramsWithCount; // The number of unigrams that occur x times
    public boolean goodTuringCountsAvailable = false; // True when good turing counts are available
    
    public static void main(String[] args)
    {
		if (args.length != 2) {
            System.out.println("You must supply 2 arguments:\n(1) Training file\n" +
                               "(2) Test file");
            System.exit(1);
        }
        
		NgramParser p = new NgramParser(args[0], true);
        HashSet<String> set = p.parse();
        
        Unigram u = new Unigram(set);
        u.train();
        
        System.out.println("Done training.");

        //System.out.println(u.getSentence());
        
        NgramParser test = new NgramParser(args[1], true);
        HashSet<String> testset = test.parse();
        System.out.println("Perplexity of the test set: " + u.perplexity(testset));
    }
    
    public Unigram(Set<String> samples)
    {
        this.samples = samples;
        this.counts = new HashMap<String, Double>();
        this.totalCount = 0;
        
        this.wordSet = new HashSet<String>();
        
        this.numberOfUnigramsWithCount = new HashMap<Double, Double>();
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
                
                wordSet.add(match);
                
                double count = 0;
                if (counts.containsKey(match)) {
                    count = counts.get(match);
                    numberOfUnigramsWithCount.put(count, numberOfUnigramsWithCount.get(count)-1);
                }
                counts.put(match, count+1);
                if (!numberOfUnigramsWithCount.containsKey(count+1)) {
                    numberOfUnigramsWithCount.put(count+1, 1.0);
                } else {
                    numberOfUnigramsWithCount.put(count+1, numberOfUnigramsWithCount.get(count+1)+1);
                }
                
                totalCount++;
                numTrainingUnigrams++;
            }
        }
        
        vocabSize = wordSet.size();
    }
    
    public double unsmoothedProbability(String word)
    {
        if (counts.containsKey(word)) {
            return counts.get(word) / totalCount;
        } else {
            return 0.0;
        }
    }
    
    public double count(String word)
    {
        if (counts.containsKey(word)) {
            return counts.get(word);
        }
        return 0.0;
    }
    
    public double addOneSmoothedProbability(String word)
    {
        return (count(word) + 1.0) / (totalCount + vocabSize);
    }
    
    public double goodTuringSmoothedProbability(String word)
    {
        if (!goodTuringCountsAvailable) {
            System.out.println("Making good turing counts...");
            makeGoodTuringCounts();
            System.out.println("Done making good turing counts.");
        }
        
        // If this unigram has occurred, return good turing probability
        double gtcount = count(word);
        if (gtcount > 0.0) {
            return gtcount / totalCount;
        }
        // Otherwise, return N1/N as per book (page 101?)
        return numberOfUnigramsWithCount.get(1.0) / numTrainingUnigrams;
    }
    
    public void makeGoodTuringCounts()
    {
        // Generate good turing counts
        totalCount = 0;
        for (String word : counts.keySet()) {
            double count = counts.get(word);
            if (!numberOfUnigramsWithCount.containsKey(count+1)) {
                numberOfUnigramsWithCount.put(count+1, 0.0);
            }
            // c* = (c+1) * N(c+1) / N(c)
            double newCount = (count + 1)*(numberOfUnigramsWithCount.get(count+1.0))/(numberOfUnigramsWithCount.get(count));
            counts.put(word, newCount);
            totalCount += newCount;
        }
        goodTuringCountsAvailable = true;
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
                rand -= counts.get(currentWord);
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
                /*if (unsmoothedProbability(match) > 0) {
                    products.push(unsmoothedProbability(match));
                    wordCount++;
                }*/
                products.push(goodTuringSmoothedProbability(match));
                wordCount++;
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