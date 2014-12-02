package org.cloudbus.cloudsim.simulate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

public class CustomDatacenterBroker extends DatacenterBroker {
	public static final int STOPPED = 0;
	public static final int RUNNING = 1;

	private Map<Integer, Map<Integer, EstimationCloudletObserve>> cloudletEstimateObserveMap;
	
	private List<Cloudlet> estimationList;
	
	private int estimationStatus = STOPPED;
	private List<PartnerInfomation> partnersList = new ArrayList<PartnerInfomation>();
	protected Map<Integer,EstimationCloudletOfPartner> estimateCloudletofParnerMap;
	
	public CustomDatacenterBroker(String name) throws Exception {
		super(name);
		setEstimationList(new ArrayList<Cloudlet>());
		setCloudletEstimateObserveMap(new HashMap<Integer, Map<Integer, EstimationCloudletObserve>>());
		setPartnersList(new ArrayList<PartnerInfomation>());
		setEstimateCloudletofParnerMap(new HashMap<Integer, EstimationCloudletOfPartner>());
	}
	
	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		// Resource characteristics request
			case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
				processResourceCharacteristicsRequest(ev);
				break;
			// Resource characteristics answer
			case CloudSimTags.RESOURCE_CHARACTERISTICS:
				processResourceCharacteristics(ev);
				break;
			// VM Creation answer
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreate(ev);
				break;
			// A finished cloudlet returned
			case CloudSimTags.CLOUDLET_RETURN:
				processCloudletReturn(ev);
				break;
			// if the simulation finishes
			case CloudSimTags.END_OF_SIMULATION:
				shutdownEntity();
				break;
			case CloudSimTags.BROKER_ESTIMATE_NEXT_TASK:
				estimateNextTask();
				break;
				
			case CloudSimTags.BROKER_ESTIMATE_RETURN:
				processInternalEstimateReturn(ev);
				break;
				
			/* handle request send task to partner estimate form my datacenter  **/
			case CloudSimTags.PARTNER_INTERNAL_ESTIMATE_REQUEST:
				processPartnerCloudletInternalEstimateRequest(ev);
				break;
			/* handle request estimate from partner **/
			case CloudSimTags.PARTNER_ESTIMATE_REQUEST:
				handlerPartnerCloudletEstimateRequest(ev);
				break;
			//if the cloudle estimate result returned from partner
			case CloudSimTags.PARTNER_ESTIMATE_RETURN: 
				processReturnEstimateFromPartner(ev);
				break;

			// other unknown tags are processed by this method
			default:
				processOtherEvent(ev);
				break;
		}
	}

	@Override
	protected void submitCloudlets() {
		Log.printLine(this.getName() + " submit Cloudlet");
		for (Cloudlet cloudlet: getCloudletList()) {
			addCloudletToEstimationList(cloudlet);
			
			Log.printLine("Cloudlet #" + cloudlet.getCloudletId() + " has been submitted!");
		}
	}
	private void addCloudletToEstimationList(Cloudlet cloudlet) {
		getEstimationList().add(cloudlet);
		if (estimationStatus == STOPPED) {
			setEstimationStatus(RUNNING);
			sendNow(getId(), CloudSimTags.BROKER_ESTIMATE_NEXT_TASK);
		}
	}
	
	private void estimateNextTask() {
		if (getEstimationList().isEmpty()) {
			setEstimationStatus(STOPPED);
		} else {
			Cloudlet cloudlet = getEstimationList().get(0);
			createCloudletObserve(cloudlet);
			
			for (Integer datacenterId: getDatacenterIdsList()) {
				CustomResCloudlet rcl = new CustomResCloudlet(cloudlet);
				sendNow(datacenterId, CloudSimTags.DATACENTER_ESTIMATE_TASK, rcl);
			}
		}
	}
	
	private void createCloudletObserve(Cloudlet cloudlet) {
		int owner = cloudlet.getUserId();
		
		Map<Integer, EstimationCloudletObserve> observeMap;
		
		if (getCloudletEstimateObserveMap().containsKey(owner)) {
			observeMap = getCloudletEstimateObserveMap().get(owner);
		} else {
			observeMap = new HashMap<Integer, EstimationCloudletObserve>(); 
			getCloudletEstimateObserveMap().put(owner, observeMap);
		}
		
		EstimationCloudletObserve observe;
		if (observeMap.containsKey(cloudlet.getCloudletId())) {
			observe = observeMap.get(cloudlet.getCloudletId());
		} else {
			observe = new EstimationCloudletObserve(new CustomResCloudlet(cloudlet), new ArrayList<>(getDatacenterIdsList()));
			observeMap.put(cloudlet.getCloudletId(), observe);
		}
	}
	
	protected void processInternalEstimateReturn(SimEvent ev) {
		Log.printLine(getName() + ": Receive internal response from datacenter #" + ev.getSource());
		CustomResCloudlet re_rcl = (CustomResCloudlet) ev.getData();
		
		if (getCloudletEstimateObserveMap().containsKey(re_rcl.getUserId())) {
			Map<Integer, EstimationCloudletObserve> obserMap = getCloudletEstimateObserveMap().get(re_rcl.getUserId());
			EstimationCloudletObserve observe = obserMap.get(re_rcl.getCloudletId());
			observe.receiveEstimateResult(ev.getSource(), re_rcl);
			
			if (observe.isFinished()) {
				if (observe.isExecable()) {
					// TODO send request to exec
					Log.printLine(getName() + ": WE CAN EXEC THIS CLOUDLET");
//					CustomResCloudlet rcl = observe.getResCloudlet();
//					sendExecRequest(rcl.getBestDatacenterId(), rcl.getBestVmId(), rcl);
				} else {
					// TODO send request to partner
					Log.printLine(getName() + ": WE NEED HELP FROM PARTNER"); 
				}
				
				getEstimationList().remove(0);
				sendNow(getId(), CloudSimTags.BROKER_ESTIMATE_NEXT_TASK);
			}
		}
	}
	

	private void sendExecRequest(int targetDatacenterId, int vmId, CustomResCloudlet rcl) {
		rcl.getCloudlet().setVmId(vmId);
		
		for (int datacenterId: getDatacenterIdsList()) {
			if (datacenterId == targetDatacenterId) {
				sendNow(datacenterId, CloudSimTags.DATACENTER_EXEC_TASK, vmId);
			} else {
				sendNow(datacenterId, CloudSimTags.DATACENTER_CANCEL_ESTIMATED_TASK, vmId);
			}
		}
	}
	
	@Override
	protected void processResourceCharacteristicsRequest(SimEvent ev) {
		setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
		buildPartnerInfoList(CloudSim.getEntityList());
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloud Resource List received with "
				+ getDatacenterIdsList().size() + " resource(s)");
		for (Integer datacenterId : getDatacenterIdsList()) {
			sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
		}
	}
	/**
	 * Receive request estimate from own datacenter, process to send it to partner
	 * 
	 */
	@Override	
	protected void processPartnerCloudletInternalEstimateRequest(SimEvent ev){
		//TODO: not implement ratio
		Cloudlet cl = (Cloudlet) ev.getData();
		cl.setUserId(getId());
		CustomResCloudlet rCl = new CustomResCloudlet(cl); 
		List<Integer> partnerIdsList  = new ArrayList<Integer>();
		for( PartnerInfomation partnerInfo : this.getPartnersList()){
				Log.printLine(CloudSim.clock()+ ": "+ getName()+": #"+ getId() +" Cloudlet #"+ cl.getCloudletId()+ " have been send to broker #"+partnerInfo.getPartnerId());
				//send to partner
				send(partnerInfo.getPartnerId(), 0, CloudSimTags.PARTNER_ESTIMATE_REQUEST, cl);
				partnerIdsList.add(partnerInfo.getPartnerId());
		}
		EstimationCloudletOfPartner esOfPatner = new EstimationCloudletOfPartner(rCl, partnerIdsList);
		getEstimateCloudletofParnerMap().put(rCl.getCloudletId(), esOfPatner);
	}		
		
	
	/**
	 * Receive request estimate from partner. send it to add own datacenter to estimate
	 * s
	 */
	@Override
	public void handlerPartnerCloudletEstimateRequest(SimEvent ev){
		Cloudlet cl = (Cloudlet) ev.getData();
		this.addCloudletToEstimationList(cl);
		//DO MORE HERE
	}
	/**
	 * receive estimate result from partner
	 * @param ev
	 */
	@Override
	protected void processReturnEstimateFromPartner(SimEvent ev) {
		//TODO cloudlet not have finish time
		Cloudlet cl =(Cloudlet) ev.getData();
		Integer clouletId = cl.getCloudletId();
		Integer partnerId =  ev.getSource();
		CustomResCloudlet crl = new CustomResCloudlet(cl);
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Received estimate result from Broker #" + ev.getSource());
		EstimationCloudletOfPartner partnerCloudletEstimateList = getEstimateCloudletofParnerMap().get(clouletId);
		if (partnerCloudletEstimateList.getPartnerIdsList().contains(partnerId)) {
			partnerCloudletEstimateList.receiveEstimateResult(partnerId, crl);
			if (partnerCloudletEstimateList.isFinished()) {
				// send result to partner
				ResCloudlet resCloudlet = partnerCloudletEstimateList.getResCloudlet();
				Log.printLine(resCloudlet.getClouddletFinishTime());
				Log.printLine(resCloudlet.getCloudlet().getDeadlineTime());
				if(resCloudlet.getClouddletFinishTime() < resCloudlet.getCloudlet().getDeadlineTime()){
					sendNow(partnerId, CloudSimTags.PARTNER_EXEC, partnerCloudletEstimateList.getResCloudlet().getCloudlet());
				} else {
					Log.printLine(CloudSim.clock()+ " can not send cloudlet #"+resCloudlet.getCloudletId()+ " to any where, timeout");
					sendNow(getId(), CloudSimTags.CLOUDLET_RETURN, partnerCloudletEstimateList.getResCloudlet().getCloudlet());
				}
			}
		}
	}
	
	public void addDatacenter(int datacenterId) {
		getDatacenterIdsList().add(datacenterId);
	}

	/**
	 * Create list of partner information
	 * @param List o add Entity on system.
	 */
	private void buildPartnerInfoList(List<SimEntity> entityList) {
		for(SimEntity en: entityList){
			if (en instanceof DatacenterBroker  && en.getId() != getId()) {
				//TODO: i'm hardcode the ratio by 1. fix it;
			 PartnerInfomation partnerInfoItem   = new PartnerInfomation(en.getId(), 1);
			 this.getPartnersList().add(partnerInfoItem);
			}
		}
		Log.printLine("Debug: partner info list of borker: "+ getName());
		for( PartnerInfomation pt:  this.getPartnersList()){
			Log.printLine(pt.toString());
		}
		Log.printLine("");
	}
	
	/**
	 * getter and setter area
	 * @return
	 */

	public int getEstimationStatus() {
		return estimationStatus;
	}


	public void setEstimationStatus(int estimationStatus) {
		this.estimationStatus = estimationStatus;
	}


	public List<Cloudlet> getEstimationList() {
		return estimationList;
	}


	public void setEstimationList(List<Cloudlet> estimationList) {
		this.estimationList = estimationList;
	}

	public Map<Integer, Map<Integer, EstimationCloudletObserve>> getCloudletEstimateObserveMap() {
		return cloudletEstimateObserveMap;
	}

	public void setCloudletEstimateObserveMap(
			Map<Integer, Map<Integer, EstimationCloudletObserve>> cloudletEstimateObserveMap) {
		this.cloudletEstimateObserveMap = cloudletEstimateObserveMap;
	}

	public List<PartnerInfomation> getPartnersList() {
		return partnersList;
	}

	public void setPartnersList(List<PartnerInfomation> partnersList) {
		this.partnersList = partnersList;
	}

	public Map<Integer, EstimationCloudletOfPartner> getEstimateCloudletofParnerMap() {
		return estimateCloudletofParnerMap;
	}

	public void setEstimateCloudletofParnerMap(
			Map<Integer, EstimationCloudletOfPartner> estimateCloudletofParnerMap) {
		this.estimateCloudletofParnerMap = estimateCloudletofParnerMap;
	}

}
