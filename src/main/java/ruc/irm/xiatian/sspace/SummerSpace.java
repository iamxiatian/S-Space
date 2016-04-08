package ruc.irm.xiatian.sspace;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.TfIdfTransform;
import edu.ucla.sspace.matrix.Transform;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vsm.VectorSpaceModel;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

//import static org.junit.Assert.assertEquals;

/**
 * Created by xiatian on 4/4/16.
 */
public class SummerSpace {
    public void build(File dir) throws IOException {
        IteratorFactory.setProperties(new Properties());
        VectorSpaceModel vsm = new VectorSpaceModel();
        Properties props = new Properties();
        props.setProperty(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY, TfIdfTransform.class.getCanonicalName());
        try {
            File[] childDirs = dir.listFiles((File f)->f.isDirectory());

            for (File childDir : childDirs) {
                File[] textFiles = childDir.listFiles((File f)->f.isFile());
                for (File f : textFiles) {
                    vsm.processDocument(new BufferedReader(new FileReader(f)));
                }
            }

            vsm.processSpace(props);
            System.out.printf("Vsm has %d docs and %d words%n",
                    vsm.documentSpaceSize(), vsm.getWords().size());


            int idx = 0;
            for(String w:vsm.getWords()){
                System.out.println((idx++) + "\t ==> " + w);
            }

            for (int i = 0; i < 3; ++i) {
                DoubleVector vector = vsm.getDocumentVector(i);
                System.out.println("magnitude: " + vector.magnitude());
                for(int j=0; j<vector.length(); j++) {
                    if(vector.get(j)==0 && j<vector.length()-1) continue;
                    System.out.print(j + ":" + vector.get(j) + " ");
                }
                System.out.println("\n-------------------------\n");
            }

            MatrixIO.writeMatrix(vsm.getWordSpace(), new File("/tmp/summer.matrix"), MatrixIO.Format.SVDLIBC_SPARSE_TEXT);

        }
        catch (Throwable t) {
            throw new Error(t);
        }
    }

    public static void main(String[] args) throws IOException {
        SummerSpace space = new SummerSpace();
        File dir = new File("/home/xiatian/data/20news-subject");
        space.build(dir);
    }
}
