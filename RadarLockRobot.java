package custom;

import robocode.*;
import java.awt.Color;
import java.util.ArrayList;

public class RadarLockRobot extends AdvancedRobot {
    Enemy enemy = new Enemy();
    public static double PI = Math.PI;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        this.setColors(Color.red, Color.blue, Color.yellow,
                Color.black, Color.green);

        while (true) {
            if (enemy.name == null) {
                setTurnRadarRightRadians(2 * PI);
                execute();
            } else {
                execute();
            }
            setAhead(100);
            System.out.println("Run: setAhead(100)"); // Debug print
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        enemy.update(e, this);
        double Offset = rectify(enemy.direction -
                getRadarHeadingRadians());
        setTurnRadarRightRadians(Offset * 2);

        // Simple pattern matching: track recent heading changes
        enemy.headingChanges.add(e.getHeadingRadians() - enemy.prevHeadingRadian);
        enemy.prevHeadingRadian = e.getHeadingRadians();
        if (enemy.headingChanges.size() > 5) {
            enemy.headingChanges.remove(0);
        }

        // Weighted average of heading changes
        double avgHeadingChange = 0;
        for (int i = 0; i < enemy.headingChanges.size(); i++) {
            avgHeadingChange += enemy.headingChanges.get(i) * (enemy.headingChanges.size() - i);
        }
        avgHeadingChange /= (enemy.headingChanges.size() * (enemy.headingChanges.size() + 1) / 2.0);


        // Incorporate pattern matching into aiming
        double bulletSpeed = Rules.getBulletSpeed(1);
        double timeToImpact = enemy.distance / bulletSpeed;
        double futureX = enemy.x + Math.sin(enemy.headingRadian + avgHeadingChange * timeToImpact) * enemy.velocity * timeToImpact;
        double futureY = enemy.y + Math.cos(enemy.headingRadian + avgHeadingChange * timeToImpact) * enemy.velocity * timeToImpact;
        double absBearing = Math.atan2(futureX - getX(), futureY - getY());
        double gunOffset = rectify(absBearing - getGunHeadingRadians());
        setTurnGunRightRadians(gunOffset);

        // Targeting system
        double accuracy = 1 - Math.abs(rectify(absBearing - getGunHeadingRadians())) / PI;

        // Fire!
        if (getGunHeat() == 0 && accuracy > 0.6) { // Reduced accuracy threshold
            setFire(0.5);
        } else if (getGunHeat() < 3 && accuracy > 0.6) { // Reduced accuracy threshold
            setFire(0.1);
        }

        // Movement
        System.out.println("onScannedRobot: Moving"); // Debug print
        if (Math.random() > 0.5) {
            setTurnRightRadians(enemy.bearingRadian + PI / 2);
            System.out.println("onScannedRobot: setTurnRightRadians"); // Debug print
            if(getTurnRemainingRadians() != 0){
                setAhead(100);
            } else {
                setTurnLeftRadians(enemy.bearingRadian + PI / 2);
                System.out.println("onScannedRobot: setTurnLeftRadians (fallback)"); // Debug print
                setAhead(100);
            }
        } else {
            setTurnLeftRadians(enemy.bearingRadian + PI / 2);
            System.out.println("onScannedRobot: setTurnLeftRadians"); // Debug print
            setAhead(100);
        }

        // Automatic dodging
        if (enemy.energy < enemy.prevEnergy) {
            setTurnRightRadians(enemy.bearingRadian + PI);
            System.out.println("onScannedRobot: Dodging - setTurnRightRadians, setBack(100)"); // Debug print
            setBack(100);
        }
        enemy.prevEnergy = enemy.energy;
    }

    public void onHitRobot(HitRobotEvent e) {
        if (e.getBearing() > -90 && e.getBearing() < 90) {
            back(100);
        } else {
            ahead(100);
        }
        double randomAngle = Math.random() * 2 * PI;
        setTurnRightRadians(randomAngle);
        setAhead(100);
        enemy.name = null;

        // Check for proximity to walls and adjust movement
        double distanceToWall = Math.min(getX(), Math.min(getY(), Math.min(getBattleFieldWidth() - getX(), getBattleFieldHeight() - getY())));
        if (distanceToWall < 50) { // Adjust 50 as needed
            System.out.println("onHitRobot: Near wall, following wall");
            setTurnRightRadians(PI / 2); // Adjust direction as needed
            setAhead(100);
        }
    }

    public void onHitWall(HitWallEvent e) {
        System.out.println("onHitWall: Hit wall"); // Debug print
        double bearing = e.getBearingRadians();
        double turnAngle = PI - bearing;
        setTurnRightRadians(turnAngle);
        setAhead(100);
    }

    //角度修正方法，重要
    public double rectify(double angle) {
        if (angle < -Math.PI)
            angle += 2 * Math.PI;
        if (angle > Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    public class Enemy {
        public double x, y;
        public String name = null;
        public double headingRadian = 0.0D;
        public double bearingRadian = 0.0D;
        public double distance = 1000D;
        public double direction = 0.0D;
        public double velocity = 0.0D;
        public double prevHeadingRadian = 0.0D;
        public double energy = 100.0D;
        public double prevEnergy = 100.0D;
        public ArrayList<Double> headingChanges = new ArrayList<>();

        public void update(ScannedRobotEvent
                                   e, AdvancedRobot me) {
            name = e.getName();
            headingRadian = e.getHeadingRadians();
            bearingRadian = e.getBearingRadians();
            this.energy = e.getEnergy();
            this.velocity = e.getVelocity();
            this.distance = e.getDistance();
            direction = bearingRadian +
                    me.getHeadingRadians();
            x = me.getX() + Math.sin(direction) * distance;
            y = me.getY() + Math.cos(direction) * distance;
        }
    }
}
