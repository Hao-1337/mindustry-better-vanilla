package hao1337.lib;

import arc.math.Rand;
import mindustry.entities.Lightning;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.gen.Bullet;

public class LightningBulletType extends MissileBulletType {
    public static Rand random = new Rand(8346197);
    public float lightningDamage = 60f;
    public int lightningMinLength = 6;
    public int lightningMaxLength = 12;
    public int lightningPerHits = 5;
    public float lightningDelay = 5f;

    @Override
    public void drawTrail(Bullet b) {
        super.drawTrail(b);
        if (b.timer(0, lightningDelay))
            lightningTrail(b);
    }

    void lightningTrail(Bullet b) {
        for (int i = 0; i < lightningPerHits; i++) {
            float angle = random.random(360f);
            int length = random.random(lightningMinLength, lightningMaxLength);

            Lightning.create(
                    b.team,
                    b.type.trailColor,
                    damage,
                    b.x + b.vel.x * 3.5f,
                    b.y + b.vel.y * 3.5f,
                    angle,
                    length);
        }
    }
} 
