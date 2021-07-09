package ust.hk.praisehk.metamodelcalibration.analyticalModelImpl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelNetwork;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitTransferLink;



/**
 * 
 * @author Ashraf
 *
 */
public class CNLTransitTransferLink extends TransitTransferLink {
	private double headway=0;
	private double capacity;
	private double currentOnboardPassenger=0;	
	private final Id<TransitLink> trLinkId; 
	private CNLTransitDirectLink nextdLink;
	private Set<Id<TransitLink>> incidentLinkIds;
	
	public CNLTransitTransferLink(String startStopId, String endStopId, 
			Id<Link> startLinkId, Id<Link> endLinkId,TransitSchedule ts,
			CNLTransitDirectLink dlink) {
		super(startStopId, endStopId, startLinkId, endLinkId);
		this.nextdLink=dlink;
		if(dlink!=null) {
			this.trLinkId=Id.create(dlink.getLineId().replaceAll("\\s+", "")+"_"+dlink.getRouteId().replaceAll("\\s+", "")+
					"_"+dlink.getStartStopId().replaceAll("\\s+", ""),TransitLink.class);
		}else {
			this.trLinkId=Id.create("Destination",TransitLink.class);
		}
	}
	

	

	
	/**
	 * the network is not needed that much for this function
	 */
	@Override
	public void addPassanger(double d, AnalyticalModelNetwork Network) {
		this.passangerCount+=d;
	}

	/**
	 * this method calculates waiting time depending on the following formulae
	 * 
	 * waiting time=alpha/Frequency+1/Frequency*((PassengerTryingToBoard+PassengeronBoard)/(Frequency*Capacity))^beta
	 * for default value of alpha and beta, BPR function alpha beta has been used. 
	 * 
	 * returns 0 if this transfer link is the last one of the trip 
	 */
	@Override
	public double getWaitingTime(LinkedHashMap<String,Double>params,LinkedHashMap<String,Double> anaParams,AnalyticalModelNetwork network) {
		
		if(this.nextdLink!=null) {
			headway=this.nextdLink.getHeadway();
			capacity=this.nextdLink.getCapacity()*params.get(CNLSUEModel.CapacityMultiplierName);
			double noOfVehicles=this.nextdLink.getFrequency();
			CNLLink l_gamma = ((CNLLink)network.getLinks().get(this.nextdLink.getLinkList().get(0)));
			currentOnboardPassenger = l_gamma.getTransitPassengerVolume(this.nextdLink.getLineId()+"_"+this.nextdLink.getRouteId());
			if(this.incidentLinkIds==null || this.incidentLinkIds.size()==0)this.incidentLinkIds = l_gamma.getTransitDirectLinks(this.nextdLink.getLineId()+"_"+this.nextdLink.getRouteId());
			this.waitingTime=headway*anaParams.get(CNLSUEModel.TransferalphaName)+
					headway*Math.pow((this.currentOnboardPassenger)/(capacity*noOfVehicles),anaParams.get(CNLSUEModel.TransferbetaName));
			//can add this term : this.passangerCount+ to the current onborad passenger but I think it is overcounting. 
			if(Double.isNaN(this.waitingTime)||this.waitingTime==Double.POSITIVE_INFINITY) {
				return this.waitingTime=3600;
			}			
			return this.waitingTime;
			
		}else {
			return 0;
		}
	}





	@Override
	public Id<TransitLink> getTrLinkId() {
		
		return this.trLinkId;
	}
	
	public CNLTransitTransferLink cloneLink(CNLTransitTransferLink tl,CNLTransitDirectLink dlink) {
		return new CNLTransitTransferLink(tl.getStartStopId(),tl.getEndStopId(),tl.getStartingLinkId(),tl.getEndingLinkId(),null,dlink);
	}

	

	public Set<Id<TransitLink>> getIncidentLinkIds() {
		return incidentLinkIds;
	}





	public CNLTransitDirectLink getNextdLink() {
		return nextdLink;
	}
}
