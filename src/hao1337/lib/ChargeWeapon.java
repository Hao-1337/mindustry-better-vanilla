package hao1337.lib;

import mindustry.type.Weapon;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.audio.SoundLoop;
import mindustry.entities.Predict;
import mindustry.entities.Sized;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import static mindustry.Vars.*;

public class ChargeWeapon extends Weapon {
    /** Charge region suffix, if not present, "-heat" will get used */
    public String chargeSuffix;
    /** Charge speed in tick */
    public float chargeSpeed = 65f;
    /** Due to region dimension, custom quad limit is require due to how big the chagre region is */
    public float[] chargeQuad = { 0f, 1f };

    TextureRegion chargeRegion;

    public final RegionChargeShader shader = new RegionChargeShader() {
        @Override
        float normalizeProgress(float progress) { return chargeQuad[0] + (chargeQuad[1] - chargeQuad[0]) * progress; }
    };

    public class ChargeWeaponMount extends WeaponMount {
        public float charge;

        public ChargeWeaponMount(Weapon weapon) {
            super(weapon);
        }
    }

    public ChargeWeapon(String name) {
        super(name);
        mountType = ChargeWeaponMount::new;
        shoot = new ShootPattern() {{
            shots = 1;
            firstShotDelay = 0;
            shotDelay = 0;
        }};
    }

    @Override
    public void load() {
        super.load();
        chargeRegion = Core.atlas.find(chargeSuffix == null ? name + "-heat" : name + chargeSuffix);
    }

    public ChargeWeapon() {
        this("");
    }

    @Override
    public void draw(Unit unit, WeaponMount mount) {
        super.draw(unit, mount);
        ChargeWeaponMount m = (ChargeWeaponMount) mount;

        float rot = unit.rotation - 90 + m.rotation;
        float rx = unit.x + Angles.trnsx(rot, x, y);
        float ry = unit.y + Angles.trnsy(rot, x, y);

        float z = Draw.z();
        Draw.z(z + layerOffset);

        Draw.draw(Layer.flyingUnit, () -> {
            float prevScl = Draw.xscl;
            Draw.xscl *= -Mathf.sign(flipSprite);
            float height = chargeRegion.height * chargeRegion.scl();

            shader.progress = m.charge;
            shader.region = chargeRegion;
            shader.tint = unit.team.color;

            Draw.shader(shader);
            Draw.rect(chargeRegion, rx, ry,
                    chargeRegion.width * chargeRegion.scl(),
                    height,
                    rot);
            Draw.shader();
            Draw.xscl = prevScl;
        });
        Draw.z(z);
        ;
    }

    @Override
    public String toString() {
        return name == null || name.isEmpty() ? "hao1337.lib.ChargeWeapon" : "hao1337.lib.ChargeWeapon: " + name;
        // Vars.content.unit("hao1337-mod-zelvorak").weapons.get(0).progress
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        boolean can = unit.canShoot();
        float lastReload = mount.reload;
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
        mount.recoil = Mathf.approachDelta(mount.recoil, 0, unit.reloadMultiplier / recoilTime);
        if(recoils > 0){
            if(mount.recoils == null) mount.recoils = new float[recoils];
            for(int i = 0; i < recoils; i++){
                mount.recoils[i] = Mathf.approachDelta(mount.recoils[i], 0, unit.reloadMultiplier / recoilTime);
            }
        }
        mount.smoothReload = Mathf.lerpDelta(mount.smoothReload, mount.reload / reload, smoothReloadSpeed);
        mount.charge = mount.charging && shoot.firstShotDelay > 0 ? Mathf.approachDelta(mount.charge, 1, 1 / shoot.firstShotDelay) : 0;

        float warmupTarget = (can && mount.shoot) || (continuous && mount.bullet != null) || mount.charging ? 1f : 0f;
        if(linearWarmup){
            mount.warmup = Mathf.approachDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }else{
            mount.warmup = Mathf.lerpDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }

        //rotate if applicable
        if(rotate && (mount.rotate || mount.shoot) && can){
            float axisX = unit.x + Angles.trnsx(unit.rotation - 90,  x, y),
            axisY = unit.y + Angles.trnsy(unit.rotation - 90,  x, y);

            mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);
            if(rotationLimit < 360){
                float dst = Angles.angleDist(mount.rotation, baseRotation);
                if(dst > rotationLimit/2f){
                    mount.rotation = Angles.moveToward(mount.rotation, baseRotation, dst - rotationLimit/2f);
                }
            }
        }else if(!rotate){
            mount.rotation = baseRotation;
            mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
        }

        float
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
        shootAngle = bulletRotation(unit, mount, bulletX, bulletY);

        //find a new target
        if(!controllable && autoTarget){
            if((mount.retarget -= Time.delta) <= 0f){
                mount.target = findTarget(unit, mountX, mountY, bullet.range, bullet.collidesAir, bullet.collidesGround);
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }

            if(mount.target != null && checkTarget(unit, mount.target, mountX, mountY, bullet.range)){
                mount.target = null;
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, bullet.range + Math.abs(shootY) + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && can;

                if(predictTarget){
                    Vec2 to = Predict.intercept(unit, mount.target, bullet.speed);
                    mount.aimX = to.x;
                    mount.aimY = to.y;
                }else{
                    mount.aimX = mount.target.x();
                    mount.aimY = mount.target.y();
                }
            }

            mount.shoot = mount.rotate = shoot;

            //note that shooting state is not affected, as these cannot be controlled
            //logic will return shooting as false even if these return true, which is fine
        }

        if(alwaysShooting) mount.shoot = true;

        //update continuous state
        if(continuous && mount.bullet != null){
            if(!mount.bullet.isAdded() || mount.bullet.time >= mount.bullet.lifetime || mount.bullet.type != bullet){
                mount.bullet = null;
            }else{
                mount.bullet.rotation(weaponRotation + 90);
                mount.bullet.set(bulletX, bulletY);
                mount.reload = reload;
                mount.recoil = 1f;
                unit.vel.add(Tmp.v1.trns(unit.rotation + 180f, mount.bullet.type.recoil * Time.delta));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }

                if(alwaysContinuous && mount.shoot){
                    mount.bullet.time = mount.bullet.lifetime * mount.bullet.type.optimalLifeFract * mount.warmup;
                    mount.bullet.keepAlive = true;

                    unit.apply(shootStatus, shootStatusDuration);
                }
            }
        }else{
            //heat decreases when not firing
            mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / cooldownTime, 0);

            if(mount.sound != null){
                mount.sound.update(bulletX, bulletY, false);
            }
        }

        //flip weapon shoot side for alternating weapons
        boolean wasFlipped = mount.side;
        if(otherSide != -1 && alternate && mount.side == flipSprite && mount.reload <= reload / 2f && lastReload > reload / 2f){
            unit.mounts[otherSide].side = !unit.mounts[otherSide].side;
            mount.side = !mount.side;
        }
        ChargeWeaponMount m = (ChargeWeaponMount) mount;

        //shoot if applicable
        if(mount.shoot && //must be shooting
        can && //must be able to shoot
        !(bullet.killShooter && mount.totalShots > 0) && //if the bullet kills the shooter, you should only ever be able to shoot once
        (!useAmmo || unit.ammo > 0 || !state.rules.unitAmmo || unit.team.rules().infiniteAmmo) && //check ammo
        (!alternate || wasFlipped == flipSprite) &&
        mount.warmup >= minWarmup && //must be warmed up
        unit.vel.len() >= minShootVelocity && //check velocity requirements
        (mount.reload <= 0.0001f || (alwaysContinuous && mount.bullet == null)) && //reload has to be 0, or it has to be an always-continuous weapon
        (alwaysShooting || Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, shootCone)) //has to be within the cone
        ){
            m.charging = true;
            m.charge = Mathf.approachDelta(m.charge, 1f, Time.delta / chargeSpeed);
        }
        else {
            m.charging = false;
            m.charge = Mathf.approachDelta(m.charge, 0f, (Time.delta / chargeSpeed) * 2f);
        }

        if (m.charge >= 1f) {
            mount.reload = reload;
            shoot(unit, mount, bulletX, bulletY, shootAngle);

            if(useAmmo){
                unit.ammo--;
                if(unit.ammo < 0) unit.ammo = 0;
            }
        }
    }
}