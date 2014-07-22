package testng.com.ingby.socbox.bischeck.service;


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
		fsm = new ServiceState();
		displayState(fsm);

		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);


		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getSoftCount(),1);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		////////////////////
		ServiceState fsm1 = null;
		fsm1 = new ServiceState();
		System.out.println("NEW STATE created");
		fsm1.setState(NAGIOSSTAT.OK);
		displayState(fsm1);
		Assert.assertEquals(fsm1.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm1.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm1.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm1.isNotification(),false);
		Assert.assertEquals(fsm1.isStateChange(),true);

		////////////////

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getSoftCount(),2);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getSoftCount(),3);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getSoftCount(),0);
		Assert.assertEquals(fsm.isNotification(),true);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getSoftCount(),0);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),false);

		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getSoftCount(),0);
		Assert.assertEquals(fsm.isNotification(),true);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getSoftCount(),1);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.CRITICAL);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getSoftCount(),2);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),true);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),false);

	}

	@Test (groups = { "ServiceState" })
	public void verifyFSMmax() {
		ServiceState fsm = null;
		fsm = new ServiceState(2);

		System.out.println("With max set to 2");
		displayState(fsm);
		fsm.setState(NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.CRITICAL);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),false);

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
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm.isNotification(),true);

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
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);

		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.isNotification(),false);

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
		Assert.assertEquals(fsm.isNotification(),false);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.isNotification(),true);

	}

	@Test (groups = { "ServiceState" })
	public void verifyMulti() {
		ServiceState fsm1 = null;
		fsm1 = new ServiceState(5);
		ServiceState fsm2 = null;
		fsm2 = new ServiceState(1);
		fsm2.setState(NAGIOSSTAT.OK);
		fsm2.setState(NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm2.isNotification(),false);
		fsm2.setState(NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm2.isNotification(),true);
		Assert.assertEquals(fsm1.isNotification(),false);
		Assert.assertEquals(fsm2.getStateLevel(),State.PROBLEM_HARD);
		Assert.assertEquals(fsm1.getStateLevel(),State.OKAY_HARD);	
	}

	@Test (groups = { "ServiceState" })
	public void verifyFSMStateChangeSoft() {
		ServiceState fsm = null;
		fsm = new ServiceState(2);

		System.out.println("With max set to 2");
		fsm.setState(NAGIOSSTAT.OK);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.OK);
		Assert.assertEquals(fsm.getStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);

		fsm.setState(NAGIOSSTAT.WARNING);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.OKAY_HARD);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);


		fsm.setState(NAGIOSSTAT.CRITICAL);
		displayState(fsm);
		Assert.assertEquals(fsm.getState(),NAGIOSSTAT.CRITICAL);
		Assert.assertEquals(fsm.getPreviousState(),NAGIOSSTAT.WARNING);
		Assert.assertEquals(fsm.getStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.getPreviousStateLevel(),State.PROBLEM_SOFT);
		Assert.assertEquals(fsm.isNotification(),false);
		Assert.assertEquals(fsm.isStateChange(),true);
	}
}
