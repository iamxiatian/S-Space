package ruc.irm.xiatian.sspace;

import edu.ucla.sspace.common.statistics.PointwiseMutualInformationTest;
import edu.ucla.sspace.common.statistics.SignificanceTest;
import edu.ucla.sspace.text.TermAssociationFinder;

/**
 * 统计语料库中词语的共现关系
 *
 * @author Tian Xia
 * @date Apr 15, 2016 17:55
 */
public class WordOccurrence {
    public void process() {
        SignificanceTest test = new PointwiseMutualInformationTest();
        TermAssociationFinder finder = new TermAssociationFinder(test);
        finder.addContext(null);
    }
}
