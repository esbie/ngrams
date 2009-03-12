import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class Unigram
{
    public Set<String> samples;
    public HashMap<String, Integer> counts;
    
    public static void main(String[] args)
    {
        HashSet<String> set = new HashSet<String>();
        set.add("\n           John Blair &amp; Co. is close to an agreement to sell its TV station advertising representation operation and program production unit to an investor group led by James H. Rosenfield, a former CBS Inc. executive, industry sources said. \n\n           Industry sources put the value of the proposed acquisition at more than $100 million. \n        John Blair was acquired last year by Reliance Capital Group Inc., which has been divesting itself of John Blair's major assets. \n        John Blair represents about 130 local television stations in the placement of national and other advertising. \n\n           Mr. Rosenfield stepped down as a senior executive vice president of CBS Broadcasting in December 1985 under a CBS early retirement program. \n        Neither Mr. Rosenfield nor officials of John Blair could be reached for comment. \n\n");
        set.add("\n           South Korea posted a surplus on its current account of $419 million in February, in contrast to a deficit of $112 million a year earlier, the government said. \n        The current account comprises trade in goods and services and some unilateral transfers. \n\n");
        Unigram u = new Unigram(set);
        u.train();
        u.showCounts();
    }
    
    public Unigram(Set<String> samples)
    {
        this.samples = samples;
        this.counts = new HashMap<String, Integer>();
    }
    
    public void train()
    {
        // Regexp to match words (starting with optional apos) or any punctuation (with probably extra escaping)
        String regexp = "('?\\w+|[\\`\\~\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\_\\=\\+\\[\\{\\]\\}\\\\\\|\\;\\:\\'\\\"\\,\\<\\.\\>\\/\\?])";
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
            }
        }
    }
    
    public void showCounts()
    {
        for (String key : counts.keySet()) {
            System.out.println(key + ": " + counts.get(key));
        }
    }
}