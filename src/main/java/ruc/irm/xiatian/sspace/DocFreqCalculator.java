package ruc.irm.xiatian.sspace;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.util.StringUtils;
import com.hankcs.hanlp.seg.common.Term;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vsm.VectorSpaceModel;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Calculating the document frequency by VectorSpaceModel
 *
 *
 * @author Tian Xia
 * @date Apr 08, 2016 16:36
 */
public class DocFreqCalculator {

    /** 子任务分配任务个数 */
    private int step = 10000;

    /** upload docs */
    private List<String> articles;
    /** word-count mapping */
    private Map<String,Long> result;

    /**
     * Load docs from docs File, in the file, each line represents
     * a document
     *
     * @param docsFile
     */
    public void load(File docsFile) throws IOException{
        this.articles = Files.readLines(docsFile, Charset.forName("UTF-8"));
    }

    /**
     * Do the calculate task. Make sure #load method is called first.
     */
    public void calculate() throws Exception{
        result = Maps.newHashMap();
        ExecutorService executor = Executors.newCachedThreadPool();
        List<FutureTask<Map<String,Long>>> taskList = Lists.newArrayList();
        int total = articles.size()%step==0 ? articles.size()/step : articles.size()/step+1;
        for(int i=0; i<total; i++){
            int start = i * step;
            int end = (start+step)>articles.size() ? articles.size() : (start+step);
            WordTask task = new WordTask(articles.subList(start,end));
            FutureTask<Map<String,Long>> futureTask = new FutureTask<>(task);
            executor.submit(futureTask);
            taskList.add(futureTask);
        }
        for(FutureTask<Map<String,Long>> futureTask : taskList){
            Map<String,Long> futureResult = futureTask.get();
            result = mergeMap(result,futureResult);
        }
        executor.shutdown();
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
    public void save(File outputFile) throws IOException{
        StringBuilder builder = new StringBuilder();
        List<Map.Entry<String,Long>> list = Lists.newArrayList(result.entrySet().iterator());
        Collections.sort(list, (o1, o2) -> (int)(o2.getValue() - o1.getValue()));
        for(Map.Entry<String,Long> entry : list){
            builder.append(entry.getKey())
                    .append(" ")
                    .append(entry.getValue())
                    .append("\n");
        }
        Files.write(builder.toString(),outputFile,Charset.forName("UTF-8"));
    }

    /** map合并,合并map value */
    private Map<String,Long> mergeMap(Map<String,Long> m1,Map<String,Long> m2){
        if(m2.isEmpty()) return m1;
        Set<Map.Entry<String,Long>> entrySet = m2.entrySet();
        Iterator<Map.Entry<String,Long>> iterator = entrySet.iterator();
        while (iterator.hasNext()){
            Map.Entry<String,Long> entry = iterator.next();
            if(null == m1.get(entry.getKey())){
                m1.put(entry.getKey(),entry.getValue());
            }else{
                m1.put(entry.getKey(),entry.getValue()+m1.get(entry.getKey()));
            }
        }
        return m1;
    }

    private class WordTask implements Callable<Map<String,Long>> {

        private List<String> articles;

        public WordTask(List<String> articles){
            this.articles = articles;
        }

        @Override
        public Map<String, Long> call() throws Exception {
            Map<String,Long> result = Maps.newHashMap();
            try {
                IteratorFactory.setProperties(new Properties());
                VectorSpaceModel vsm = new VectorSpaceModel();
                for(String article : articles){
                    JSONObject json = JSON.parseObject(article);
                    //得到文章内容
                    String content = json.getString("content");
                    //去掉html标签
                    content = HtmlUtil.html2Text(content);
                    List<String> words = Lists.newArrayList();
                    //分词
                    List<Term> termList = HanLP.segment(content);
                    for(Term term : termList){
                        String tword = term.word.trim();
                        if(!StringUtils.isBlankOrNull(tword) &&
                                tword.length() > 1 &&
                                (term.nature.startsWith("n")||term.nature.startsWith("v")||term.nature.startsWith("a"))&&
                                !containsSymbol(tword)){
                            words.add(tword);
                        }
                    }
                    vsm.processDocument(words.iterator());
                }
                vsm.processSpace(new Properties());
                System.out.printf("Vsm has %d docs and %d words%n",vsm.documentSpaceSize(), vsm.getWords().size());
                for(String w : vsm.getWords()){
                    DoubleVector vector = (DoubleVector)vsm.getVector(w);
                    for(int j=0; j<vector.length(); j++) {
                        if(vector.get(j) > 0){
                            Long count = result.get(w);
                            if(null == count){
                                count = 0L;
                            }
                            result.put(w,++count);
                        }
                    }
                }
            }
            catch (Throwable t) {
                throw new Error(t);
            }
            return result;
        }

        /** 是否包括字母等特殊字符 */
        public boolean containsSymbol(String word){
            for(int i=0; i< word.length(); i++){
                char c = word.charAt(i);
                if(c < 127){
                    return true;
                }
            }
            return false;
        }
    }

    public static void main(String[] args) throws Exception{
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
