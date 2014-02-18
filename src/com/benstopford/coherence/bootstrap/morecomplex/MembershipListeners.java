package com.benstopford.coherence.bootstrap.morecomplex;

import com.benstopford.coherence.bootstrap.structures.framework.CoherenceClusteredTest;
import com.tangosol.net.*;
import com.tangosol.util.Base;

import java.io.IOException;


/**
 * BTS, 12-May-2008
 */
public class MembershipListeners extends CoherenceClusteredTest {
    private Process otherMemberProcess;
    private static final String CONFIG_BASIC_CACHE_ON_DIFFERENT_CACHE_SERVICE_XML = "config/basic-cache-on-different-cache-service.xml";

    public void testInspectNodesArrivingAndLeavingCluster() throws IOException, InterruptedException {
        TestMembershipListener listener = new TestMembershipListener();

        NamedCache cache = getCache(CONFIG_BASIC_CACHE_ON_DIFFERENT_CACHE_SERVICE_XML,"foobar2");
        cache.getCacheService().addMemberListener(listener);
        Cluster cluster = cache.getCacheService().getCluster();

        assertEquals(1, cluster.getMemberSet().size());

        otherMemberProcess = startOutOfProcess(CONFIG_BASIC_CACHE_ON_DIFFERENT_CACHE_SERVICE_XML);

        //ensure there are two members
        assertEquals("Oops - expected 2 members but found " + cluster.getMemberSet().size(), 2, cluster.getMemberSet().size());

        //terminate member to generate listner event
        otherMemberProcess.destroy();
        Thread.sleep(5000);

        //ensure that the member listener was called
        assertEquals("Oops - expected one call to the listener but it was called: " + cluster.getMemberSet().size(), 1, cluster.getMemberSet().size());
        assertEquals("Oops - expected one call to the memberLeft but it was called: " + listener.memberLeft,1, listener.memberLeft);
    }

    public static class TestMembershipListener extends Base implements MemberListener {
        int memberJoined;
        int memberLeaving;
        int memberLeft;

        public void memberJoined(MemberEvent evt) {
            memberJoined++;
        }

        public void memberLeaving(MemberEvent evt) {
            memberLeaving++;
        }

        public void memberLeft(MemberEvent evt) {
            memberLeft++;
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        setDefaultProperties();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (otherMemberProcess != null && otherMemberProcess.exitValue() < 0) {
            otherMemberProcess.destroy();
        }
    }
}
