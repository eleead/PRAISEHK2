package calibrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.utils.collections.Tuple;

import analyticalModel.AnalyticalModel;
/**
 * This class reads the default parameter values,limit, sub-population and any other code specified for the parameters 
 * 
 * The file should be in .csv
 * It should contain a header.
 * 
 * The columns should be 
 * SubPopulation,ParameterName,id,LowerLimit,UpperLimit,CurretValue,Code,IncludeIninitialParam
 * 
 * The id will be generated in the constructor as SubPopulationName+space+ParameterName
 * 
 * The code will be used to identify the parameters. This code is provided to facilitate same parameters for different subPopulation
 * same code parameters will be treated as one parameter. 
 * 
 * Parameter Names should include all the parameter names mentioned in AnalyticalModel Interface for all subPopulations
 * subPopulation names containing GV may exclude the parameters related to PT 
 * 
 * If There is no sub-population, then sub-population field should contain All. 
 * 
 * @author Ashraf
 *
 */
public class ParamReader {
/**
 * This file reads params and create the ParameterLimits
 */
	private final File paramFile;
	private final String defaultFileLoc="src/main/resources/paramReaderTrial1.csv";
	private ArrayList<String> subPopulationName=new ArrayList<>();
	private LinkedHashMap<String,Double>DefaultParam=new LinkedHashMap<>();
	private LinkedHashMap<String,Tuple<Double,Double>>paramLimit=new LinkedHashMap<>();
	private ArrayList<String>paramName=new ArrayList<>();
	private LinkedHashMap<String,Double> defaultParamPaper=new LinkedHashMap<>();
	private LinkedHashMap<String,String> ParamNoCode=new LinkedHashMap<>();
	private LinkedHashMap<String,Double>initialParam=new LinkedHashMap<>();
	private LinkedHashMap<String,Tuple<Double,Double>>initialParamLimit=new LinkedHashMap<>();
	
	
	
	public ParamReader(String fileLoc) {
		File file=new File(fileLoc);
		if(file.exists()) {
			this.paramFile=file;
		}else {
			this.paramFile=new File(defaultFileLoc);
		}
		try {
			BufferedReader bf=new BufferedReader(new FileReader(this.paramFile));
			bf.readLine();//getReadof the header
			String line;
			while((line=bf.readLine())!=null) {
				String[] part=line.split(",");
				String subPopName=part[0];
				String paramName=part[1];
				Double paramValue=Double.parseDouble(part[5]);
				Double upperLimit=Double.parseDouble(part[4]);
				Double lowerLimit=Double.parseDouble(part[3]);
				String paramId=part[2];
				if(subPopName=="") {
					paramId=part[1];
				}else {
					paramId=part[0]+" "+part[1];
				}
				this.DefaultParam.put(part[6], paramValue);
				this.paramLimit.put(part[6], new Tuple<Double,Double>(lowerLimit,upperLimit));
				if(!this.subPopulationName.contains(subPopName) && !subPopName.equals("All") && !subPopName.equals("")) {
					this.subPopulationName.add(subPopName);
				}
				this.paramName.add(paramName);
				if(part[6]!=null) {
					this.ParamNoCode.put(paramId, part[6]);
				}
				if(Boolean.parseBoolean(part[7])==true) {
					this.initialParam.put(part[6], paramValue);
					this.initialParamLimit.put(part[6], this.paramLimit.get(part[6]));
				}
			}
			bf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public LinkedHashMap<String, Double> getInitialParam() {
		return initialParam;
	}


	public LinkedHashMap<String, Tuple<Double, Double>> getInitialParamLimit() {
		return initialParamLimit;
	}


	public LinkedHashMap<String, Double> getDefaultParamPaper() {
		return defaultParamPaper;
	}


	public void setDefaultParamPaper(LinkedHashMap<String, Double> defaultParamPaper) {
		this.defaultParamPaper = defaultParamPaper;
	}


	public String getDefaultFileLoc() {
		return defaultFileLoc;
	}


	public ArrayList<String> getSubPopulationName() {
		return subPopulationName;
	}


	public LinkedHashMap<String, Double> getDefaultParam() {
		return DefaultParam;
	}


	public LinkedHashMap<String, Tuple<Double, Double>> getParamLimit() {
		return paramLimit;
	}


	public ArrayList<String> getParamName() {
		return paramName;
	}
	
	public HashMap<String, Tuple<Double,Double>> getDefaultTimeBean() {
		HashMap<String, Tuple<Double,Double>> timeBean=new HashMap<>();
		timeBean.put("BeforeMorningPeak", new Tuple<Double,Double>(0.0,25200.));
		timeBean.put("MorningPeak", new Tuple<Double,Double>(25200.,36000.));
		timeBean.put("AfterMorningPeak", new Tuple<Double,Double>(36000.,57600.));
		timeBean.put("EveningPeak", new Tuple<Double,Double>(57600.,72000.));
		timeBean.put("AfterEveningPeak", new Tuple<Double,Double>(72000.,86400.));
		return timeBean;
	}
	
	
	
	public LinkedHashMap<String,Double> ScaleUp(LinkedHashMap<String,Double>param){
		LinkedHashMap<String,Double> scaledParam=new LinkedHashMap<String,Double>();
		for(Entry<String, String> e:this.ParamNoCode.entrySet()) {
			if(param.get(e.getValue())==null) {
				scaledParam.put(e.getKey(),this.DefaultParam.get(e.getValue()));
			}else {
				scaledParam.put(e.getKey(), param.get(e.getValue()));
			}
		}
		return scaledParam;
	}
	
	
	
	
	
	
	
	
	public Config SetParamToConfig(Config config, LinkedHashMap<String, Double> Noparams) {
		
		new ConfigWriter(config).write("config_Intermediate.xml");
		Config configOut=ConfigUtils.loadConfig("config_Intermediate.xml");
		for(String s:this.DefaultParam.keySet()) {
			if(Noparams.get(s)==null) {
				Noparams.put(s,this.DefaultParam.get(s));
			}
		}
		LinkedHashMap<String,Double> params=this.ScaleUp(Noparams);
		
		if(this.subPopulationName.size()!=0) {
		for(String subPop:this.subPopulationName) {
			if(!subPop.contains("GV")) {
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofTravelCarName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofDistanceCarName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).setMarginalUtilityOfMoney(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofMoneyName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostCarName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofTravelptName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.MarginalUtilityOfDistancePtName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).setMarginalUtlOfWaitingPt_utils_hr(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofWaitingName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).setUtilityOfLineSwitch(params.get(subPop+" "+AnalyticalModel.UtilityOfLineSwitchName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityOfWalkingName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostWalkName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("pt").setConstant(params.get(subPop+" "+AnalyticalModel.ModeConstantPtname));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setConstant(params.get(subPop+" "+AnalyticalModel.ModeConstantCarName));
			configOut.planCalcScore().getOrCreateScoringParameters(subPop).setPerforming_utils_hr(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofPerformName));
			
			}else {
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofTravelCarName));
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofDistanceCarName));
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).setMarginalUtilityOfMoney(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofMoneyName));
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostCarName));
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(subPop+" "+AnalyticalModel.MarginalUtilityOfWalkingName));
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(subPop+" "+AnalyticalModel.DistanceBasedMoneyCostWalkName));
				configOut.planCalcScore().getOrCreateScoringParameters(subPop).setPerforming_utils_hr(params.get(subPop+" "+AnalyticalModel.MarginalUtilityofPerformName));
			}
		}
		}else {
			configOut.planCalcScore().getOrCreateModeParams("car").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityofTravelCarName));
			configOut.planCalcScore().getOrCreateModeParams("car").setMarginalUtilityOfDistance(params.get(AnalyticalModel.MarginalUtilityofDistanceCarName));
			configOut.planCalcScore().setMarginalUtilityOfMoney(params.get(AnalyticalModel.MarginalUtilityofMoneyName));
			configOut.planCalcScore().getOrCreateModeParams("car").setMonetaryDistanceRate(params.get(AnalyticalModel.DistanceBasedMoneyCostCarName));
			configOut.planCalcScore().getOrCreateModeParams("pt").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityofTravelptName));
			configOut.planCalcScore().getOrCreateModeParams("pt").setMonetaryDistanceRate(params.get(AnalyticalModel.MarginalUtilityOfDistancePtName));
			configOut.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(params.get(AnalyticalModel.MarginalUtilityofWaitingName));
			configOut.planCalcScore().setUtilityOfLineSwitch(params.get(AnalyticalModel.UtilityOfLineSwitchName));
			configOut.planCalcScore().getOrCreateModeParams("walk").setMarginalUtilityOfTraveling(params.get(AnalyticalModel.MarginalUtilityOfWalkingName));
			configOut.planCalcScore().getOrCreateModeParams("walk").setMonetaryDistanceRate(params.get(AnalyticalModel.DistanceBasedMoneyCostWalkName));
			configOut.planCalcScore().getOrCreateModeParams("pt").setConstant(params.get(AnalyticalModel.ModeConstantPtname));
			configOut.planCalcScore().getOrCreateModeParams("car").setConstant(params.get(AnalyticalModel.ModeConstantCarName));
			configOut.planCalcScore().setPerforming_utils_hr(params.get(AnalyticalModel.MarginalUtilityofPerformName));
		}
		configOut.qsim().setFlowCapFactor(params.get(AnalyticalModel.CapacityMultiplierName));
		return configOut;
	}
	
	public static void main(String[] args) {
		ParamReader pReader=new ParamReader("src/main/resources/paramReaderTrial1.csv");
	}
	
	
}
