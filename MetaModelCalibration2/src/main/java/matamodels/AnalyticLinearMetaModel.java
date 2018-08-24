package matamodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import de.xypron.jcobyla.Calcfc;
import de.xypron.jcobyla.Cobyla;
import de.xypron.jcobyla.CobylaExitStatus;

/**
 * 
 * @author Ashraf
 *
 */
public class AnalyticLinearMetaModel extends MetaModelImpl {
	/**
	 * a ridge penalty is added
	 * assuming constant of 1
	 * 
	 * The format is Y=Bo+B1*A+B(2-N+1)X
	 * Bo --> Const.
	 * B --> Vector of Meta-Model Parameters
	 * X --> Vector of Parameters to be Calibrated
	 * A --> Analytical Model Output 
	 */
	/**
	 * constructor
	 * 
	 */
	private final boolean addRidgePenalty=true;
	private final double ridgeCoefficient=1.0;
	private ArrayList<Double> analyticalData=new ArrayList<>();
	
	public AnalyticLinearMetaModel(HashMap<Integer,HashMap<String,Double>> SimData, HashMap<Integer,HashMap<String,Double>> AnalyticalData,
			HashMap<Integer, LinkedHashMap<String, Double>> paramsToCalibrate,String timeBeanId, int counter) {
		
		super(SimData,paramsToCalibrate,timeBeanId,counter);
		for(Entry<Integer,HashMap<String,Double>> e:AnalyticalData.entrySet()) {
			this.analyticalData.add(e.getValue().get(timeBeanId));
			
		}
		this.noOfMetaModelParams=this.noOfParams+2;
		this.calibrateMetaModel(counter);
		
		this.params.clear();
		this.simData.clear();
		this.analyticalData.clear();
		
	}
		
	
	public void calibrateMetaModel(final int counter) {
		Calcfc optimization=new Calcfc() {

			@Override
			public double compute(int m, int n, double[] x, double[] constrains) {
				double objective=0;
				MetaModelParams=x;
				for(int i:params.keySet()) {
					objective+=Math.pow(calcMetaModel(analyticalData.get(i), params.get(i))-simData.get(i),2)*calcEuclDistanceBasedWeight(params, i,counter);
				}
				if(addRidgePenalty==true) {
					for(double d:x) {
						objective+=d*d*ridgeCoefficient;
					}
				}
				return objective;
			}
			
		};
		double[] x=new double[this.noOfMetaModelParams]; 
		for(int i=0;i<this.noOfMetaModelParams;i++) {
			x[i]=0;
		}
	    CobylaExitStatus result = Cobyla.findMinimum(optimization, this.noOfMetaModelParams, 0, x,0.5,Math.pow(10, -6) ,0, 1500);
	    this.MetaModelParams=x;
		
	}

	@Override
	public double calcMetaModel(double analyticalModelPart, LinkedHashMap<String, Double> param) {
		double modelOutput=this.MetaModelParams[0]+MetaModelParams[1]*analyticalModelPart;
		int i=1;
		for(double d:param.values()) {
			modelOutput+=this.MetaModelParams[i+1]*d;
			i++;
		}
		
		return modelOutput;
		
	}


	@Override
	public String getMetaModelName() {
		return this.AnalyticalLinearMetaModelName;
	}
}
