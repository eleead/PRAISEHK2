package ust.hk.praisehk.metamodelcalibration.measurements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

import transitCalculatorsWithFare.FareLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.SUEModelOutput;

public class Measurement {
	
	/**
	 * Some attributes name are kept as public and final string
	 */
	public static final String linkListAttributeName="LINK_LIST";
	public static final String transitLineAttributeName="TRANSIT_LINE";
	public static final String transitRouteAttributeName="Transit_Route";
	public static final String transitBoardingStopAtrributeName="transit_boarding_stop";
	public static final String transitAlightingStopAttributeName="transit_alighting_stop";
	public static final String transitModeAttributeName="transitMode";
	public static final String FareLinkAttributeName=FareLink.FareLinkAttributeName;
	public static final String FareLinkClusterAttributeName = "fare_link_cluster";
	public static final String MaaSPackageAttributeName = "MaaSPackage";
	public static final String MTRLineRouteStopLinkInfosName = "lineRouteLinkStops";
	
	private final Id<Measurement> id;
	private Map<String,Object> attributes=new HashMap<>();
	private final Map<String,Tuple<Double,Double>> timeBean;
	private Map<String,Double> volumes=new ConcurrentHashMap<>();
	private Map<String,Double> sd = new ConcurrentHashMap<>();
	private static final Logger logger=Logger.getLogger(Measurement.class);
	private Coord coord=null;
	private final MeasurementType measurementType;
	
	public Coord getCoord() {
		return coord;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	protected Measurement(String id, Map<String,Tuple<Double,Double>> timeBean,MeasurementType mType) {
		this.id=Id.create(id, Measurement.class);
		this.timeBean=timeBean;
		this.attributes.put(linkListAttributeName, new ArrayList<Id<Link>>());
		this.measurementType=mType;
	}
	/**
	 * Sorry for the confusing name. maybe put volume is more suitable. No it do not add. Rather it replaces the old volume
	 * @param timeBeanId
	 * @param volume
	 */
	public void putVolume(String timeBeanId,double volume) {
		
		if(!this.timeBean.containsKey(timeBeanId)){
			logger.error("timeBean do not contain timeBeanId"+timeBeanId+", please check.");
			logger.warn("Ignoring volume for timeBeanId"+timeBeanId);
		}else {
			this.volumes.put(timeBeanId, volume);
			if(!this.sd.containsKey(timeBeanId))this.sd.put(timeBeanId, 0.);
		}
	}
	
	
	public void putSD(String timeBeanId,double SD) {
		if(!this.timeBean.containsKey(timeBeanId)){
			logger.error("timeBean do not contain timeBeanId"+timeBeanId+", please check.");
			logger.warn("Ignoring volume for timeBeanId"+timeBeanId);
		}else {
			this.sd.put(timeBeanId, SD);
		}
	}
	
	/**
	 * Call to this method with this.linkListAttributeName String will 
	 * return a ArrayList<Id<Link>> containing the link Ids of all the links in this measurement
	 * @param attributeName
	 * @return
	 */
	public Object getAttribute(String attributeName) {
		return this.attributes.get(attributeName);
	}
	
	public Id<Measurement> getId() {
		return id;
	}

	public Map<String, Tuple<Double, Double>> getTimeBean() {
		return timeBean;
	}

	public void setAttribute(String attributeName, Object attribute) {
		this.attributes.put(attributeName, attribute);
	}
	public Map<String,Double> getVolumes(){
		return this.volumes;
	}
	
	public Map<String,Double> getSD(){
		return this.sd;
	}
	
	public Measurement clone() {
		Measurement m=new Measurement(this.id.toString(),new HashMap<>(timeBean),this.measurementType);
		for(String s:this.volumes.keySet()) {
			m.putVolume(s, this.getVolumes().get(s));
		}
		
		for(String s:this.sd.keySet()) {
			m.putSD(s, this.getSD().get(s));
		}
		
		for(String s:this.attributes.keySet()) {
			m.setAttribute(s, this.attributes.get(s));
		}
		return m;
	}
	
	public double getVolume(String timeBean) {
		return this.volumes.get(timeBean);
	}
	
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * Default implementation of updater.
	 * Should be overridden if necessary.
	 * @param linkVolumes
	 * is a Map<timeBeanId,Map<Id<Link>,Double>> containing link traffic volumes of all the time beans
	 */
	@SuppressWarnings("unchecked")
	public void updateMeasurement(SUEModelOutput modelOut,AnalyticalModel sue,Object otherDataContainer) {
		this.measurementType.updateMeasurement(modelOut, sue, otherDataContainer,this);
	}

	public MeasurementType getMeasurementType() {
		return measurementType;
	}
	
}
