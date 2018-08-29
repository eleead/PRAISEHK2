package calibrator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import analyticalModel.AnalyticalModel;
import matamodels.AnalyticLinearMetaModel;
import matamodels.AnalyticalQuadraticMetaModel;
import matamodels.GradientBaseOptimizedMetaModel;
import matamodels.GradientBasedMetaModel;
import matamodels.GradientOptimizedMetaModel;
import matamodels.LinearMetaModel;
import matamodels.MetaModel;
import matamodels.QuadraticMetaModel;
import measurements.Measurement;
import measurements.Measurements;

public class CalibratorImpl implements Calibrator {

	//Necessary Containers

	private Map<Integer,Measurements> simMeasurements;
	private Map<Integer,Measurements> anaMeasurements;
	private Map<Id<Measurement>,Map<String,MetaModel>>metaModels;
	private Map<Integer,LinkedHashMap<String,Double>>params;

	private Measurements calibrationMeasurements;

	private int iterationNo=0;
	private int currentParamNo=0;

	private LinkedHashMap<String,Double> currentParam=new LinkedHashMap<>();
	private LinkedHashMap<String,Double> trialParam=new LinkedHashMap<>();



	private AnalyticalModel sueAssignment;

	private static final Logger logger=Logger.getLogger(CalibratorImpl.class);

	//Trust region parameters

	private int maxIteration=100;
	private double initialTrRadius=25;
	private double maxTrRadius=2.5*this.initialTrRadius;
	private double minTrRadius=0.001;
	private double maxSuccesiveRejection=4;
	private double minMetaParamChange=.001;
	private double thresholdErrorRatio=.01;
	private String metaModelType=MetaModel.AnalyticalLinearMetaModelName;
	private double trusRegionIncreamentRatio=1.25;
	private double trustRegionDecreamentRatio=0.9;


	public void resetIteration() {
		this.iterationNo=0;
	}


	public void updateAnalyticalModel(AnalyticalModel SUE) {
		this.sueAssignment=SUE;
	}

	@Override
	public void updateSimMeasurements(Measurements m) {
		this.simMeasurements.put(this.iterationNo, m);
	}

	@Override
	public void updateAnalyticalMeasurement(Map<Integer, Measurements> measurements) {
		if(this.anaMeasurements.size()!=measurements.size()) {
			logger.error("Measurements size must match. Aborting update");
			for(int i:this.anaMeasurements.keySet()) {
				if(measurements.get(i)==null) {
					logger.error("Measurements do not match. This is a fatal error!!!");
					throw new IllegalArgumentException("Measurements do not match. This is a fatal error!!!Calibration will exit.");
				}
				this.anaMeasurements.put(i, measurements.get(i));
			}
		}
	}

	
	/**
	 * The input gradients can be null for non gradient based meatamodels.
	 * The gradient must contain the same measurement IDs and volumes as the calibration measurements
	 * @param simGradient
	 * @param anaGradient
	 * @throws IllegalArgumentException
	 */
	@Override
	public void createMetaModel(Map<Id<Measurement>,Map<String,LinkedHashMap<String,Double>>>simGradient,Map<Id<Measurement>,Map<String,LinkedHashMap<String,Double>>> anaGradient, String metaModelType) {
		try {
		if((this.metaModelType.equals(MetaModel.GradientBased_I_MetaModelName)||this.metaModelType.equals(MetaModel.GradientBased_II_MetaModelName)||this.metaModelType.equals(MetaModel.GradientBased_III_MetaModelName))&& (anaGradient==null||simGradient==null)) {
			logger.error("Cannot create gradient based meta-model without gradient. switching to AnalyticalLinear");
			throw new IllegalArgumentException("Gradient cannot be null");

		}
		}catch(Exception e) {
			metaModelType=MetaModel.AnalyticalLinearMetaModelName;
		}

		for(Measurement m:this.calibrationMeasurements.getMeasurements().values()) {
			this.metaModels.put(m.getId(), new HashMap<String,MetaModel>());
			for(String timeBeanId:m.getVolumes().keySet()) {

				MetaModel metaModel;

				switch(metaModelType) {

				case MetaModel.AnalyticalLinearMetaModelName: metaModel=new AnalyticLinearMetaModel(m.getId(), this.simMeasurements, this.anaMeasurements, this.params, timeBeanId, this.currentParamNo) ;

				case MetaModel.AnalyticalQuadraticMetaModelName: metaModel=new AnalyticalQuadraticMetaModel(m.getId(), this.simMeasurements, this.anaMeasurements, this.params, timeBeanId, this.currentParamNo);

				case MetaModel.LinearMetaModelName: metaModel=new LinearMetaModel(m.getId(), this.simMeasurements, this.params, timeBeanId, this.currentParamNo);

				case MetaModel.QudaraticMetaModelName: metaModel=new QuadraticMetaModel(m.getId(), this.simMeasurements, this.params, timeBeanId, this.currentParamNo) ;

				case MetaModel.GradientBased_I_MetaModelName: metaModel=new GradientBasedMetaModel(m.getId(), this.simMeasurements, this.anaMeasurements, this.params, timeBeanId, this.currentParamNo,simGradient.get(m.getId()).get(timeBeanId), anaGradient.get(m.getId()).get(timeBeanId));

				case MetaModel.GradientBased_II_MetaModelName: metaModel=new GradientBaseOptimizedMetaModel(m.getId(), this.simMeasurements, this.anaMeasurements, this.params, timeBeanId, this.currentParamNo, simGradient.get(m.getId()).get(timeBeanId), anaGradient.get(m.getId()).get(timeBeanId), this.iterationNo);

				case MetaModel.GradientBased_III_MetaModelName: metaModel=new GradientOptimizedMetaModel(m.getId(), this.simMeasurements, this.anaMeasurements, this.params, timeBeanId, this.currentParamNo, simGradient.get(m.getId()).get(timeBeanId), anaGradient.get(m.getId()).get(timeBeanId), this.iterationNo);

				default : metaModel=new AnalyticLinearMetaModel(m.getId(), this.simMeasurements, this.anaMeasurements, this.params, timeBeanId, this.currentParamNo) ;

				
				}
				this.metaModels.get(m.getId()).put(timeBeanId, metaModel);
			}
		}
	}


	

	@Override
	public LinkedHashMap<String, Double> drawRandomPoint(LinkedHashMap<String, Tuple<Double, Double>> paramLimit) {
		LinkedHashMap<String, Double> randPoint=new LinkedHashMap<>();
		for(String s: paramLimit.keySet()) {
			double l=paramLimit.get(s).getFirst();
			double u=paramLimit.get(s).getSecond();
			randPoint.put(s,l+Math.random()*(u-l));
		}
		
		return randPoint;
	}


	@Override
	public void writeMeasurementComparison(String fileLoc) {
		try {
			FileWriter fw=new FileWriter(new File(fileLoc),false);
			fw.append("MeasurementId,timeBeanId,RealCount,SimCount,metaModelPrediction,AbsPercentDifference,PercentDifference\n");
			for(Measurement m: this.calibrationMeasurements.getMeasurements().values()) {
				for(String timeBean:m.getVolumes().keySet()) {
					fw.append(m.getId()+","+timeBean+","+this.calibrationMeasurements.getMeasurements().get(m.getId()).getVolumes().get(timeBean)+","+this.simMeasurements.get(this.iterationNo).getMeasurements())
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void CalcMetaModelPrediction(Id<Measurement>mId,String timeId) {
		
	}

}
