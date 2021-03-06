package org.cloudbus.cloudsim.simulate;

import org.cloudbus.cloudsim.Log;

public class PartnerInfomation {

	private int partnerId;
	
	private double ratio;
	
	private double requested;
	
	private double satified;
	
	/**
	 * L in argithorm
	 */
	private double lenghtRatio;
	
	/**
	 * l in argithorm
	 */
	private double kRatio;

	
	public PartnerInfomation(int partnerId, double ratio, double requested,
			double satified,double lenghtRatio,  double kRatio) {
		super();
		this.partnerId = partnerId;
		this.ratio = ratio;
		this.requested = requested;
		this.satified = satified;
		this.lenghtRatio = lenghtRatio;
		this.kRatio = kRatio;
	}

	public PartnerInfomation(int partnerId) {
		super();
		this.partnerId = partnerId;
		this.ratio = 1;
		this.requested = 0;
		this.satified = 0;
		this.lenghtRatio = 0;
		this.kRatio = 0;
	}
	
	public PartnerInfomation(int partnerId, double ratio,double satified,double requested) {
		super();
		this.partnerId = partnerId;
		this.ratio = ratio;
		this.requested = requested;
		this.satified = satified;
		this.lenghtRatio = ratio;
		this.kRatio = 0;
	}

	@Override
	public String toString() {
		return "PartnerInfomation [partnerId=" + partnerId + ", ratio=" + ratio
				+ ", requested=" + requested + ", satified=" + satified + "lenghtRatio= " + lenghtRatio + "]";
	}
	
	/**
	 * deviation  =  ti so tong do dai dung dung i goi cho j tren tong do dai j goi cho i 
	 * @param request_lenght
	 * @param satify_lenght
	 * @return
	 */
	public double updateLenghtRatio(double request_lenght,double satify_lenght){
		double deviation;
		deviation = calcLenghtRatio(this.getRequested(),this.getSatified());
		setLenghtRatio(deviation);
		return deviation;
	}
	
	public double updateLenghtRatio(){
		double deviation;
		deviation = calcLenghtRatio(0,0);
		setLenghtRatio(deviation);
		return deviation;
	}
	
	public double updateRequested(double request_lenght){
		setRequested(getRequested()+request_lenght);
		return getRequested()+request_lenght;
	}
	
	public double updateSatified(double satify_lenght){
		setSatified(getSatified()+satify_lenght);
		return getSatified()+satify_lenght;
	}
	public double updateKRatio(){
		setkRatio(getKRatio());
		return getKRatio();
	}
	
	public double calcLenghtRatio(double request_lenght,double satify_lenght){
		double deviation;
		if(this.getSatified() != 0 ){
			deviation = (getSatified()+satify_lenght)/(this.getRequested()+request_lenght);
//			deviation = (this.getRequested()+request_lenght)/(getSatified()+satify_lenght);
		}
		else {
			deviation = 0;
		}
		return deviation;
	}
	
	/**
	 * K ratio = L/init_ratio
	 * @return
	 */
	public double getKRatio() {
		double k;
		if(this.getRatio() == 0 ){
			k = 1;
		} else {
//			k = Math.abs(getLenghtRatio()/getRatio()-1);
			k = getLenghtRatio()/getRatio() - 1;
		}
		return k;
	}
	
	public double getKRatioWithCurrentTask(double request_lenght,double satify_lenght) {
		double k;
		if(this.getRatio() == 0 ){
			k = 1;
		} else {
//			k = Math.abs((calcLenghtRatio(request_lenght, satify_lenght))/getRatio()-1);
			k = (calcLenghtRatio(request_lenght, satify_lenght))/getRatio()-1;
			Log.printLine("request_lenght: "+(getRequested()+request_lenght));
			Log.printLine("satify_lenght: "+(getSatified()+satify_lenght));
//			Log.printLine("L: "+calcLenghtRatio(request_lenght, satify_lenght));
			Log.printLine("L: "+calcLenghtRatio(0,0));
			Log.printLine("Alpha: "+getRatio());
			Log.printLine("K: "+k);
		}
		return k;
	}

	
	/**
	 * Getter & Setter Area
	 * @return
	 */
	public int getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(int partnerId) {
		this.partnerId = partnerId;
	}

	public double getRatio() {
		return ratio;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}

	public double getRequested() {
		return requested;
	}

	public void setRequested(double requested) {
		this.requested = requested;
		updateLenghtRatio();
	}

	public double getSatified() {
		return satified;
	}

	public void setSatified(double satified) {
		this.satified = satified;
		updateLenghtRatio();
	}

	public double getLenghtRatio() {
		return lenghtRatio;
	}

	public void setLenghtRatio(double lenghtRatio) {
		this.lenghtRatio = lenghtRatio;
	}

	/**
	 * @return the kRatio
	 */
	public double getkRatio() {
		return kRatio;
	}

	/**
	 * @param kRatio the kRatio to set
	 */
	public double setkRatio(double kRatio) {
		this.kRatio = kRatio;
		return this.kRatio;
	}

}

