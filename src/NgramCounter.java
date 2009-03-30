import java.util.HashMap;
import java.util.Iterator;

public class NgramCounter
{
    public int level; // level into the tree (root = highest numbered level)
    public HashMap<String, NgramCounter> map; // links to child nodes, each link is the next word
    public double count; // leaf node's count for an n-gram
    
    public NgramCounter(int level)
    {
        this.level = level;
        if (level == 0) {
            // There are no links to child nodes, we are a leaf node
            this.map = null;
            // Set the initial count to 0.
            this.count = 0.0;
        } else {
            // We are not a leaf node, set up child node link hash.
            this.map = new HashMap<String, NgramCounter>();
        }
    }
    
    public void insert(String remaining)
    {
        // Recursive base case - If this is a leaf, increment the count
        if (level == 0) {
            count++;
            return;
        }
        
        // Recursive step - Find/create the next node to travel to and recurse
        
        // limit=2 means we will get [0]=nextword, [1]=rest
        String[] word_and_rest = remaining.split("\t", 2);
        
        NgramCounter next;
        if (map.containsKey(word_and_rest[0])) {
            next = map.get(word_and_rest[0]);
        } else {
            next = new NgramCounter(level-1);
            map.put(word_and_rest[0], next);
        }
        
        // These are really the same case, but check to avoid null pointer
        if (word_and_rest.length == 1) {
            next.insert(null);
        } else {
            next.insert(word_and_rest[1]);
        }
    }
    
    public double count(String remaining)
    {
        // Recursive base case - If this is a leaf, return the count
        if (level == 0) {
            return count;
        }
        
        // Recursive step - Find the next node to travel to and recurse
        
        // limit=2 means we will get [0]=nextword, [1]=rest
        String[] word_and_rest = remaining.split("\t", 2);
        
        if (!map.containsKey(word_and_rest[0])) {
            return 0.0; // we never saw this n-gram, so the count is 0.
        }
        
        // These are really the same case, but check to avoid null pointer
        if (word_and_rest.length == 1) {
            return map.get(word_and_rest[0]).count(null);
        }
        return map.get(word_and_rest[0]).count(word_and_rest[1]);
    }
    
    public double level1Count(String remaining)
    {
        // Recursive base case - One level above leaf nodes, sum all counts of leaf nodes
        if (level == 1) {
            double sum = 0.0;
            for (NgramCounter ngc : map.values()) {
                sum += ngc.count(null);
            }
            return sum;
        }
        
        // Recursive step - Find the next node to travel to and recurse
        
        // limit=2 means we will get [0]=nextword, [1]=rest
        String[] word_and_rest = remaining.split("\t", 2);
        if (!map.containsKey(word_and_rest[0])) {
            return 0; // we never saw this n-gram
        }
        return map.get(word_and_rest[0]).level1Count(word_and_rest[1]);
    }
    
    public String generateNextWord(String remaining)
    {
        // Recursive base case - One level above leaf nodes, find a random next word based on counts
        if (level == 1) {
            double totalCountForLevel = level1Count(null);
            
            // Generate a random distance into the counts to take a word from
            double rand = Math.random() * totalCountForLevel;
            
            // Go through the possible words and see how far our random number gets us
            Iterator<String> i = map.keySet().iterator();
            String nextWord = "";
            while (i.hasNext() && rand >= 0) {
                nextWord = i.next();
                rand -= map.get(nextWord).count(null);
            }
            
            return nextWord;
        }
        
        // Recursive step - Find the next node to travel to and recurse
        
        // limit=2 means we will get [0]=nextword, [1]=rest
        String[] word_and_rest = remaining.split("\t", 2);
        return map.get(word_and_rest[0]).generateNextWord(word_and_rest[1]);
    }
}