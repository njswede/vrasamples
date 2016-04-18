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

public class MachineConfiguration {
	private final int numCPUs;
	
	private final int memoryMB;
	
	private final int diskGB;
	
	private final String reservationPolicyId;
	
	public MachineConfiguration(int numCPUs, int memoryMB, int diskGB, String reservationPolicyId) {
		super();
		this.numCPUs = numCPUs;
		this.memoryMB = memoryMB;
		this.diskGB = diskGB;
		this.reservationPolicyId = reservationPolicyId;
	}

	public int getNumCPUs() {
		return numCPUs;
	}

	public int getMemoryMB() {
		return memoryMB;
	}

	public int getDiskGB() {
		return diskGB;
	}
	
	public String reservationPolicyId() {
		return reservationPolicyId;
	}
}
