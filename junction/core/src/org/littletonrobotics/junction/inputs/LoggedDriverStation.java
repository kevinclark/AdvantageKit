// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.littletonrobotics.junction.inputs;

import org.littletonrobotics.conduit.ConduitApi;
import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.Logger;

/**
 * Manages logging and replaying data from the driver station (robot state,
 * joysticks, etc.)
 */
public class LoggedDriverStation {

  private static LoggedDriverStation instance;
  private static final Logger logger = Logger.getInstance();
  private static final ConduitApi conduit = ConduitApi.getInstance();

  private final DriverStationInputs dsInputs = new DriverStationInputs();
  private final JoystickInputs[] joystickInputs = { new JoystickInputs(), new JoystickInputs(), new JoystickInputs(),
      new JoystickInputs(), new JoystickInputs(), new JoystickInputs() };

  private LoggedDriverStation() {
  }

  public static LoggedDriverStation getInstance() {
    if (instance == null) {
      instance = new LoggedDriverStation();
    }
    return instance;
  }

  /**
   * General driver station data that needs to be updated throughout the match.
   */
  private static class DriverStationInputs implements LoggableInputs {
    public int allianceStation = 0;
    public String eventName = "";
    public String gameSpecificMessage = "";
    public int matchNumber = 0;
    public int replayNumber = 0;
    public int matchType = 0;
    public double matchTime = 0.00;

    public boolean enabled = false;
    public boolean autonomous = false;
    public boolean test = false;
    public boolean emergencyStop = false;
    public boolean fmsAttached = false;
    public boolean dsAttached = false;

    public void toLog(LogTable table) {
      table.put("AllianceStation", allianceStation);
      table.put("EventName", eventName);
      table.put("GameSpecificMessage", gameSpecificMessage);
      table.put("MatchNumber", matchNumber);
      table.put("ReplayNumber", replayNumber);
      table.put("MatchType", matchType);
      table.put("MatchTime", matchTime);

      table.put("Enabled", enabled);
      table.put("Autonomous", autonomous);
      table.put("Test", test);
      table.put("EmergencyStop", emergencyStop);
      table.put("FMSAttached", fmsAttached);
      table.put("DSAttached", dsAttached);
    }

    public void fromLog(LogTable table) {
      allianceStation = table.getInteger("AllianceStation", allianceStation);
      eventName = table.getString("EventName", eventName);
      gameSpecificMessage = table.getString("GameSpecificMessage", gameSpecificMessage);
      matchNumber = table.getInteger("MatchNumber", matchNumber);
      replayNumber = table.getInteger("ReplayNumber", replayNumber);
      matchType = table.getInteger("MatchType", matchType);
      matchTime = table.getDouble("MatchTime", matchTime);

      enabled = table.getBoolean("Enabled", enabled);
      autonomous = table.getBoolean("Autonomous", autonomous);
      test = table.getBoolean("Test", test);
      emergencyStop = table.getBoolean("EmergencyStop", emergencyStop);
      fmsAttached = table.getBoolean("FMSAttached", fmsAttached);
      dsAttached = table.getBoolean("DSAttached", dsAttached);
    }
  }

  /**
   * All of the required inputs for a single joystick.
   */
  private static class JoystickInputs implements LoggableInputs {
    public String name = "";
    public int type = 0;
    public boolean xbox = false;
    public boolean[] buttons = {};
    public double[] axisValues = {};
    public int[] axisTypes = {};
    public int[] povs = {};

    public void toLog(LogTable table) {
      table.put("Name", name);
      table.put("Type", type);
      table.put("Xbox", xbox);
      table.put("Buttons", buttons);
      table.put("AxisValues", axisValues);
      table.put("AxisTypes", axisTypes);
      table.put("POVs", povs);
    }

    public void fromLog(LogTable table) {
      name = table.getString("Name", name);
      type = table.getInteger("Type", type);
      xbox = table.getBoolean("Xbox", xbox);
      buttons = table.getBooleanArray("Buttons", buttons);
      axisValues = table.getDoubleArray("AxisValues", axisValues);
      axisTypes = table.getIntegerArray("AxisTypes", axisTypes);
      povs = table.getIntegerArray("POVs", povs);
    }
  }

  /**
   * Records inputs from the real driver station via conduit
   */
  public void periodic() {
    // Update inputs from conduit
    if (!logger.hasReplaySource()) {
      dsInputs.allianceStation = conduit.getAllianceStation();
      dsInputs.eventName = conduit.getEventName();
      dsInputs.gameSpecificMessage = conduit.getGameSpecificMessage();
      dsInputs.matchNumber = conduit.getMatchNumber();
      dsInputs.replayNumber = conduit.getReplayNumber();
      dsInputs.matchType = conduit.getMatchType();
      dsInputs.matchTime = conduit.getMatchTime();

      int controlWord = conduit.getControlWord();
      dsInputs.enabled = (controlWord & 1) != 0;
      dsInputs.autonomous = (controlWord & 2) != 0;
      dsInputs.test = (controlWord & 4) != 0;
      dsInputs.emergencyStop = (controlWord & 8) != 0;
      dsInputs.fmsAttached = (controlWord & 16) != 0;
      dsInputs.dsAttached = (controlWord & 32) != 0;

      for (int id = 0; id < joystickInputs.length; id++) {
        JoystickInputs joystick = joystickInputs[id];
        joystick.name = conduit.getJoystickName(id);
        joystick.type = conduit.getJoystickType(id);
        joystick.xbox = conduit.isXbox(id);
        joystick.axisTypes = conduit.getAxisTypes(id);
        joystick.povs = conduit.getPovValues(id);

        float[] axisValues = conduit.getAxisValues(id);
        joystick.axisValues = new double[axisValues.length];
        for (int i = 0; i < axisValues.length; i++) {
          joystick.axisValues[i] = axisValues[i];
        }

        int buttons = conduit.getButtonValues(id);
        int buttonCount = conduit.getButtonCount(id);
        joystick.buttons = new boolean[buttonCount];
        for (int i = 0; i < buttonCount; i++) {
          joystick.buttons[i] = ((buttons >> i) & 1) != 0;
        }
      }
    }

    // Send/receive log data
    logger.processInputs("DriverStation", dsInputs);
    for (int id = 0; id < joystickInputs.length; id++) {
      logger.processInputs("DriverStation/Joystick" + Integer.toString(id), joystickInputs[id]);
    }
  }
}
