import java.io.*;
import java.util.*;
import java.lang.Math;

public class Bayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    static enum ActionType
    {
        TRAIN, TEST 
    }

    // This a class with two counters (for regular and for spam)
    /// We added the probability per word to the value stored in the hashtable.
    static class Multiple_Counter_Probability
    {
        int counter_spam = 0;
        int counter_regular = 0;
        double probability_spam = 0;
        double probability_regular = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter_Probability> vocab = new Hashtable <String, Multiple_Counter_Probability> ();

    
    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Multiple_Counter_Probability counter = new Multiple_Counter_Probability();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(word, counter);                       // put the word with its counter into the hashtable
    }

    // Count words (spam and regular) in the vocabulary
    private static Multiple_Counter_Probability countWords()
    {
        Multiple_Counter_Probability counterTotal = new Multiple_Counter_Probability();
        Multiple_Counter_Probability counter = new Multiple_Counter_Probability();


        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);

            counterTotal.counter_regular += counter.counter_regular;
            counterTotal.counter_spam += counter.counter_spam;
        }

        return counterTotal;

    }


    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
        System.out.println(listing_regular);
    }

    
    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Multiple_Counter_Probability counter = new Multiple_Counter_Probability();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);
            
            System.out.println( word + " | in regular: " + counter.counter_regular + 
                                " in spam: "    + counter.counter_spam);
        }
    }


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages(MessageType type, ActionType action)
    throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        
        for (int i = 0; i < messages.length; ++i)
        {
            double logPsum_spam = 0;
            double logPsum_regular = 0;
            Multiple_Counter_Probability probabilities = new Multiple_Counter_Probability();

            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
        
                while (st.hasMoreTokens())                  // while there are stille words left..
                {
        		    /// Temporarily store token for parsing
        		    String nextWord = st.nextToken();
        		    /// Make word lower case, remove every symbol that is not a lower case letter
        		    nextWord = nextWord.toLowerCase().replaceAll("[^a-z]", "");
        		    /// Only add words to the vocabulary with more than three characters
        		    if (nextWord.length() >= 4) {
                        if (action == ActionType.TRAIN) {
                            addWord(nextWord, type);                  // add them to the vocabulary
                        } else if (vocab.get(nextWord) != null) {
                            probabilities = vocab.get(nextWord);
                            logPsum_spam += probabilities.probability_spam;
                            logPsum_regular += probabilities.probability_regular;
                        } 
        		    }
                }
            }

            /// calculate probability

            /// check how bad it is

            in.close();
        }
    }


    private static void trainClassifier() {
        /// Define parameter
        double epsilon = 1;

        ///  Computing a priori class probabilities.
        long nMessagesRegular = listing_regular.length;
        long nMessagesSpam = listing_spam.length;
        long nMessagesTotal = nMessagesRegular + nMessagesSpam;
        double pRegular = Math.log(nMessagesRegular / nMessagesTotal);
        double pSpam = Math.log(nMessagesSpam / nMessagesTotal);
        /// Computing class conditional word likelihoods
        Multiple_Counter_Probability counter = countWords();
        long nWordsRegular = counter.counter_regular;
        long nWordsSpam = counter.counter_spam;
        /// Looping through the vocabulary calculating the probability for spam and regular per word.
        Multiple_Counter_Probability probabilities = new Multiple_Counter_Probability();
        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            probabilities  = vocab.get(word);

            if (probabilities.counter_spam == 0) {
                probabilities.probability_spam = Math.log(epsilon / (nWordsRegular+nWordsSpam));
            } else {
               probabilities.probability_spam = Math.log(probabilities.counter_spam/nWordsSpam); 
            }

            if (probabilities.counter_regular == 0) {
                probabilities.probability_regular = Math.log(epsilon / (nWordsRegular+nWordsSpam));
            } else {
                probabilities.probability_regular = Math.log(probabilities.counter_regular/nWordsRegular);    
            }

            vocab.put(word, probabilities);
        }


    }
   
    public static void main(String[] args)
    throws IOException
    {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        File dir_location_test = new File( args[1]);
        
        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() || !dir_location_test.isDirectory())
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        // Print out the hash table
        //printVocab();

	   /// Start the training.
       trainClassifier();
        
        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}