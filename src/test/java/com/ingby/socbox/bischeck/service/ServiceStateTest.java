package com.ingby.socbox.bischeck.service;


import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.ingby.socbox.bischeck.threshold.Threshold.NAGIOSSTAT;
import com.ingby.socbox.bischeck.service.ServiceState;
import com.ingby.socbox.bischeck.service.ServiceState.State;

public class ServiceStateTest {



    @BeforeTest
    public void beforeTest() throws Exception {


    }
    void displayState(ServiceState fsm) {
        System.out.println("Nagios:" + fsm.getState() +
                "  FSM:" + fsm.getStateLevel() + 
                "  Prev: "+ fsm.getPreviousStateLevel() +
                "  Notification: " + fsm.isNotification());
    }

    @Test (groups = { "ServiceState" })
    public void verifyFSM() {
        ServiceState fsm = null;
        fsm = new ServiceState(true);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousState(),NAGIOSSTAT.UNKNOWN);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());


        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getSoftCount(),1);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        ////////////////////
        ServiceState fsm1 = null;
        fsm1 = new ServiceState(true);
        System.out.println("NEW STATE created");
        fsm1.setState(NAGIOSSTAT.OK);
        displayState(fsm1);
        Assert.assertEquals(fsm1.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm1.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm1.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm1.isStateChange());
        Assert.assertFalse(fsm1.isNotification());

        ////////////////

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getSoftCount(),2);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getSoftCount(),3);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getSoftCount(),0);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());
        System.out.println(fsm.getCurrentIncidentId());
        

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getSoftCount(),0);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.CRITICAL);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getSoftCount(),0);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getSoftCount(),0);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());
        System.out.println(fsm.getCurrentIncidentId());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getSoftCount(),1);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        fsm.setState(NAGIOSSTAT.CRITICAL);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getSoftCount(),2);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());
        System.out.println(fsm.getCurrentIncidentId());
        
        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
        
        fsm.setState(NAGIOSSTAT.CRITICAL);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());
        System.out.println(fsm.getCurrentIncidentId());
        
    }

    @Test (groups = { "ServiceState" })
    public void verifyFSMmax() {
        ServiceState fsm = null;
        fsm = new ServiceState(2,true);

        System.out.println("With max set to 2");
        displayState(fsm);
        fsm.setState(NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.CRITICAL);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

    }

    @Test (groups = { "ServiceState" })
    public void verifyFSMmaxWithState() {
        ServiceState fsm = null;
        fsm = new ServiceState(NAGIOSSTAT.CRITICAL,5);
        System.out.println("With max set to 5 and inital to CRITICAL");
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getPreviousState(),NAGIOSSTAT.CRITICAL);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.CRITICAL);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertTrue(fsm.isNotification());

    }

    @Test (groups = { "ServiceState" })
    public void verifyMulti() {
        ServiceState fsm1 = null;
        fsm1 = new ServiceState(5);
        ServiceState fsm2 = null;
        fsm2 = new ServiceState(1);
        fsm2.setState(NAGIOSSTAT.OK);
        fsm2.setState(NAGIOSSTAT.WARNING);
        Assert.assertFalse(fsm2.isNotification());
        fsm2.setState(NAGIOSSTAT.WARNING);
        Assert.assertTrue(fsm2.isNotification());
        Assert.assertFalse(fsm1.isNotification());
        Assert.assertEquals(fsm2.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm1.getStateLevel(),State.OKAY_HARD);  
    }

    @Test (groups = { "ServiceState" })
    public void verifyFSMStateChangeSoft() {
        ServiceState fsm = null;
        fsm = new ServiceState(2,true);

        System.out.println("With max set to 2");
        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertFalse(fsm.isNotification());
        Assert.assertTrue(fsm.isStateChange());

        fsm.setState(NAGIOSSTAT.WARNING);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertFalse(fsm.isNotification());
        Assert.assertTrue(fsm.isStateChange());


        fsm.setState(NAGIOSSTAT.CRITICAL);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
        Assert.assertEquals(fsm.getPreviousState(),NAGIOSSTAT.WARNING);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertFalse(fsm.isNotification());
        Assert.assertTrue(fsm.isStateChange());
    }
    
    @Test (groups = { "ServiceState" })
    public void verifyFSMStateUnkown() {
        ServiceState fsm = null;
        fsm = new ServiceState(2,true);
        displayState(fsm);

        fsm.setState(NAGIOSSTAT.OK);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
        Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.UNKNOWN);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.UNKNOWN);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.UNKNOWN);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.UNKNOWN);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.UNKNOWN);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.UNKNOWN);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
        Assert.assertTrue(fsm.isStateChange());
        Assert.assertTrue(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.UNKNOWN);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.UNKNOWN);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());

        fsm.setState(NAGIOSSTAT.UNKNOWN);
        displayState(fsm);
        Assert.assertEquals(fsm.getState(),NAGIOSSTAT.UNKNOWN);
        Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
        Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
        Assert.assertFalse(fsm.isStateChange());
        Assert.assertFalse(fsm.isNotification());
    }
}
