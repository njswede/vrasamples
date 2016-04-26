/*
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF 
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.vmware.demo.bankpoc.client;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vmware.cafe.consumer.ConsumerService;
import com.vmware.vcac.authentication.rest.client.service.SubtenantService;
import com.vmware.vcac.authentication.rest.stubs.Subtenant;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerCatalogItemService;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerEntitledCatalogItemService;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerRequestService;
import com.vmware.vcac.catalog.rest.client.service.impl.ConsumerResourceService;
import com.vmware.vcac.catalog.rest.stubs.CatalogItem;
import com.vmware.vcac.catalog.rest.stubs.CatalogItemProvisioningRequest;
import com.vmware.vcac.catalog.rest.stubs.CatalogResourceRequest;
import com.vmware.vcac.catalog.rest.stubs.CatalogResourceView;
import com.vmware.vcac.catalog.rest.stubs.ConsumerResourceOperation;
import com.vmware.vcac.catalog.rest.stubs.Request;
import com.vmware.vcac.catalog.rest.stubs.v7_0.CatalogResource;
import com.vmware.vcac.core.reservation.rest.client.service.ReservationPolicyService;
import com.vmware.vcac.platform.rest.client.RestClient;
import com.vmware.vcac.platform.rest.client.query.FilterParam;
import com.vmware.vcac.platform.rest.client.query.OdataQuery;
import com.vmware.vcac.platform.rest.client.query.PageOdataRequest;
import com.vmware.vcac.platform.security.SslCertificateTrust;
import com.vmware.vcac.reservation.rest.stubs.ReservationPolicy;

/**
 * A simple wrapper for vRA API functionality to be used during a POC.
 * 
 * DO NOT USE IN PRODUCTION!
 * 
 * @author prydin@vmware.com
 *
 */
public class POCClient {
	private ConsumerService service;
	private RestClient catalogClient;
	private RestClient identityClient;
	private RestClient reservationClient;

	private static final String CATALOG_SERVICE = "catalog-service";
	private static final String RESERVATION_SERVICE = "reservation-service";
	private static final String IDENTITY_SERVICE = "identity";

	public POCClient(String url, String user, String password, String tenant) {
		service = new ConsumerService(url, SslCertificateTrust.ALWAYS_TRUST);
		if(tenant == null) 
			service.authenticate(user, password);
		else
			service.authenticate(tenant, user, password);
		catalogClient = service.getDefaultRestClientForService(CATALOG_SERVICE);
		catalogClient.setMediaType(MediaType.APPLICATION_JSON);
		identityClient = service.getDefaultRestClientForService(IDENTITY_SERVICE);
		identityClient.setMediaType(MediaType.APPLICATION_JSON);
		reservationClient = service.getDefaultRestClientForService(RESERVATION_SERVICE);
		reservationClient.setMediaType(MediaType.APPLICATION_JSON);
	}

	/**
	 * Returns a list of catalog item based on a name. If the name is specified,
	 * the method returns any catalog item with a name that starts with the string specified. 
	 * 
	 * @param name Starting characters to match. All items returned if null.
	 * @param limit Maximum number of catalog items to return
	 * @return
	 */
	public Collection<CatalogItem> getCatalogItems(String name, int limit) {
		ConsumerCatalogItemService catalogItemService = new ConsumerCatalogItemService(catalogClient);
		OdataQuery query = OdataQuery.query().addAscOrderBy("name");
		if(name != null) 
			query.addFilter(FilterParam.startsWith("name", "'" + name + "'"));
		Pageable page = PageOdataRequest.page(1, limit, query);
		return catalogItemService.getCatalogItems(page).getContent();
	}

	/**
	 * Returns a list of catalog resource based on a name. If the name is specified,
	 * the method returns any catalog resource with a name that starts with the string specified. 
	 * 
	 * @param name Starting characters to match. All items returned if null.
	 * @param limit Maximum number of catalog resource to return
	 * @return
	 */
	public Collection<CatalogResource> getCatalogResources(String name, int limit) {
		ConsumerResourceService consumerResourceService = new ConsumerResourceService(catalogClient);
		OdataQuery query = OdataQuery.query().addAscOrderBy("name");
		if(name != null) 
			query.addFilter(FilterParam.startsWith("name", "'" + name + "'"));
		Pageable page = PageOdataRequest.page(1, limit, query);
		return consumerResourceService.getResourcesList(true, true, page);
	}
	
	/**
	 * Returns a list of all business groups for a tenant.
	 * If a name is specified, the method returns any business group with a name that starts with the string specified.
	 * @param tenantId The if of a tenant
	 * @param name Name to match. Passing null returns all business groups for a tenant.
	 * @param limit Maximum number of business groups to return.
	 * @return
	 */
	public Set<Subtenant> getBusinessGroups(String tenantId, String name, int limit) {
		SubtenantService subtenantService = new SubtenantService(identityClient);
		OdataQuery query = OdataQuery.query().addAscOrderBy("name");
		if(name != null) 
			query.addFilter(FilterParam.startsWith("name", "'" + name + "'"));
		Pageable page = PageOdataRequest.page(1, limit, query);
		return subtenantService.getSubtenants(tenantId, page);
	}
	
	/**
	 * Returns a list of day 2 operations for an existing resource.
	 * @param resourceId The id of the resource.
	 * @return A list of day 2 operations.
	 */
	public Collection<ConsumerResourceOperation> getDay2Operations(String resourceId) {
		ConsumerResourceService resourceService = new ConsumerResourceService(catalogClient);
		return resourceService.getAvailableOperations(resourceId);
	}
	/**
	 * Returns a day 2 operation on an existing resource based on its resource id and 
	 * operation id.
	 * @param resourceId The resource ID
	 * @param opId The operation ID
	 * @return A day 2 operation
	 */
	public ConsumerResourceOperation getDay2OperationById(String resourceId, String opId) {
		ConsumerResourceService resourceService = new ConsumerResourceService(catalogClient);
		Collection<ConsumerResourceOperation> ops = this.getDay2Operations(resourceId);
		for(ConsumerResourceOperation op : ops) {
			if(opId.equals(op.getBindingId())) {
				return op;
			}
		}
		return null;
	}
	
	/**
	 * Returns a day 2 operation on an existing resource based on its resource id and 
	 * operation name.
	 * @param resourceId The resource ID
	 * @param opName The operation name
	 * @return A day 2 operation
	 */
	public ConsumerResourceOperation getDay2OperationByName(String resourceId, String opName) {
		ConsumerResourceService resourceService = new ConsumerResourceService(catalogClient);
		Collection<ConsumerResourceOperation> ops = this.getDay2Operations(resourceId);
		for(ConsumerResourceOperation op : ops) {
			if(opName.equals(op.getName())) {
				return op;
			}
		}
		return null;
	}
	
	/**
	 * Loads a request based on a URI.
	 * @param uri The URI of the request to load.
	 * @return
	 */
	public Request getRequestFromURI(URI uri) {
		ConsumerRequestService requestService = new ConsumerRequestService(catalogClient);
		return requestService.getRequest(uri);
	}
	
	/**
	 * Requests the provisioning of a machine.
	 * 
	 * @param catalogItemId The ID of the catalog item to provision.
	 * @param businessGroupId The business group to place the machine in. If null, the machine goes in the default business group of the blueprint.
	 * @param config The machine configuration.
	 * @param customProperties Custom properties (if any). Can be null if no custom properties are needed.
	 * @return
	 */
	public URI requestMachine(String catalogItemId, String businessGroupId, Map<String, MachineConfiguration> config, Map<String, Object> customProperties) {
		// Fetch a template request. Requests are complex and highly dynamic, so it's a best 
		// practice to start from a template.
		//
		ConsumerEntitledCatalogItemService entitledItemsService = new ConsumerEntitledCatalogItemService(catalogClient);
		CatalogItemProvisioningRequest request = entitledItemsService.getTemplateCatalogItemProvisioningRequest(catalogItemId);
		if(businessGroupId != null)
			request.setBusinessGroupId(businessGroupId);
		
		// Iterate through the configurations (if specified) and try to find the corresponding
		// item in the request. Thus, if config is empty, we will retain the default values.
		// This code needs a lot more error checking in production! The map navigation code is
		// not typesafe!
		//
		Map<String, Object> data = request.getData();
		if(config != null) {
			this.applyConfiguration(config, customProperties, data);
		}
		System.out.println(data);
		return entitledItemsService.submitCatalogItemProvisionRequest(request);
	}
	
	/**
	 * Submits a request for a machine reconfiguration
	 * 
	 * @param machineId The ID of the machine to reconfigure
	 * @param config The new configuration
	 * @param allowShutdown Set to "true" if the machine is allowed to be shut down during reconfiguration.
	 * @return
	 */
	public URI requestMachineChange(String machineId, MachineConfiguration config, boolean allowShutdown) {
		CatalogResourceRequest request = this.getTemplate(machineId, "Infrastructure.Machine.Action.Reconfigure");
		Map<String, Object> data = request.getData();
		data.put("cpu", config.getNumCPUs());
		data.put("memory", config.getMemoryMB());
		data.put("allowForceShutdown", allowShutdown); // Allow shutdown
		data.put("Cafe.Shim.VirtualMachine.Reconfigure.AllowForceShutdown", allowShutdown ? "True" : "False");
		data.put("Cafe.Shim.VirtualMachine.Reconfigure.Requestor", 1);
		//data.put("executionSelector", 1); // Immediate shutdown
		data.put("powerActionSelector", 0); // Power-off allowed
		return this.submitResourceRequest(machineId, request);
	}
	
	
	public URI requestPropertyChange(String machineId, Map<String, String> properties) throws JsonProcessingException  {
		// Update vR Ops grouping
		//
		Map<String, Object> payload = new HashMap<String, Object>();
		ObjectMapper om = new ObjectMapper();
		payload.put("properties", om.writer().writeValueAsString(properties));
		this.submitCustomRequest(machineId, "Refresh vR Ops grouping", payload);
		
		// Update the properties in vRA
		//
		CatalogResourceRequest request = this.getTemplate(machineId, "Infrastructure.Machine.Action.Reconfigure");
		Map<String, Object> data = request.getData();
		data.put("allowForceShutdown", false); // Allow shutdown
		data.put("Cafe.Shim.VirtualMachine.Reconfigure.AllowForceShutdown", "False");
		data.put("Cafe.Shim.VirtualMachine.Reconfigure.Requestor", 1);
		data.put("powerActionSelector", 0); // Power-off allowed
		Collection<Map<String, Object>> propList = (Collection<Map<String, Object>>) data.get("customProperties");
		for(Map<String, Object> propRecord : propList) {
			if(!"Infrastructure.CustomProperty".equals(propRecord.get("classId"))) 
				continue;
			Map<String, String> prop = (Map<String, String>) propRecord.get("data");
			String name = prop.get("id");
			if(properties.containsKey(name))  {
				// Set property if found and remove it from the list
				// of properties to set.
				//
				prop.put("value", properties.get(name));
				properties.remove(name);
			}
		}
		
		// Any remaining values in "properties" are net new and 
		// need to be created separately
		//
		for(Map.Entry<String, String> entry : properties.entrySet()) {
			Map<String, Object> propRecord = new HashMap<String, Object>();
			propRecord.put("classId", "Infrastructure.CustomProperty");
			Map<String, String> prop = new HashMap<String, String>();
			prop.put("id", entry.getKey());
			prop.put("value", entry.getValue());
			propRecord.put("data", prop);
			propList.add(propRecord);
		}
		return this.submitResourceRequest(machineId, request);
	}
	
	/**
	 * Submits a custom request, i.e. an XaaS day 2 operation.
	 * 
	 * @param machineId ID of the machine
	 * @param operationName Name (not ID!) of the operation to run
	 * @param rqData Additional request data, such as request parameters.
	 * @return
	 */
	public URI submitCustomRequest(String machineId, String operationName, Map<String, Object> rqData) {
		ConsumerResourceOperation op = this.getDay2OperationByName(machineId, operationName);
		CatalogResourceRequest request = new CatalogResourceRequest();
		request.setActionId(op.getId());
		request.setResourceId(machineId);
		Map<String, Object> data = request.getData();
		for(Map.Entry<String, Object> entry : rqData.entrySet()) 
			data.put(entry.getKey(), entry.getValue());
		request.setData(data);
		return this.submitResourceRequest(machineId, request);
	}
	
	/**
	 * Returns true if a reconfigure machine request is pending.
	 * 
	 * @param machineId The machine to check.
	 * @return
	 */
	public boolean isChangePending(String machineId) {
		return this.getDay2OperationById(machineId, "Infrastructure.Machine.Action.ExecuteReconfigure") != null;
	}
	
	/**
	 * Finalizes a machine change (not needed of immediate machine reconfigure is requested)
	 * 
	 * @param machineId The ID of the machine to reconfigure.
	 * @return
	 */
	public URI finalizeMachineChange(String machineId) {
		CatalogResourceRequest request = this.getTemplate(machineId, "Infrastructure.Machine.Action.ExecuteReconfigure");
		return this.submitResourceRequest(machineId, request);
	}
	
	/**
	 * Submits a request for machine reprovisioning.
	 * 
	 * @param machineId The ID of the machine to reprovision.
	 * @return
	 */
	public URI requestMachineReprovision(String machineId) {
		CatalogResourceRequest request = this.getTemplate(machineId, "Infrastructure.Machine.Action.Reprovision");
		return this.submitResourceRequest(machineId, request);
	}
	
	/**
	 * Destroys a virtual machine or other resource. 
	 * @param machineId The id of the machine to destroy.
	 * @return
	 */
	public URI destroyMachine(String machineId) {
		CatalogResourceRequest request = this.getTemplate(machineId, "Infrastructure.Virtual.Action.Destroy");
		return this.submitResourceRequest(machineId, request);
	}
	
	/**
	 * Returns a list of reservation policies based on a name. If the name is specified,
	 * the method returns any reservation policy with a name that starts with the string specified. 
	 * 
	 * @param name Starting characters to match. All items returned if null.
	 * @param limit Maximum number of reservation policies to return
	 * @return A list of reservation policies.
	 */
	public Collection<ReservationPolicy> getReservationPolicies(String name, int limit) {
		ReservationPolicyService reservationPolicyService = new ReservationPolicyService(reservationClient);
		OdataQuery query = OdataQuery.query().addAscOrderBy("name");
		if(name != null) 
			query.addFilter(FilterParam.startsWith("name", "'" + name + "'"));
		Pageable page = PageOdataRequest.page(1, limit, query);
		return reservationPolicyService.getAllReservationPolicies(page).getContent();
	}
	
	/**
	 * Returns a map of detailed machine information.
	 * 
	 * @param machineId The machine ID to obtain information for.
	 * @return
	 */
	public Map<String, Object> getMachineDetails(String machineId) {
		return catalogClient.get("/consumer/resourceViews/" + machineId, HashMap.class);
	}
	
	/**
	 * Returns a client to a vR Ops system
	 * @param url vR Ops URL	
	 * @param username vR Ops username
	 * @param password vR Ops password
	 * @return
	 */
	public VROPSClient createVROPSClient(String url, String username, String password) {
		return new VROPSClient(url, username, password, false);
	}
	
	/**
	 * Applies machine configuration and custom properties to a provisioning request.
	 * 
	 * @param config The machine configuration
	 * @param customProperties Custom properties to be added to the request.
	 * @param data The request data to add the information to.
	 */
	protected void applyConfiguration(Map<String, MachineConfiguration> config,
			Map<String, Object> customProperties, Map<String, Object> data) {
		for(Map.Entry<String, MachineConfiguration> element : config.entrySet()) {
			Object o = data.get(element.getKey());
			if(o == null)
				throw new IllegalStateException("Element " + element.getKey() + " not found in template"); 
			Map<String, Object> machineConfig = ((Map<String, Map<String, Object>>) o).get("data");
			machineConfig.put("cpu", element.getValue().getNumCPUs());
			machineConfig.put("memory", element.getValue().getMemoryMB());
			if(customProperties != null) {
				for(Map.Entry<String, Object> property : customProperties.entrySet()) 
					machineConfig.put(property.getKey(), property.getValue());
			}
		}
	}
	
	/**
	 * Returns a request template based on a machine ID and an operation name.
	 * @param machineId The ID of the machine to obtain a template for
	 * @param operationName The name of the operation to obtain a template for.
	 * @return
	 */
	protected CatalogResourceRequest getTemplate(String machineId, String operationName) {
		ConsumerResourceService resourceService = new ConsumerResourceService(catalogClient);
		ConsumerResourceOperation op = this.getDay2OperationById(machineId, operationName);
		if(op == null) 
			throw new IllegalStateException("Operation " + operationName + " not found");
		return resourceService.getTemplateResourceRequest(machineId, op.getId());
	}
	
	/**
	 * Executes resource request, i.e. a day 2 operation on a machine.
	 * @param machineId The ID of the machine to execute an operation for.
	 * @param request The request to execute.
	 * @return
	 */
	protected URI submitResourceRequest(String machineId,
			CatalogResourceRequest request) {
		Map<String, Object> payload = new HashMap<String, Object>();
		payload.put("resourceId", request.getResourceId());
		payload.put("actionId", request.getActionId());
		payload.put("description", request.getDescription());
		payload.put("data", request.getData());
		payload.put("type", "com.vmware.vcac.catalog.domain.request.CatalogResourceRequest");	
		return catalogClient.post("consumer/resources/" + machineId + "/actions/" + request.getActionId() + "/requests", payload);
	}
}
