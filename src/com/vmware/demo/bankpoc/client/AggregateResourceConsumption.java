package com.vmware.demo.bankpoc.client;

import java.util.List;
import java.util.Map;

public class AggregateResourceConsumption {
	private final ResourceConsumption totals;
	
	private final Map<String, ResourceConsumption> machines;

	public AggregateResourceConsumption(ResourceConsumption totals,
			Map<String, ResourceConsumption> machines) {
		super();
		this.totals = totals;
		this.machines = machines;
	}

	public ResourceConsumption getTotals() {
		return totals;
	}

	public Map<String, ResourceConsumption> getMachines() {
		return machines;
	}
}
