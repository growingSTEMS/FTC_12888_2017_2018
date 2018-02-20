package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

public abstract class Autonomous_Parent extends Robot_Parent {

    //Vuforia variables
    private int cameraMonitorViewId;
    private VuforiaLocalizer.Parameters vParameters;
    private VuforiaTrackables relicTrackables;
    protected RelicRecoveryVuMark cryptoboxKey = null;
    private VuforiaTrackable relicTemplate = null;
    private VuforiaLocalizer vuforia;

    BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();

    private final double DRIVE_EN_COUNT_PER_FT = 1377.5;
    //private final double SECONDS_PER_FOOT = 0.531;
    private final double INTAKE_OPEN_FULLY = 0.0;
    protected final double ENCODER_DRIVE_POWER = 0.25;
    protected final double JEWEL_DRIVE_POWER = 0.15;
    private final double COMPASS_TURN_POWER = 0.5;
    // COMPASS_PAUSE_TIME - When using compassTurn, it waits COMPASS_PAUSE_TIME milliseconds before
    // using the compass to ensure the robot has begun moving.
    private final long COMPASS_PAUSE_TIME = 200;
    private final boolean USE_LEFT_ENCODER = false; // False means use right encoder
    private final double PICTOGRAPH_SEARCH_TIME = 5.0;

    private final int MIN_BLUE_HUE = 120;
    private final int MAX_BLUE_HUE = 300;

    BNO055IMU imu;
    Orientation angles;

    enum Color {
        RED,
        BLUE,
        UNKNOWN
    }

    @Override
    public void initializeRobot() {
        imu = hardwareMap.get(BNO055IMU.class, "imu");

        if (INTAKE_OPERATES_BY_POWER)
            setIntake(INTAKE_DROP_POWER);
        else
            setIntake(INTAKE_OPEN_FULLY);

        raiseJewelArm();

        setupVuMarkData();
        setupIMU();
    }

    @Override
    public void runRobot() {
        relicTrackables.activate();
        if (INTAKE_OPERATES_BY_POWER)
            stopIntake();
        else
            setIntake(INTAKE_OPEN_POSITION);

        runAutonomous();
    }

    public void runAutonomous() {

    }

    protected void moveStraightTime(double power, long time) {
        setDrive(power, power);
        sleep(time);
        setDrive(0.0, 0.0);
    }

    protected void moveStraightEncoder(double dist_feet, double maxRuntime_seconds){
        moveStraightEncoder(dist_feet, maxRuntime_seconds, ENCODER_DRIVE_POWER);
    }

    protected void moveStraightEncoder(double dist_feet, double maxRuntime_seconds, double drivePower){
        ElapsedTime runtime = new ElapsedTime();
        runtime.reset();

        /*
        TURNS ENCODER-BASED CODE INTO TIME-BASED CODE
        */
       //maxRuntime_seconds = Math.abs(dist_feet) * SECONDS_PER_FOOT;
        //dist_feet *= 20.0;

        int end_pos = getDriveEncoder() + (int)(dist_feet * DRIVE_EN_COUNT_PER_FT);
        if (dist_feet > 0.0) {
            setDrive(drivePower, drivePower);
            while ((getDriveEncoder() < end_pos) && (runtime.seconds() < maxRuntime_seconds) && opModeIsActive()) {
                telemetry.addData("Goal / End Position","%d", end_pos);
                telemetry.addData("Current Position","%d", getDriveEncoder());
                telemetry.update();
            }
        }
        else
        {
            setDrive(-drivePower, -drivePower);
            while ((getDriveEncoder() > end_pos) && (runtime.seconds() < maxRuntime_seconds) && opModeIsActive()) {
                telemetry.addData("Goal / End Position","%d", end_pos);
                telemetry.addData("Current Position","%d", getDriveEncoder());
                telemetry.update();
            }
        }
        setDrive(0.0, 0.0);
    }

    private int getDriveEncoder() {
        if (USE_LEFT_ENCODER)
            return backLeftDrive.getCurrentPosition();
        else
            return backRightDrive.getCurrentPosition();
    }

    protected RelicRecoveryVuMark getPictographKey(){
        RelicRecoveryVuMark vuMark = RelicRecoveryVuMark.from(relicTemplate);
        telemetry.addData("Status","Searching for Pictograph");
        telemetry.update();
        ElapsedTime pictographTime = new ElapsedTime();
        pictographTime.reset();
        while (vuMark == RelicRecoveryVuMark.UNKNOWN && opModeIsActive() && (pictographTime.seconds() < PICTOGRAPH_SEARCH_TIME))
        {
            vuMark = RelicRecoveryVuMark.from(relicTemplate);
        }
        telemetry.addData("Status","Pictograph Found");
        telemetry.update();
        return vuMark;
    }

    protected void compassTurn(double degrees) {
        float startPos = getCurrentDegrees();
        float goalAngle;
        if (degrees < 0.0)
        {
            degrees += 10.0;
            goalAngle = startPos - ((float) degrees);
            // Turning left
            setDrive(-COMPASS_TURN_POWER, COMPASS_TURN_POWER);
            if (goalAngle > 175.0) {
                goalAngle -= 360.0;
                sleep(COMPASS_PAUSE_TIME);
                while (getCurrentDegrees() >= startPos && opModeIsActive())
                {
                    telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                    telemetry.update();
                }
            }
            while (getCurrentDegrees() < goalAngle && opModeIsActive())
            {
                telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                telemetry.update();
            }
        }
        else
        {
            degrees -= 10.0;
            goalAngle = startPos - ((float) degrees);
            // Turning right
            setDrive(COMPASS_TURN_POWER, -COMPASS_TURN_POWER);
            if (goalAngle < -175.0) {
                goalAngle += 360.0;
                sleep(COMPASS_PAUSE_TIME);
                while (getCurrentDegrees() <= startPos && opModeIsActive())
                {
                    telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                    telemetry.update();
                }
            }
            while (getCurrentDegrees() > goalAngle && opModeIsActive())
            {
                telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                telemetry.update();
            }
        }
        setDrive(0.0, 0.0);
    }

    protected void oneWheelCompassTurn(double degrees, boolean isRightWheel) {
        float startPos = getCurrentDegrees();
        float goalAngle;
        if (degrees < 0.0)
        {
            degrees += 10.0;
            goalAngle = startPos - ((float) degrees);
            // Turning left
            if (isRightWheel)
                setDrive(0.0, COMPASS_TURN_POWER);
            else
                setDrive(-COMPASS_TURN_POWER, 0.0);
            if (goalAngle > 175.0) {
                goalAngle -= 360.0;
                sleep(COMPASS_PAUSE_TIME);
                while (getCurrentDegrees() >= startPos && opModeIsActive())
                {
                    telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                    telemetry.update();
                }
            }
            while (getCurrentDegrees() < goalAngle && opModeIsActive())
            {
                telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                telemetry.update();
            }
        }
        else
        {
            degrees -= 10.0;
            goalAngle = startPos - ((float) degrees);
            // Turning right
            if (isRightWheel)
                setDrive(0.0, -COMPASS_TURN_POWER);
            else
                setDrive(COMPASS_TURN_POWER, 0.0);
            if (goalAngle < -175.0) {
                goalAngle += 360.0;
                sleep(COMPASS_PAUSE_TIME);
                while (getCurrentDegrees() <= startPos && opModeIsActive())
                {
                    telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                    telemetry.update();
                }
            }
            while (getCurrentDegrees() > goalAngle && opModeIsActive())
            {
                telemetry.addData("Angle1","%.2f",getCurrentDegrees());
                telemetry.update();
            }
        }
        setDrive(0.0, 0.0);
    }

    private float getCurrentDegrees()
    {
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        return AngleUnit.DEGREES.normalize(AngleUnit.DEGREES.fromUnit(angles.angleUnit, angles.firstAngle));
    }

    private void setupVuMarkData()
    {
        cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        vParameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);

        vParameters.vuforiaLicenseKey = "ATR3/Cb/////AAAAGTVIvIjz0kb9u+DtnsWzGj0JigqZYbgQE+XMNBbu/++4Xnjd7/uUpFGFKVr/yZ7lnRorZBA+mpukXprPG9dDy22DQIPjId5gCDNTGs1faBtAwVnoDm8qXxeCgIoRXh7aXbQBCVdy9xusOMwgnJwn2lsINNC7dHUF4Z+azbhfjIjoZoNUsLqUBfnXoO7+Emfu62Nlnl6DQhsKLRcjCE551beyEi2Co6RLn2+so7oCY3Favuwpm4H5+f1TPMBW2fhBJH9g4nEKziL90BTu+jLjA/Pt8LIOa3OQaLy7A8gmf8GLnNFvpYQSSOuE+JCMi55Ebv8POx1MmH20HkklMkpWIdmfM/gKfnDKShnG3bJ7oOg+";

        vParameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        this.vuforia = ClassFactory.createVuforiaLocalizer(vParameters);
        relicTrackables = this.vuforia.loadTrackablesFromAsset("RelicVuMark");
        relicTemplate = relicTrackables.get(0);
    }

    private void setupIMU()
    {
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        imu.initialize(parameters);
    }

    protected void pushCube() {
        grab();
        sleep(500);
        holdCube();
        moveStraightTime(0.10, 1500);
        sleep(1000);
        moveStraightTime(-0.3, 500);
    }

    protected Color getJewelColor() {
        int hue = getHue(colorSensor.red(),colorSensor.green(),colorSensor.blue());
        if (hue > MIN_BLUE_HUE && hue < MAX_BLUE_HUE)
            return Color.BLUE;
        else
            return Color.RED;
    }

    protected void hitJewel(boolean knockBlueJewel){
        lowerJewelArm();
        sleep(2000);
        Color jewelColor = getJewelColor();
        double DRIVE_DISTANCE = 2.5 / 12.0; // Inches -> Feet
        if ((jewelColor == Color.BLUE && knockBlueJewel) || (jewelColor == Color.RED && !knockBlueJewel))
        {
            // Drive Forwards
            moveStraightEncoder(DRIVE_DISTANCE, 1.0, JEWEL_DRIVE_POWER);
            sleep(1000);
            raiseJewelArm();
            sleep(1000);
            moveStraightEncoder(-DRIVE_DISTANCE, 1.0, JEWEL_DRIVE_POWER);
        }
        else if ((jewelColor == Color.BLUE && !knockBlueJewel) || (jewelColor == Color.RED && knockBlueJewel))
        {
            // Drive Backwards
            moveStraightEncoder(-DRIVE_DISTANCE, 1.0, JEWEL_DRIVE_POWER);
            sleep(1000);
            raiseJewelArm();
            sleep(1000);
            moveStraightEncoder(DRIVE_DISTANCE + (1.0 / 12.0), 1.0, JEWEL_DRIVE_POWER);
        }
        sleep(1000);
    }

    // Function from Zarokka
    // https://stackoverflow.com/questions/23090019/fastest-formula-to-get-hue-from-rgb
    protected int getHue(int red, int green, int blue) {
        float min = Math.min(Math.min(red, green), blue);
        float max = Math.max(Math.max(red, green), blue);

        if (min == max) {
            return 0;
        }

        float hue = 0f;
        if (max == red) {
            hue = (green - blue) / (max - min);

        } else if (max == green) {
            hue = 2f + (blue - red) / (max - min);

        } else {
            hue = 4f + (red - green) / (max - min);
        }

        hue = hue * 60;
        if (hue < 0) hue = hue + 360;

        return Math.round(hue);
    }
}

