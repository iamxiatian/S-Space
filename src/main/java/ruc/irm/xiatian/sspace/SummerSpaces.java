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

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Created by xiatian on 4/4/16.
 */
public class SummerSpaces {

    public static void main(String[] args) throws Exception {
        File file = new File("/Users/zhongkai/workspace/openProject/NEW.JSON");
        List<String> articles = Files.readLines(file, Charset.forName("UTF-8"));
        Map<String,Long> result = Maps.newHashMap();
        ExecutorService executor = Executors.newCachedThreadPool();
        List<FutureTask<Map<String,Long>>> taskList = Lists.newArrayList();
        int step = 10000;
        for(int i=0; i<10; i++){
            WordTask task = new WordTask(articles.subList(i*step,(i+1)*step));
            FutureTask<Map<String,Long>> futureTask = new FutureTask<>(task);
            executor.submit(futureTask);
            taskList.add(futureTask);
        }
        for(FutureTask<Map<String,Long>> futureTask : taskList){
            Map<String,Long> futureResult = futureTask.get();
            result = mergeMap(result,futureResult);
        }
        executor.shutdown();
        String wordMap = JSON.toJSONString(result);
        Files.write(wordMap,new File("/Users/zhongkai/workspace/openProject/wordMap.json"),Charset.forName("UTF-8"));
    }

    /** map合并,合并map value */
    private static Map<String,Long> mergeMap(Map<String,Long> m1,Map<String,Long> m2){
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


    private static class WordTask implements Callable<Map<String,Long>>{

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
                Properties props = new Properties();
//            props.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY, TfIdfTransform.class.getCanonicalName());

                for(String article : articles){
                    JSONObject json = JSON.parseObject(article);
                    String content = json.getString("content");
                    content = HtmlUtil.html2Text(content);
                    List<String> words = Lists.newArrayList();
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
                vsm.processSpace(props);
                System.out.printf("Vsm has %d docs and %d words%n",vsm.documentSpaceSize(), vsm.getWords().size());

                int idx = 0;
                for(String w : vsm.getWords()){
                    DoubleVector vector = (DoubleVector)vsm.getVector(w);
//                    System.out.println((idx++) + " ==> " + w +" ==> "+vector);
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

}
