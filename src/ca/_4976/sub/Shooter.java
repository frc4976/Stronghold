package ca._4976.sub;

import ca._4976.io.Input;
import ca._4976.io.Output;
import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.Preferences;

import static ca._4976.io.Output.*;
import static ca._4976.io.Input.*;
import static ca._4976.io.Controller.*;

public class Shooter {

    private PIDController pid = new PIDController(8.0e-6, 0, 1.0E-4, 0, Encoder.SHOOTER, Motor.SHOOTER);
    private Preferences preferences = Preferences.getInstance();
    private Targeting targeting;

    private int state = 0;
    private long waitTimeFlag;
    private long onDelayFlag = System.currentTimeMillis();

    boolean last = false;

    public void addTargetingSubsystem(Targeting targeting) { this.targeting = targeting; }

    public void disabledInit() {

        Primary.vibrate(0.0f);
        pid.reset();
        pid.setPID(preferences.getDouble("P", 0), preferences.getDouble("I", 0), preferences.getDouble("D", 0));
        state = 0;
        pid.disable();
        Motor.SHOOTER.set(0);
    }

    void cock() {

        state = 0;
        pid.enable();
        pid.setSetpoint(preferences.getInt("Shooter_RPM", 0));

        Solenoid.INTAKE.set(true);
    }

    void disablePID() {

        pid.disable();
        pid.reset();
        Motor.SHOOTER.set(0);
    }

    boolean shoot() {

        switch (state) {

            case 0:

                if (Math.abs(pid.getError()) < 50) {

                    Motor.INTAKE_ROLLERS.set(1);
                    state++;

                } return false;
            case 1:

                if (!Digital.BALL_DETECTED.get()) {

                    waitTimeFlag = System.currentTimeMillis();
                    state++;

                } return false;

            case 2:

                if (System.currentTimeMillis() - waitTimeFlag > 500) {

                    disablePID();
                    Motor.INTAKE_ROLLERS.set(0);
                    return true;

                } return false;
        }

        return false;
    }

    public void teleopPeriodic() {

        System.out.println(Digital.BALL_DETECTED.get());

        if (Primary.Button.X.isDownOnce()) {

            Primary.vibrate(0);
            pid.disable();
            pid.reset();
            Motor.SHOOTER.set(0);
            state = 0;
        }

        switch (state) {

            default: break;

            case 0:

                if (Primary.Button.B.isDownOnce() || Primary.Button.RIGHT_BUMPER.isDownOnce()) state = 1;

                break;
            case 1:

                pid.setSetpoint(preferences.getInt("Shooter_RPM", 0));
                pid.enable();
                Output.Solenoid.INTAKE.set(true);
                state = 2;

                break;
            case 2:

                if (!(Math.abs(pid.getError()) < 50)) {
                    Primary.vibrate(0.0f);
                    onDelayFlag = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - onDelayFlag > 300)
                        Primary.vibrate(1f);
                    else Primary.vibrate(0.0f);
                }

                if (Primary.Button.B.isDownOnce() && Math.abs(pid.getError()) < 70) {

                    Motor.INTAKE_ROLLERS.set(1);
                    state = 3;

                } break;
            case 3:

                Primary.vibrate(0.0f);

                if (!Digital.BALL_DETECTED.get() && last) {

                    waitTimeFlag = System.currentTimeMillis();
                    state = 4;

                }

                last = Digital.BALL_DETECTED.get();
                break;
            case 4:

                if (System.currentTimeMillis() - waitTimeFlag > 500) {

                    pid.disable();
                    pid.reset();
                    Motor.SHOOTER.set(0);
                    Motor.INTAKE_ROLLERS.set(0);
                    state = 0;

                }
                break;
        }

        if (Math.abs(Secondary.Stick.LEFT.vertical()) > 0.2) {

            state = -1;
            pid.disable();
            Motor.SHOOTER.set(Secondary.Stick.LEFT.vertical());

        } else if (state == -1) {

            state = 0;
            Motor.SHOOTER.set(0);
        }

        if (Secondary.Button.X.isDownOnce()) {

            state = 0;
            Motor.SHOOTER.set(0);
        }
    }
}
