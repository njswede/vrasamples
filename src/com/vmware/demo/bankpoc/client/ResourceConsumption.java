package com.vmware.demo.bankpoc.client;

public class ResourceConsumption {
	private final double cpuMHz;
	
	private final double memoryKB;
	
	private final double storageMB;

	public ResourceConsumption(double cpuMHz, double memoryKB, double storageMB) {
		super();
		this.cpuMHz = cpuMHz;
		this.memoryKB = memoryKB;
		this.storageMB = storageMB;
	}

	public double getCpuMHz() {
		return cpuMHz;
	}

	public double getMemoryKB() {
		return memoryKB;
	}

	public double getStorageMB() {
		return storageMB;
	}
}
