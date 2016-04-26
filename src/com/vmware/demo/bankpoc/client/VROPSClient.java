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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vmware.ops.api.client.Client;
import com.vmware.ops.api.client.Client.ClientConfig;
import com.vmware.ops.api.client.controllers.ResourcesClient;
import com.vmware.ops.api.model.common.PageInfo;
import com.vmware.ops.api.model.resource.ResourceDto;
import com.vmware.ops.api.model.resource.ResourceDto.ResourceRelationDto;
import com.vmware.ops.api.model.resource.ResourceQuery;
import com.vmware.ops.api.model.resource.ResourceDto.ResourceDtoList;
import com.vmware.ops.api.model.stat.LatestStatQuery;
import com.vmware.ops.api.model.stat.Stat;
import com.vmware.ops.api.model.stat.Stat.ResourcesStats;

public class VROPSClient {
	private final Client vrops;
	
	private final Map<String, ResourceDto> resourceCache = new HashMap<String, ResourceDto>();
	
	/**
	 * Creates a new connection to vR Ops
	 * @param url The URL to the vR Ops API 
	 * @param username Username
	 * @param password Password
	 * @param verifyCertificate True if certificate verification is needed
	 */
	public VROPSClient(String url, String username, String password, boolean verifyCertificate) {
		vrops = ClientConfig.builder()
                .basicAuth(username, password)
                .useJson()
                .serverUrl(url)
                .verify(Boolean.toString(verifyCertificate))
                .ignoreHostName(!verifyCertificate)
                .build()
                .newClient();	
		}
	
	/**
	 * Returns a resource ID based on a resource name
	 * @param resourceKindKey The resorce kind
	 * @param name The resource name
	 * @return
	 */
	public ResourceDto findResourceByName(String resourceKindKey, String name) {
		ResourcesClient rc = vrops.resourcesClient();
		ResourceQuery q = new ResourceQuery();
		String cacheKey = "" + resourceKindKey + ":" + name;
		ResourceDto resource = resourceCache.get(cacheKey);
		if(resource == null) {
			if(resourceKindKey != null)
				q.setResourceKind(new String[] { resourceKindKey });
			q.setName(new String[] { name });
			ResourceDtoList result = rc.getResources(q, new PageInfo(0, 1, 1));
			resource = result.getResourceList().size() > 0 ? result.getResourceList().get(0) : null;
			resourceCache.put(cacheKey, resource);
		}
		return resource;
	}
	
	/**
	 * Returns resource consumption data based on a VM name
	 * @param vmName The vm name
	 * @return
	 */
	public ResourceConsumption getVMMetricsByName(String vmName) {
		ResourcesClient rc = vrops.resourcesClient();
		LatestStatQuery query = new LatestStatQuery();
		ResourceDto res = this.findResourceByName("VirtualMachine", vmName);
		query.setResourceId(new UUID[] { res.getIdentifier() });
		query.setStatKey(new String[] { "cpu|usagemhz_average", "mem|consumed_average", "diskspace|used" });
		ResourcesStats stats = rc.getLatestStats(query);
		List<Stat> statList = stats.getValues().get(0).getStats().getStatList();
		return new ResourceConsumption(statList.get(0).getData()[0], statList.get(2).getData()[0], statList.get(3).getData()[0]);
	}
	
	/**
	 * Returns consumption data based on a VM ID
	 * @param vmId The VM ID
	 * @return
	 */
	public ResourceConsumption getVMMetricsByMachineID(UUID vmId) {
		ResourcesClient rc = vrops.resourcesClient();
		LatestStatQuery query = new LatestStatQuery();
		query.setResourceId(new UUID[] { vmId });
		query.setStatKey(new String[] { "cpu|usagemhz_average", "mem|consumed_average", "diskspace|used" });
		ResourcesStats stats = rc.getLatestStats(query);
		if(stats.getValues().size() == 0)
			return null;
		List<Stat> statList = stats.getValues().get(0).getStats().getStatList();
		return new ResourceConsumption(statList.get(0).getData()[0], statList.get(2).getData()[0], statList.get(3).getData()[0]);
	}
	
	/**
	 * Returns aggregate resource consumption based on an AIT
	 * @param ait The AIT
	 * @return
	 */
	public AggregateResourceConsumption getGroupConsumption(String tagName, String tagValue) {
		double totCpu = 0.0;
		double totMem = 0.0;
		double totStorage = 0.0;
		Map<String, ResourceConsumption> machines = new HashMap<String, ResourceConsumption>();
		ResourceDto res = this.findResourceByName("Environment", tagName + "-" + tagValue);
		ResourcesClient rc = vrops.resourcesClient();
		PageInfo page = new PageInfo(0, 10000, 10000);
		ResourceRelationDto children = rc.getChildren(res.getIdentifier(), page);
		for(ResourceDto child : children.getResourceList()) {
			ResourceConsumption thisMachine = this.getVMMetricsByMachineID(child.getIdentifier());
			if(thisMachine == null)
				continue;
			totCpu += thisMachine.getCpuMHz();
			totMem += thisMachine.getMemoryKB();
			totStorage = thisMachine.getStorageMB();
			machines.put(child.getResourceKey().getName(), thisMachine);
		}
		return new AggregateResourceConsumption(new ResourceConsumption(totCpu, totMem, totStorage), machines);
	}
}
