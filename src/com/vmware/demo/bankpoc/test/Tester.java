package com.vmware.demo.bankpoc.test;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.vmware.demo.bankpoc.client.MachineConfiguration;
import com.vmware.demo.bankpoc.client.POCClient;
import com.vmware.vcac.authentication.rest.stubs.Subtenant;
import com.vmware.vcac.catalog.rest.stubs.CatalogItem;
import com.vmware.vcac.catalog.rest.stubs.CatalogResourceView;
import com.vmware.vcac.catalog.rest.stubs.ConsumerResourceOperation;
import com.vmware.vcac.catalog.rest.stubs.Request;
import com.vmware.vcac.catalog.rest.stubs.v7_0.CatalogResource;
import com.vmware.vcac.reservation.rest.stubs.ReservationPolicy;

public class Tester {
	private static void testGetCatalogItems(POCClient client) {
		System.out.println("\n***** Catalog items *****");
		Collection<CatalogItem> items = client.getCatalogItems(null, 100);
		for(CatalogItem item : items) {
			System.out.println(item.getId() + " " + item.getName());
		}
	}
	
	private static void testGetResources(POCClient client) {
		System.out.println("\n***** Catalog resources *****");
		Collection<CatalogResource> items = client.getCatalogResources(null, 100);
		for(CatalogResource item : items) {
			System.out.println(item.getId() + " " + item.getName() + " " + item.getProviderBinding().getProviderRef().getLabel());
		}
	}
	
	private static void testGetBusinessGroups(POCClient client, String tenant) {
		System.out.println("\n***** Business groups items *****");
		Set<Subtenant> items = client.getBusinessGroups(tenant, null, 1);
		for(Subtenant item : items) {
			System.out.println(item.getId() + " " + item.getName());
		}
	}
	
	private static void testRequestMachine(POCClient client, String tenant, String catalogItemName, String reservationPolicyName, String ou, String affinityGroup) {
		System.out.println("\n***** Request machine *****");
		
		// Look up the catalog item
		//
		Collection<CatalogItem> items = client.getCatalogItems(catalogItemName, 1);
		CatalogItem item = items.iterator().next();
		String policyId = null;
		
		// Look up reservation policy if it was specified.
		//
		if(reservationPolicyName != null) {
			Collection<ReservationPolicy> policies = client.getReservationPolicies(reservationPolicyName, 1);
			ReservationPolicy policy = policies.iterator().next();
		}
		
		// Set up configuration
		//
		HashMap<String, MachineConfiguration> config = new HashMap<String, MachineConfiguration>();
		config.put("vSphere_Machine_1", new MachineConfiguration(2, 1024, 60, policyId));
		Map<String, Object> props = new HashMap<String, Object>();
		//props.put("Hostname", "test123");
		props.put("ou", ou);
		props.put("affinityGroup", affinityGroup);
		
		// Request the machine
		//
		URI rqURI = client.requestMachine(item.getId(), null, config, props);
		Request rq = client.getRequestFromURI(rqURI);
		System.out.println("Request: " + rq.getRequestNumber() + " status: " + rq.getStateName());
	}
	
	private static void testGetDay2Operations(POCClient client, String resourceName) {
		System.out.println("\n***** Get day 2 operations *****");
		Collection<CatalogResource> resources = client.getCatalogResources(resourceName, 1);
		CatalogResource resource = resources.iterator().next();
		Collection<ConsumerResourceOperation> ops = client.getDay2Operations(resource.getId());
		for(ConsumerResourceOperation op : ops)
			System.out.println(op.getId() + " " + op.getName() + " " + op.getBindingId());
	}
	
	private static void testReconfigureMachine(POCClient client, String machineName, MachineConfiguration config) throws InterruptedException {
		System.out.println("\n***** Reconfigure machine *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		client.requestMachineChange(resource.getId(), config, true);
		
		// Wait for initial phase to finish
		//
		for(;;) {
			Thread.sleep(5000);
			testGetDay2Operations(client, machineName);
			if(client.isChangePending(resource.getId()))
				break;
		}
		
		// Ready for final step! Execute!
		//
		//Thread.sleep(30000);
		//client.finalizeMachineChange(resource.getId());
	}
	
	private static void testGetReservationPolicies(POCClient client) {
		System.out.println("\n***** Get policies *****");
		Collection<ReservationPolicy> policies = client.getReservationPolicies(null, 100);
		for(ReservationPolicy policy : policies) 
			System.out.println(policy.getId() + " " + policy.getName());
	}
	
	private static void testDestroyMachine(POCClient client, String machineName) {
		System.out.println("\n***** Destroy machine *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		client.destroyMachine(resource.getId());
	}
	
	private static void testGetMachineInfo(POCClient client, String machineName) {
		System.out.println("\n***** Get machine info *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		Map<String, Object> details = client.getMachineDetails(resource.getId());
		Map<String, Object> data = (Map<String, Object>) details.get("data");
		System.out.println(resource.getName() + ": " + data.get("MachineCPU") + "/" + data.get("MachineMemory"));
	}
	
	private static void testMachineReprovision(POCClient client, String machineName) {
		System.out.println("\n***** Machine reprovision *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		client.requestMachineReprovision(resource.getId());
	}
	
	private static void testMigrateStorage(POCClient client, String machineName) {
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("targetDatastore", "123");
		client.submitCustomRequest(resource.getId(), "Change storage", data);
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("***** Login *****");
		POCClient client = new POCClient(args[0], args[1], args[2], args[3]);
		testGetCatalogItems(client); 
		testGetResources(client);
		testGetBusinessGroups(client, args[3]);
		//testRequestMachine(client, args[3], "CentOS7 Minimal", "East", "ThisIsMyOU", "Development Sandbox");
		testGetDay2Operations(client, "dev-0091"); 
		//testReconfigureMachine(client, "dev-0092", new MachineConfiguration(3, 1024, 60, null));
		testGetDay2Operations(client, "dev-0091"); 
		//testMachineReprovision(client, "dev-0092");
		//testDestroyMachine(client, "dev-0100");
		testGetReservationPolicies(client);
		testGetMachineInfo(client, "dev-0091");	
		testMigrateStorage(client, "dev-0091");
	}
}
