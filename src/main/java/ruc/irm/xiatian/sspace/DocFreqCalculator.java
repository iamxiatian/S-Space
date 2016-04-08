package ruc.irm.xiatian.sspace;

import com.google.common.base.Charsets;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileFilter;

/**
 * Calculating the document frequency by VectorSpaceModel
 *
 *
 * @author Tian Xia
 * @date Apr 08, 2016 16:36
 */
public class DocFreqCalculator {

    /**
     * Load docs from docs File, in the file, each line represents
     * a document
     *
     * @param docsFile
     */
    public void load(File docsFile) {

    }

    /**
     * Do the calculate task. Make sure #load method is called first.
     */
    public void calculate() {

    }

    /**
     * Save document frequency into outputFile. In the output file, each line
     * has the following format:<br/>
     * <ul>
     *  <li>word1 10000</li>
     *  <li>word2 9800</li>
     *  <li>word3 8000</li>
     *  <li>...</li>
     * </ul>
     *
     * @param outputFile
     */
    public void save(File outputFile) {

    }

    public static void main(String[] args) throws ParseException {
        String helpMsg = "usage: DocFreqCalculator -if input_filename -of output_filename";

        HelpFormatter helpFormatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption(new Option("if", true, "Input file that contains all documents."));
        options.addOption(new Option("of", true, "Output file that contains document frequency"));

        CommandLine commandLine = parser.parse(options, args);
        if (!commandLine.hasOption("if") || !commandLine.hasOption("of")) {
            helpFormatter.printHelp(helpMsg, options);
            return;
        }

        DocFreqCalculator calculator = new DocFreqCalculator();
        File inFile = new File(commandLine.getOptionValue("if"));
        File outFile = new File(commandLine.getOptionValue("of"));
        calculator.load(inFile);
        calculator.calculate();
        calculator.save(outFile);
        System.out.println("Document Frequencies are stored in file " + outFile.getAbsolutePath());
        System.out.println("I'm DONE!");
    }
}
