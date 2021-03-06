import java.io.*;
import java.util.*;
import java.lang.Math;

public class BigramBayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    /// We defined an additional enum in order to re-use code in readmessages.
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

    /// Probabilities for being spam or regular
    private static double pRegular = 0;
    private static double pSpam = 0;

    /// Confusion Matrix
    private static double true_regular = 0;
    private static double true_spam = 0;
    private static double false_regular = 0;
    private static double false_spam = 0;
    
    /// Define parameters; 
    /// in order to be able to give the parameters as input in the terminal
    private static double epsilon = 1;
    private static int minWordLength = 4;
    private static int freqThreshold = 1;

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
     ///Depending of the actiontype the function adds the words to vocab or calculates the probabilities of the words and the messages  
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
            double pSum_spam = 0;
            double pSum_regular = 0;
            Multiple_Counter_Probability probabilities = new Multiple_Counter_Probability();

            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String bigram;
            String word1 = "";
            String word2 = "";
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
                
                if (st.hasMoreTokens()){
                    word1 = st.nextToken();    
                }
                

                while (st.hasMoreTokens())                  // while there are stille words left..
                {
        		    /// Temporarily store token for parsing
        		    word2 = st.nextToken();
        		    /// Make word lower case, remove every symbol that is not a lower case letter
                    /// Then stick the two words together with a space in between.
        		    bigram = word1.toLowerCase().replaceAll("[^a-z]", "") + " " + word2.toLowerCase().replaceAll("[^a-z]", "");
        		    /// Only add words to the vocabulary with more than three characters
        		    if (word1.length() >= minWordLength && word2.length() >= minWordLength) {
                        if (action == ActionType.TRAIN) {
                            addWord(bigram, type);                  // add them to the vocabulary
                        } else if (vocab.get(bigram) != null) {
                             /// calculating the probability of the message being spam or regular for each word
                            probabilities = vocab.get(bigram);
                            pSum_spam += probabilities.probability_spam;
                            pSum_regular += probabilities.probability_regular;
                        } 
        		    }
                    word1 = word2;
                }
            }

            /// This calculates the final probability of message being regular or spam and compares the two
            if (action == ActionType.TEST) {
                double pMsg_spam =  pSpam + pSum_spam;
                double pMsg_regular = pRegular + pSum_regular;
                // System.out.println("regular " + pMsg_regular + " spam " + pMsg_regular);
                if (pMsg_regular > pMsg_spam) {
                    if (type == MessageType.NORMAL) {
                        true_regular++;
                    } else {
                        false_regular++;
                    }
                } else {
                    if (type == MessageType.SPAM) {
                        true_spam++;
                    } else {
                        false_spam++;
                    }
                }
            }

            in.close();
        }
    }

    /// We wrote this function to calculate all probabilities that are needed before you can start testing
    private static void trainClassifier() {
        System.out.println("size" + vocab.size());
        applyFreqThreshold();
        System.out.println("size" + vocab.size());
        
        ///  Computing a priori class probabilities.
        double nMessagesRegular = listing_regular.length;
        double nMessagesSpam = listing_spam.length;
        double nMessagesTotal = nMessagesRegular + nMessagesSpam;
        pRegular = Math.log(nMessagesRegular / nMessagesTotal);
        pSpam = Math.log(nMessagesSpam / nMessagesTotal);
        /// Computing class conditional word likelihoods
        Multiple_Counter_Probability counter = countWords();
        double nWordsRegular = counter.counter_regular;
        double nWordsSpam = counter.counter_spam;
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

         // To be able to find out where the weird things happen...
        // System.out.println(nMessagesRegular);
        // System.out.println(nMessagesSpam);
        // System.out.println(nMessagesTotal);
        // System.out.println("pRegular" + pRegular);
        // System.out.println("pSpam" + pSpam);
        // System.out.println(nWordsRegular);
        // System.out.println(nWordsSpam);


    }

     /// This function loops through the vocab, removing all words with too low counts.
    private static void applyFreqThreshold()
    {
         Multiple_Counter_Probability counter = new Multiple_Counter_Probability();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);

            if (counter.counter_regular + counter.counter_spam < freqThreshold) {
                vocab.remove(word);
            }
        }
    }
   
    public static void main(String[] args)
    throws IOException
    {
        /// in order to be able to give the parameters as input in the terminal
        if (args.length > 2) {
            epsilon = Double.parseDouble(args[2]);
            freqThreshold = Integer.parseInt(args[3]);
            minWordLength = Integer.parseInt(args[4]);
        }

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
        readMessages(MessageType.NORMAL, ActionType.TRAIN);
        readMessages(MessageType.SPAM, ActionType.TRAIN);

        // Print out the hash table
        //printVocab();

	   /// Start the training.
       trainClassifier();

       /// List directories in test folder
       listDirs(dir_location_test);

       ///Testing
        readMessages(MessageType.NORMAL, ActionType.TEST);
        readMessages(MessageType.SPAM, ActionType.TEST);

        /// print the confusion matrix
        System.out.println("True regular: " + true_regular + " False spam: " + false_spam);
        System.out.println("False regular: " + false_regular + " True spam: " + true_spam);

        /// calculate the performance percentages
        double percentageSpam = (true_spam / (true_spam+false_regular))*100.0;
        double percentageRegular = (true_regular / (true_regular+false_spam))*100.0;
        double averall = ((true_spam+true_regular) / (true_spam+false_regular+true_regular+false_spam))*100.0;        

        /// print the performance percentages
        System.out.println("Performance spam: " + percentageSpam + " %");
        System.out.println("Performance regular: " + percentageRegular + " %");
        System.out.println("Performance overall: " + averall + " %");
   }
}