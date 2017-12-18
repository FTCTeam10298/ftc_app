/* Copyright (c) 2017 Brain Stormz, FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of Brain Stormz, nor FIRST, nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;

import ftclib.*;
import hallib.*;

@Autonomous(name="OurBot: Autonomous", group ="OurBot")
public class OurBot_Autonomous extends LinearOpMode implements FtcMenu.MenuButtons {
    public enum Alliance {
        ALLIANCE_RED,
        ALLIANCE_BLUE
    }
    public enum StartPosition {
        STARTPOSITION1,
        STARTPOSITION2
    }
    public enum RunMode {
        RUNMODE_AUTO,
        RUNMODE_DEBUG
    }
    public enum Column {
        COLUMN_LEFT,
        COLUMN_CENTER,
        COLUMN_RIGHT
    }

    // Menu option variables
    RunMode        runmode       = RunMode.RUNMODE_AUTO;
    Alliance       alliance      = Alliance.ALLIANCE_RED;
    int            delay         = 0;
    StartPosition  startposition = StartPosition.STARTPOSITION1;

    /* Declare OpMode members. */
    private HalDashboard dashboard;
    OurBot_Hardware   robot         = new OurBot_Hardware();
    ColorSensor       color_sensor;

    static final double     COUNTS_PER_MOTOR_REV      = 1120;    // Rev HD Hex v2 Motor Encoder
    static final double     DRIVE_GEAR_REDUCTION      = 1.25;     // This is < 1.0 if geared UP
    static final double     WHEEL_DIAMETER_INCHES     = 4.0;     // For figuring circumference
    static final double     COUNTS_PER_INCH           = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * 3.1416);

    Column column = Column.COLUMN_CENTER;

    /**
     * Define the variable we will use to store our instance of the Vuforia
     * localization engine.
     */
    VuforiaLocalizer vuforia;

    /**
     * Color sensor declaration
     */

    @Override public void runOpMode() {
        // Initialize the hardware -----------------------------------------------------------------
        robot.init(hardwareMap);
        color_sensor = hardwareMap.colorSensor.get("jewel");

        // Initialize dashboard --------------------------------------------------------------------
        dashboard = HalDashboard.createInstance(telemetry);

        // Initialize Vuforia ----------------------------------------------------------------------
        /*
         * To start up Vuforia, tell it the view that we wish to use for camera monitor
         * (on the RC phone); If no camera monitor is desired, use the parameterless
         * constructor instead (commented out below).
         */
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId",
                                                    "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        // Or, don't activate the Camera Monitor view, to save power
        //VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        // Our key
        parameters.vuforiaLicenseKey = "AfXF2Nr/////AAAAGdMsV8BsHUFiomBNGmOw3NEPpvX+GToNkTKFRAohdsZ57x+R4vVXCAhd45zcQxYy3psYjXYJaAQtxU9PkBtn2l3T8BTkpEq46FZGRakXKnidi6F3kOYYco4wemaLAii9TcfZ3T7U7BJ3fXs+wRecumf1+e406AOvdiv5EU6PhWsPqjWu25y2Ta4D13WjxTgW6klkpgsTMeeA/4bu1DhcUPkhkA/c+MIf1qFXzidUyuI0DVHH5eO9KxjFzGzAB1e/oQV6ePyPbpnU55xhVtY8vG0ruO8skOweWBP4W+ABjLBrDR+XXeMirX+MsnbbreP4ZDE0NUokTOMqEGS5o0JL/E7FrcVDIFMrr41hlFg8WUR/";

        /*
         * We also indicate which camera on the RC that we wish to use.
         * Here we chose the back (HiRes) camera (for greater range), but
         * for a competition robot, the front camera might be more convenient.
         */
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        this.vuforia = ClassFactory.createVuforiaLocalizer(parameters);

        /*
         * Load the data set containing the VuMarks for Relic Recovery. There's only one trackable
         * in this data set: all three of the VuMarks in the game were created from this one
         * template, but differ in their instance id information.
         * @see VuMarkInstanceId
         */
        VuforiaTrackables relicTrackables = this.vuforia.loadTrackablesFromAsset("RelicVuMark");
        VuforiaTrackable relicTemplate = relicTrackables.get(0);
        relicTemplate.setName("relicVuMarkTemplate"); // can help in debugging; otherwise not necessary

        relicTrackables.activate();

        // Run though the menu ---------------------------------------------------------------------
        doMenus();
        dashboard.displayPrintf(0, "Status: Ready to start");

        robot.jewelArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.jewelArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        robot.jewelArm.setTargetPosition(0);
        robot.jewelArm.setPower(0.5); // was 0.3

        // Wait for the game to start (driver presses PLAY)
        waitForStart();

        /*
         * ----- AUTONOMOUS START-------------------------------------------------------------------
         */

        // Pause the program for the selected delay period
        sleep(delay);

        if (DoTask("Read VuMark", runmode)) {
            RelicRecoveryVuMark vuMark = RelicRecoveryVuMark.from(relicTemplate);
            if (vuMark == RelicRecoveryVuMark.LEFT) {
                column = Column.COLUMN_LEFT;
                dashboard.displayPrintf(6, "VuMark: LEFT visible");
            } else if (vuMark == RelicRecoveryVuMark.CENTER) {
                column = Column.COLUMN_CENTER;
                dashboard.displayPrintf(6, "VuMark: CENTER visible");
            } else if (vuMark == RelicRecoveryVuMark.RIGHT) {
                column = Column.COLUMN_RIGHT;
                dashboard.displayPrintf(6, "VuMark: RIGHT visible");
            } else {
                column = Column.COLUMN_CENTER;
                dashboard.displayPrintf(6, "VuMark: UNKNOWN visible");
            }
        }

        if (DoTask("Init", runmode)) {
            // Init - optimized
            robot.jewelArm.setTargetPosition(110);
            robot.slideClaw.setPower(0.3);
            sleep(500);
            robot.rightSlide.setPower(1);
            robot.leftSlide.setPower(1);
            sleep(250);
            robot.rightSlide.setPower(0);
            robot.leftSlide.setPower(0);

            sleep(1500);
        }

        if (DoTask("Knock off jewel", runmode)) {
            if ((color_sensor.red() > color_sensor.blue() && alliance == Alliance.ALLIANCE_RED)
                    || (color_sensor.blue() > color_sensor.red() && alliance == Alliance.ALLIANCE_BLUE)) {
                DriveRobotTurn(.1, -20);
                robot.jewelArm.setTargetPosition(0);
                sleep(500); // was 2000
                DriveRobotTurn(.1, 20);
                sleep(500); // was 1000
            } else if ((color_sensor.red() > color_sensor.blue() && alliance == Alliance.ALLIANCE_BLUE)
                    || (color_sensor.blue() > color_sensor.red() && alliance == Alliance.ALLIANCE_RED)) {
                DriveRobotTurn(.1, 20);
                robot.jewelArm.setTargetPosition(0);
                sleep(500);  // was 2000
                DriveRobotTurn(.1, -20);
                sleep(500); // was 1000
            } else {
                robot.jewelArm.setTargetPosition(0);
                sleep(1000); // was 2000
            }
        } else {
            robot.jewelArm.setTargetPosition(0);
            sleep(1000);
        }

        // drive
        if (DoTask("Drive to CryptoBox", runmode)) {
            if (startposition == StartPosition.STARTPOSITION1) {
                if (alliance == Alliance.ALLIANCE_RED) {
                    if (column == Column.COLUMN_RIGHT) {
                        DriveRobotPosition(0.1, 32.25);
                    } else if (column == Column.COLUMN_CENTER) {
                        DriveRobotPosition(0.1, 39.25);
                    } else {
                        DriveRobotPosition(0.1, 47.25);
                    }
                } else {
                    if (column == Column.COLUMN_LEFT) {
                        DriveRobotPosition(0.1, -30);
                    } else if (column == Column.COLUMN_CENTER) {
                        DriveRobotPosition(0.1, -38.5);
                    } else {
                        DriveRobotPosition(0.1, -46);
                    }
                }
                sleep(1000);
                DriveRobotTurn(0.25, 90);
                sleep(1000);
                DriveRobotPosition(0.25, 6);

            } else {
                if (alliance == Alliance.ALLIANCE_RED) {
                    DriveRobotPosition(.1, 29);
                    sleep(500);
                    if (column == Column.COLUMN_CENTER) {
                        DriveRobotTurn(.25, -50);
                        sleep(500);
                        DriveRobotPosition(.1, 8);
                        sleep(500);
                    }
                    else if (column == Column.COLUMN_LEFT){
                        DriveRobotTurn(.25, -64);
                        sleep(500);
                        DriveRobotPosition(.1, 14);
                        sleep(500);
                        DriveRobotTurn(.25,8);
                    }
                    else {
                        DriveRobotTurn(.25, -25);
                        sleep(500);
                        DriveRobotPosition(.1, 7);
                        sleep(500);
                    }

                } else {
                    DriveRobotPosition(.1, -26);
                    if (column == Column.COLUMN_CENTER) {
                        DriveRobotPosition(.1,-4);
                        sleep(500);
                        DriveRobotTurn(.25, -147);
                        sleep(500);
                        DriveRobotPosition(.1, 8);
                        sleep(500);
                    }
                    else if (column == Column.COLUMN_LEFT){
                        DriveRobotPosition(.1,-4);
                        sleep (500);
                        DriveRobotTurn(.25, -165);
                        sleep(500);
                        DriveRobotPosition(.1, 7);
                        sleep(500);
                    }
                    else {
                        sleep(500);
                        DriveRobotTurn(.25, -137);
                        sleep(500);
                        DriveRobotPosition(.1, 14);
                        sleep(500);
                    }
                }
            }
        }

        // Drop glyph
        robot.slideClaw.setPower(-0.2);
        sleep(1000);
        DriveRobotPosition(0.25, -5);

        for (int i = 0; i < 2; i++) {
            DriveRobotPosition(0.2, 7);
            DriveRobotPosition(0.35, -7);
            //DriveRobotTime(1000, 1.0);
            //DriveRobotTime(500, -1.0);
        }
        sleep(250);
        robot.dunkClawArm.setPower(0.5);
        sleep(750);
        robot.dunkClawArm.setPower(0);
        robot.spinnyClaw.setPosition(1);
        robot.dunkClawLeft1.setPosition(0.5);
        robot.dunkClawRight1.setPosition(0.5);
        robot.dunkClawLeft2.setPosition(0.5);
        robot.dunkClawRight2.setPosition(0.5);
        sleep(1000);
    }



    /**
     * FUNCTIONS -----------------------------------------------------------------------------------
     */

    boolean DoTask (String taskname, RunMode debug)
    {
        dashboard.displayPrintf(0, taskname);
        if (debug == RunMode.RUNMODE_DEBUG)
        {
            dashboard.displayPrintf(1, "Press A to run, B to skip");
            while (opModeIsActive()) {
                if (gamepad1.a) {
                    dashboard.displayPrintf(1, "Run");
                    return true;
                }
                if (gamepad1.b) {
                    dashboard.displayPrintf(1, "Skip");
                    sleep(1000);
                    return false;
                }
            }
        }
        return true;
    }

    void DriveRobotTime(int ms, double power)
    {
        DrivePowerAll(power);
        sleep(ms);
        DrivePowerAll(0);
    }

    /**
     * DrivePowerAll drives the robot the specified number of inches at the specified power level.
     * @param inches How far to drive, can be negative
     * @param power Power level to set motors to
     */
    void DriveRobotPosition(double power, double inches)
    {
        double position = -inches*COUNTS_PER_INCH;

        robot.leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        robot.leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        robot.rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        DrivePowerAll(power);

        robot.leftDrive.setTargetPosition((int)position);
        robot.rightDrive.setTargetPosition((int)position);

        while (robot.leftDrive.isBusy() && robot.rightDrive.isBusy()) {
            dashboard.displayPrintf(3,"Left encoder: %d", robot.leftDrive.getCurrentPosition());
            dashboard.displayPrintf(4,"Right encoder: %d", robot.rightDrive.getCurrentPosition());
        }

        sleep(100);

        DrivePowerAll(0);

    }

    // FIXME: position equation
    void DriveRobotTurn (double power, double degree)
    {
        double position = degree*DRIVE_GEAR_REDUCTION*11.25;

        robot.leftDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        robot.rightDrive.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        robot.leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        robot.rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        robot.leftDrive.setPower(power);
        robot.rightDrive.setPower(-power);

        robot.leftDrive.setTargetPosition((int)position);
        robot.rightDrive.setTargetPosition(-(int)position);

        while (robot.leftDrive.isBusy() && robot.rightDrive.isBusy()) {
            dashboard.displayPrintf(3,"Left encoder: %d", robot.leftDrive.getCurrentPosition());
            dashboard.displayPrintf(4,"Right encoder: %d", robot.rightDrive.getCurrentPosition());
        }

        sleep(100);

        DrivePowerAll(0);
    }

    /**
     * DrivePowerAll sets all of the drive train motors to the specified power level.
     * @param power Power level to set motors to
     */
    void DrivePowerAll (double power)
    {
        robot.leftDrive.setPower(-power);
        robot.rightDrive.setPower(-power);
    }

    // MENU ----------------------------------------------------------------------------------------
    @Override
    public boolean isMenuUpButton() {
        return gamepad1.dpad_up;
    }

    @Override
    public boolean isMenuDownButton() {
        return gamepad1.dpad_down;
    }

    @Override
    public boolean isMenuEnterButton() {
        return gamepad1.dpad_right;
    }

    @Override
    public boolean isMenuBackButton() {
        return gamepad1.dpad_left;
    }

    private void doMenus() {
        FtcChoiceMenu<RunMode> modeMenu = new FtcChoiceMenu<>("Run Mode", null, this);
        FtcChoiceMenu<Alliance> allianceMenu = new FtcChoiceMenu<>("Alliance:", modeMenu, this);
        FtcValueMenu delayMenu = new FtcValueMenu("Delay:", allianceMenu, this, 0, 20000, 1000, 0, "%.0f msec");
        FtcChoiceMenu<StartPosition> startPositionMenu = new FtcChoiceMenu<>("Start Position:", delayMenu, this);

        modeMenu.addChoice("Auto", RunMode.RUNMODE_AUTO, true, allianceMenu);
        modeMenu.addChoice("Debug", RunMode.RUNMODE_DEBUG, false, allianceMenu);

        allianceMenu.addChoice("Red", Alliance.ALLIANCE_RED, true, delayMenu);
        allianceMenu.addChoice("Blue", Alliance.ALLIANCE_BLUE, false, delayMenu);

        delayMenu.setChildMenu(startPositionMenu);

        startPositionMenu.addChoice("1", StartPosition.STARTPOSITION1, true, null);
        startPositionMenu.addChoice("2", StartPosition.STARTPOSITION2, false, null);

        FtcMenu.walkMenuTree(modeMenu, this);
        runmode = modeMenu.getCurrentChoiceObject();
        alliance = allianceMenu.getCurrentChoiceObject();
        delay = (int) delayMenu.getCurrentValue();
        startposition = startPositionMenu.getCurrentChoiceObject();

        dashboard.displayPrintf(9, "Mode: %s (%s)", modeMenu.getCurrentChoiceText(), runmode.toString());
        dashboard.displayPrintf(10, "Alliance: %s (%s)", allianceMenu.getCurrentChoiceText(), alliance.toString());
        dashboard.displayPrintf(11, "Delay = %d msec", delay);
        dashboard.displayPrintf(12, "Start position: %s (%s)", startPositionMenu.getCurrentChoiceText(), startposition.toString());
    }
    // END MENU ------------------------------------------------------------------------------------
}