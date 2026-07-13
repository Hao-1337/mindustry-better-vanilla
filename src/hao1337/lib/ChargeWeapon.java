package hao1337.lib;

import mindustry.type.Weapon;
import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.pattern.ShootPattern;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;

public class ChargeWeapon extends Weapon {
    /** Charge region suffix, if not present, "-heat" will get used */
    public String chargeSuffix;
    /** Charge speed in tick */
    public float chargeSpeed = 65f;
    /** As the name */
    public float chargeSoundMinPitch = 1f;
    /** Due to region dimension, custom quad limit is require due to how big the chagre region is */
    public float[] chargeQuad = { 0f, 1f };
    /** Charge region layer */
    public float chargeLayer = Layer.bullet - 2f;

    TextureRegion chargeRegion;

    public final RegionChargeShader shader = new RegionChargeShader() {
        @Override
        float normalizeProgress(float progress) { return chargeQuad[0] + (chargeQuad[1] - chargeQuad[0]) * progress; }
    };

    public class ChargeWeaponMount extends WeaponMount {
        public float charge = 0f;
        public float lastReload = 0f;

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
        float targetZ = z + layerOffset;
        Draw.z(targetZ < chargeLayer ? chargeLayer : targetZ);

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
        Draw.reset();
    }

    @Override
    public String toString() {
        return name == null || name.isEmpty() ? "hao1337.lib.ChargeWeapon" : "hao1337.lib.ChargeWeapon: " + name;
        // Vars.content.unit("hao1337-mod-zelvorak").weapons.get(0).progress
    }
    
    @Override
    public void update(Unit unit, WeaponMount mount) {
        super.update(unit, mount);
        ChargeWeaponMount m = (ChargeWeaponMount) mount;

        if (!m.charging) {
            m.charge = Mathf.approachDelta(m.charge, 0f, (Time.delta / chargeSpeed) * 2f);
        }

        m.reload = m.lastReload;
        m.charging = false;
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation) {
        ChargeWeaponMount m = (ChargeWeaponMount) mount;
        m.charging = true;

        if (chargeSound != null && m.charge <= 0.05f) chargeSound.at(unit, chargeSoundMinPitch);
        
        m.charge = Mathf.approachDelta(m.charge, 1f, Time.delta / chargeSpeed);
        m.lastReload = m.reload;

        if (m.charge >= 1f) {
            m.charge = 0f;
            m.reload = reload;
            super.shoot(unit, m, shootX, shootY, rotation);
        }
    }
}