package com.vmware.demo.bankpoc.test;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vmware.demo.bankpoc.client.AggregateResourceConsumption;
import com.vmware.demo.bankpoc.client.MachineConfiguration;
import com.vmware.demo.bankpoc.client.POCClient;
import com.vmware.demo.bankpoc.client.ResourceConsumption;
import com.vmware.demo.bankpoc.client.VROPSClient;
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
	
	private static void testRequestMachine(POCClient client, String tenant, String catalogItemName, String reservationPolicyName, String ou, String affinityGroup, String ait) {
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
		config.put("Amazon_Machine_1", new MachineConfiguration(2, 7680, 60, policyId));
		Map<String, Object> props = new HashMap<String, Object>();
		//props.put("Hostname", "test123");
		props.put("ou", ou);
		props.put("affinityGroup", affinityGroup);
		props.put("bac_tag_ait", ait);
		props.put("bac_tag_ou", ou);
		
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
		System.out.println(details);
	}
	
	private static void testMachineReprovision(POCClient client, String machineName) {
		System.out.println("\n***** Machine reprovision *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		client.requestMachineReprovision(resource.getId());
	}
	
	private static void testMigrateStorage(POCClient client, String machineName, String datastoreName) {
		System.out.println("\n***** Test migrate storage *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("targetDatastoreName", datastoreName);
		client.submitCustomRequest(resource.getId(), "Change storage", data);
	}
	
	private static void testChangeProperties(POCClient client, String machineName, String key, String value) throws JsonProcessingException {
		System.out.println("\n***** Test change properties *****");
		Collection<CatalogResource> resources = client.getCatalogResources(machineName, 1);
		CatalogResource resource = resources.iterator().next();
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(key, value);
		client.requestPropertyChange(resource.getId(), properties);
	}
	
	private static void testGetResourceConsumption(VROPSClient vrops, String vmName) {
		System.out.println("\n***** Test resource consumption *****");
		ResourceConsumption rc = vrops.getVMMetricsByName(vmName);
		System.out.println("cpu: " + rc.getCpuMHz() + " memory: " + rc.getMemoryKB() + " storage: " + rc.getStorageMB());
	}
	
	private static void testGetGroupConsumption(VROPSClient vrops, String ait) {
		System.out.println("\n***** Test group resource consumption *****");
		AggregateResourceConsumption rcs = vrops.getGroupConsumption("ait", ait);
		for(Map.Entry<String, ResourceConsumption> entry : rcs.getMachines().entrySet()) {
			System.out.println(entry.getKey() + " cpu: " + entry.getValue().getCpuMHz() + " memory: " +entry.getValue().getMemoryKB() + " storage: " + entry.getValue().getStorageMB());
		}
		ResourceConsumption totals = rcs.getTotals();
		System.out.println("Totals: " + totals.getCpuMHz() + " memory: " +totals.getMemoryKB() + " storage: " + totals.getStorageMB());
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("***** Login *****");
		POCClient client = new POCClient(args[0], args[1], args[2], args.length == 4 ? args[3] : null);
		testGetCatalogItems(client); 
		testGetResources(client);
		//testGetBusinessGroups(client, args[3]);
		//testRequestMachine(client, args[3], "CentOS7 Minmal", "East", "ThisIsMyOU", "Development Sandbox", "1234");
		//testGetDay2Operations(client, "dev-0091"); 
		testReconfigureMachine(client, "dev-0145", new MachineConfiguration(3, 1024, 40, null));
		testGetDay2Operations(client, "dev-0091"); 
		//testMachineReprovision(client, "dev-0092");
		//testDestroyMachine(client, "dev-0100");
		//testGetReservationPolicies(client);
		testGetMachineInfo(client, "dev-0091");	
		testMigrateStorage(client, "dev-0091", "vivaldi");
		testChangeProperties(client, "dev-0134", "bac_tag_ait1", "1113");
		//VROPSClient vrops = client.createVROPSClient("https://vrops-01.rydin.nu/suite-api", "admin", args[2]);
		//testGetResourceConsumption(vrops, "dev-0091");
		//testGetGroupConsumption(vrops, "1234");
	}
}
