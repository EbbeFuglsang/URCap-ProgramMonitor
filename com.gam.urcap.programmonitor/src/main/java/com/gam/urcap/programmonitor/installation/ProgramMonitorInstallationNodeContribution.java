package com.gam.urcap.programmonitor.installation;

import java.awt.EventQueue;
import java.util.Timer;
import java.util.TimerTask;

import com.gam.urcap.programmonitor.daemon.DaemonXmlRpcCommunicator;
import com.gam.urcap.programmonitor.daemon.DaemonXmlRpcCommunicator.RESULT_TYPE;
import com.gam.urcap.programmonitor.daemon.ProgramMonitorDaemonService;
import com.gam.urcap.programmonitor.monitoring.ResultSet;
import com.ur.urcap.api.contribution.DaemonContribution.State;
import com.ur.urcap.api.contribution.InstallationNodeContribution;
import com.ur.urcap.api.contribution.installation.InstallationAPIProvider;
import com.ur.urcap.api.domain.feature.*;
import com.ur.urcap.api.domain.script.ScriptWriter;
import com.ur.urcap.api.domain.value.PoseFactory;
import com.ur.urcap.api.domain.value.simple.Length.Unit;
import com.ur.urcap.api.domain.value.simple.*;

public class ProgramMonitorInstallationNodeContribution implements InstallationNodeContribution{

	private final InstallationAPIProvider apiProvider;	// Unused
	private final ProgramMonitorInstallationNodeView view;
	private final ProgramMonitorDaemonService programMonitorDaemonService;
	private final DaemonXmlRpcCommunicator monitorCommunicator = new DaemonXmlRpcCommunicator();
	private final FeatureModel featureModel; 
	private final FeatureContributionModel featureContributionModel;
	private final PoseFactory poseFactory;
	private boolean featuresGreated = false; 
	
	private static final String XMLRPC_HANDLE = "PROGRAM_MONITOR_DAEMON";
	
	private Timer updateTimer;
	
	private ResultSet RESULTS = new ResultSet();
	
	/*****
	 * BEWARE! 
	 * This is an instance variable, and is not the typical approach for most URCaps. 
	 * This is expressly used, since the Program Monitor should only be active for testing purposes. 
	 * This setting is not persisted through reboot, reloading installations etc. 
	 * For a more typical approach, where the value is saved, use the DataModel.
	 * 
	 * This variable indicates if Program Monitoring is set active for the time being. 
	 */
	private boolean ENABLE_PROGRAM_MONITORING = false;
	
	public ProgramMonitorInstallationNodeContribution(InstallationAPIProvider apiProvider,
			ProgramMonitorInstallationNodeView view, 
			ProgramMonitorDaemonService programMonitorDaemon) {
		this.apiProvider = apiProvider;
		this.view = view;
		this.programMonitorDaemonService = programMonitorDaemon;
		this.featureModel =apiProvider.getInstallationAPI().getFeatureModel();
		this.featureContributionModel = apiProvider.getInstallationAPI().getFeatureContributionModel();
		this.poseFactory = apiProvider.getInstallationAPI().getValueFactoryProvider().getPoseFactory();
	}
	
	public void enableMonitoringChanged(boolean enabled) {
		ENABLE_PROGRAM_MONITORING = enabled;
		view.setMonitoringEnabledState(ENABLE_PROGRAM_MONITORING);
		if(ENABLE_PROGRAM_MONITORING) {
			startTimer();
		}
		applyDaemonState(enabled);
	}
	
	public void updatePlanes() {
		//FeatureContributionModel featureContributionModel = 
		double xmin = monitorCommunicator.getResult(RESULT_TYPE.X_MIN);
		double xmax = monitorCommunicator.getResult(RESULT_TYPE.X_MAX);
		double ymin = monitorCommunicator.getResult(RESULT_TYPE.Y_MIN);
		double ymax = monitorCommunicator.getResult(RESULT_TYPE.Y_MAX);
		double zmin = monitorCommunicator.getResult(RESULT_TYPE.Z_MIN);
		double zmax = monitorCommunicator.getResult(RESULT_TYPE.Z_MAX);
		
		

		if(this.featuresGreated == false) {
			this.featureContributionModel.addFeature("safePlane1xMin", "X_min_plane", poseFactory.createPose(xmin, ymin, zmin, 0, Math.PI/2, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.addFeature("safePlane1yMin", "Y_min_plane", poseFactory.createPose(xmin, ymin, zmin, -Math.PI/2, 0, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.addFeature("safePlane1zMin", "Z_min_plane", poseFactory.createPose(xmin, ymin, zmin, 0, 0, 0, Length.Unit.M, Angle.Unit.RAD));
			
			this.featureContributionModel.addFeature("safePlane1xMax", "X_max_plane", poseFactory.createPose(xmax, ymax, zmax, 0, -Math.PI/2, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.addFeature("safePlane1yMax", "Y_max_plane", poseFactory.createPose(xmin, ymin, zmax, Math.PI/2, 0, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.addFeature("safePlane1zMax", "Z_max_plane", poseFactory.createPose(xmin, ymax, zmax, Math.PI, 0, 0, Length.Unit.M, Angle.Unit.RAD));			
			this.featuresGreated = true;
		}else {
			this.featureContributionModel.updateFeature("safePlane1xMin", poseFactory.createPose(xmin, ymin, zmin, 0, Math.PI/2, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.updateFeature("safePlane1yMin", poseFactory.createPose(xmin, ymin, zmin, -Math.PI/2, 0, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.updateFeature("safePlane1zMin", poseFactory.createPose(xmin, ymin, zmin, 0, 0, 0, Length.Unit.M, Angle.Unit.RAD));
			
			this.featureContributionModel.updateFeature("safePlane1xMax", poseFactory.createPose(xmax, ymax, zmax, 0, -Math.PI/2, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.updateFeature("safePlane1yMax", poseFactory.createPose(xmin, ymin, zmax, Math.PI/2, 0, 0, Length.Unit.M, Angle.Unit.RAD));
			this.featureContributionModel.updateFeature("safePlane1zMax", poseFactory.createPose(xmin, ymax, zmax, Math.PI, 0, 0, Length.Unit.M, Angle.Unit.RAD));			
		}
			
	}
	
	
	public void userSwitchedToResultsTab() {
		checkForAndDisplayResults();
	}
	
	@Override
	public void openView() {
		view.setMonitoringEnabledState(ENABLE_PROGRAM_MONITORING);
		view.setDaemonStatusLabel(getDaemonStatusLabel());
		if(ENABLE_PROGRAM_MONITORING) {
			startTimer();
		}
		checkForAndDisplayResults();
	}

	@Override
	public void closeView() {
		stopTimer();
	}

	@Override
	public void generateScript(ScriptWriter writer) {
		if(ENABLE_PROGRAM_MONITORING) {
			// Only write any script, if the enable-flag is set. 
			
			String THREAD_NAME = "programMonitoringThread";
			String THREAD_HANDLE = "programMonitorThreadHandle";
			
			// Make an informational popup to the user
			writer.appendLine("popup(\"Program Monitor is active, and will monitor the execution of this program\",\"Program Monitor\",blocking=True)");
			
			// Create an XML-RPC handle
			writer.assign(XMLRPC_HANDLE, "rpc_factory(\"xmlrpc\",\"http://127.0.0.1:23444\")");
			
			String GLOBALVAR_MIN_X = "progMonitor_minX";
			String GLOBALVAR_MAX_X = "progMonitor_maxX";
			String GLOBALVAR_MIN_Y = "progMonitor_minY";
			String GLOBALVAR_MAX_Y = "progMonitor_maxY";
			String GLOBALVAR_MIN_Z = "progMonitor_minZ";
			String GLOBALVAR_MAX_Z = "progMonitor_maxZ";
			String GLOBALVAR_MAX_SPEED = "progMonitor_maxSpeed";
			
			String LOCALVAR_TCP_POSE = "nowTCP";
			String LOCALVAR_TCP_SPEED_6D = "nowSpeed6D";
			String LOCALVAR_TCP_SPEED = "nowSpeed";
			
			String LOCAL_MIN_DEFAULT = "10000";
			String LOCAL_MAX_DEFAULT = "-10000";
			String LOCAL_SPEED_DEFAULT = "0";
			
			// Define variables for local min/max
			writer.assign(GLOBALVAR_MIN_X, LOCAL_MIN_DEFAULT);
			writer.assign(GLOBALVAR_MAX_X, LOCAL_MAX_DEFAULT);
			writer.assign(GLOBALVAR_MIN_Y, LOCAL_MIN_DEFAULT);
			writer.assign(GLOBALVAR_MAX_Y, LOCAL_MAX_DEFAULT);
			writer.assign(GLOBALVAR_MIN_Z, LOCAL_MIN_DEFAULT);
			writer.assign(GLOBALVAR_MAX_Z, LOCAL_MAX_DEFAULT);
			writer.assign(GLOBALVAR_MAX_SPEED, LOCAL_SPEED_DEFAULT);
			
			writer.appendLine(XMLRPC_HANDLE + ".resetResults()");
			
			writer.defineThread(THREAD_NAME);
			writer.whileTrue();
			
				writer.assign("local "+LOCALVAR_TCP_POSE, "get_actual_tcp_pose()");
				writer.assign("local "+LOCALVAR_TCP_SPEED_6D, "get_actual_tcp_speed()");
				writer.assign("local "+LOCALVAR_TCP_SPEED, "norm("+LOCALVAR_TCP_SPEED_6D+")");
				
				writer.ifCondition(LOCALVAR_TCP_POSE + "[0] < " + GLOBALVAR_MIN_X);
					writer.assign(GLOBALVAR_MIN_X, LOCALVAR_TCP_POSE + "[0]");
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"xmin\"," + GLOBALVAR_MIN_X + ")");
				writer.end();  // End of if	
				
				writer.ifCondition(LOCALVAR_TCP_POSE + "[0] > " + GLOBALVAR_MAX_X);
					writer.assign(GLOBALVAR_MAX_X, LOCALVAR_TCP_POSE + "[0]");
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"xmax\"," + GLOBALVAR_MAX_X + ")");
				writer.end();  // End of if	
				
				writer.ifCondition(LOCALVAR_TCP_POSE + "[1] < " + GLOBALVAR_MIN_Y);
					writer.assign(GLOBALVAR_MIN_Y, LOCALVAR_TCP_POSE + "[1]");
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"ymin\"," + GLOBALVAR_MIN_Y + ")");
				writer.end();  // End of if	
				
				writer.ifCondition(LOCALVAR_TCP_POSE + "[1] > " + GLOBALVAR_MAX_Y);
					writer.assign(GLOBALVAR_MAX_Y, LOCALVAR_TCP_POSE + "[1]");
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"ymax\"," + GLOBALVAR_MAX_Y + ")");
				writer.end();  // End of if	
				
				writer.ifCondition(LOCALVAR_TCP_POSE + "[2] < " + GLOBALVAR_MIN_Z);
					writer.assign(GLOBALVAR_MIN_Z, LOCALVAR_TCP_POSE + "[2]");
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"zmin\"," + GLOBALVAR_MIN_Z + ")");
				writer.end();  // End of if	
				
				writer.ifCondition(LOCALVAR_TCP_POSE + "[2] > " + GLOBALVAR_MAX_Z);
					writer.assign(GLOBALVAR_MAX_Z, LOCALVAR_TCP_POSE + "[2]");
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"zmax\"," + GLOBALVAR_MAX_Z + ")");
				writer.end();  // End of if	
				
				writer.ifCondition(LOCALVAR_TCP_SPEED + " > " + GLOBALVAR_MAX_SPEED);
					writer.assign(GLOBALVAR_MAX_SPEED, LOCALVAR_TCP_SPEED);
					writer.appendLine(XMLRPC_HANDLE + ".setResult(\"speedmax\"," + GLOBALVAR_MAX_SPEED + ")");
				writer.end();  // End of if	
				
			writer.sync(); 
			writer.end();	// End of while-true
			writer.end();	// End of thread definition
			
			writer.runThread(THREAD_HANDLE, THREAD_NAME+"()");
		}
	}
	
	private void startTimer() {
		updateTimer = new Timer(true);
		updateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						view.setDaemonStatusLabel(getDaemonStatusLabel());
					}
				});
			}
		}, 0, 500);
	}
	
	private void stopTimer() {
		if(updateTimer!=null) {
			updateTimer.cancel();
		}
	}
	
	private void applyDaemonState(boolean state) {
		if(state) {
			this.programMonitorDaemonService.getDaemonContribution().start();
		} else {
			if(resultsAvailable()) {
				RESULTS = getResults();
			}
			this.programMonitorDaemonService.getDaemonContribution().stop();
		}
	}
	
	private boolean isDaemonRunning() {
		return this.programMonitorDaemonService.getDaemonContribution().getState().equals(State.RUNNING);
	}
	
	private String getDaemonStatusLabel() {
		if(ENABLE_PROGRAM_MONITORING) {
			if(isDaemonRunning()) {
				return "Ready to monitor";
			} else {
				return "Initializing...";
			}
		} else {
			return "Monitoring not activated";
		}
	}
	
	private void checkForAndDisplayResults() {
		if(ENABLE_PROGRAM_MONITORING) {
			if(resultsAvailable()) {
				RESULTS = getResults();
				view.setResultSet(RESULTS);
			}
		}
	}
	
	private boolean resultsAvailable() {
		if(!isDaemonRunning()) {
			return false;
		}else {
			return monitorCommunicator.hasMonitored();
		}
		
	}
	
	private ResultSet getResults() {
		ResultSet results = new ResultSet();
		results.setX(monitorCommunicator.getResult(RESULT_TYPE.X_MIN), monitorCommunicator.getResult(RESULT_TYPE.X_MAX));
		results.setY(monitorCommunicator.getResult(RESULT_TYPE.Y_MIN), monitorCommunicator.getResult(RESULT_TYPE.Y_MAX));
		results.setZ(monitorCommunicator.getResult(RESULT_TYPE.Z_MIN), monitorCommunicator.getResult(RESULT_TYPE.Z_MAX));
		results.setSpeedMax(monitorCommunicator.getResult(RESULT_TYPE.SPEED_MAX));
		
		return results;
	}

}
